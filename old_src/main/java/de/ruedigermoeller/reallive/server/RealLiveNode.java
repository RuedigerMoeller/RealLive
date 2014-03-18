package de.ruedigermoeller.reallive.server;

import de.ruedigermoeller.fastcast.config.FCClusterConfig;
import de.ruedigermoeller.fastcast.config.FCConfigBuilder;
import de.ruedigermoeller.fastcast.remoting.FCFutureResultHandler;
import de.ruedigermoeller.fastcast.remoting.FCRemoting;
import de.ruedigermoeller.fastcast.remoting.FastCast;
import de.ruedigermoeller.fastcast.util.FCLog;
import de.ruedigermoeller.heapoff.bytez.malloc.MallocBytezAllocator;
import de.ruedigermoeller.reallive.client.SystemSchema;
import de.ruedigermoeller.reallive.facade.database.RLTableConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 08.11.13
 * Time: 17:28
 * To change this template use File | Settings | File Templates.
 */
public class RealLiveNode {

    private boolean isPrimary = false;

    // -XX:+UseConcMarkSweepGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=10 -Xmx2g -Xms2g -XX:-UseAdaptiveSizePolicy -XX:ParallelGCThreads=2 -verbose:gc -XX:SurvivorRatio=4 -XX:NewSize=300m

    public static FCClusterConfig getClusterConfig() {
        FCClusterConfig conf = FCConfigBuilder.New()
                .socketTransport("server", "ifac", "229.9.9.8", 55554)
                    .topic("rlserver", 0, 4000, 4).setOpenCalls(1000,5000)
                    .end()
                .socketTransport("global", "ifac", "229.9.9.9", 55555)
                   .topic("rlglobal", 1, 14000, 7)
                   .end()
                .socketTransport("control", "ifac", "229.9.9.10", 55556)
                    .topic("rlinternal", 2, 10, 7)
                    .topic("rlclientclasses", 5, 10, 7)
                    .membership("members", 3)
                    .end()
                .socketTransport("client", "ifac", "229.9.9.11", 55557)
                   .topic("rlclient", 4, 4000, 7) // not too low compared to rlglobal/num datanodes, else queries block change processing
                      .setSendQueuePercentage(20)
                   .end()
            .build();
        conf.defineInterface("ifac","127.0.0.1");
        conf.setLogLevel(FCLog.INFO);
        try {
            // write out config to enable ClusterView
            new File("/tmp").mkdir(); // windoze ..
            conf.write("/tmp/rl.yaml");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return conf;
    }

    long maxOffHeapMemUsage = 32 * 1024 * 1024 * 1024l; // GB

    RealLiveTopicService rlService;
    FCRemoting rem;
    RealLiveNodeInternals rlInternals;
    RealLiveNodeInternals rlinternalRemote;

    public void connect() throws Exception {
        rem = FastCast.getRemoting();

        // join
        rem.joinCluster(getClusterConfig(),"data",null);

        // start send/receive internals
        rlInternals = new RealLiveNodeInternals();
        rem.startReceiving("rlinternal", rlInternals);
        rlinternalRemote = (RealLiveNodeInternals) rem.startSending("rlinternal", RealLiveNodeInternals.class);

        // count datanodes and assign node mask id
        final AtomicInteger nodeCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);
        rlinternalRemote.ping(new FCFutureResultHandler() {
            @Override
            public void resultReceived(Object obj, String sender) {
                nodeCount.incrementAndGet();
            }

            @Override
            public void timeoutReached() {
                latch.countDown();
            }
        });

        try {
            latch.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int nodeMaskId = nodeCount.get() + 1;
        rlService = new RealLiveTopicService(nodeMaskId); // set after init
        FCLog.log("assigned node mask id "+nodeMaskId);

        // start PingNodeInfo supplier thread
        rem.getMemberShipLocal().setNodeState(createNodePingInfo());
        new Thread("pingnodeinfo") {
            public void run() {
                while( true ) {
                    try {
                        Thread.sleep(2000);
                        rem.getMemberShipLocal().setNodeState(createNodePingInfo());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        // start listening to clients
        rem.startReceiving("rlserver",rlService);
        if ( nodeCount.get() == 0 ) {
            FCLog.log("primary node, creating systables ..");
            isPrimary = true;
            SystemSchema.create(rlService);
            SystemSchema.User defUser = new SystemSchema.User();

            defUser.getUid().setString("admin");
            defUser.getFullname().setString("admin user");
            defUser.getPassword().setString("pwd");
            defUser.setCreated(System.currentTimeMillis());
            defUser.setId(1);
            rlService.getDb().getTableMutator(SystemSchema.User.class).add((SystemSchema.User) defUser.toOffHeap());

            RLTableConfiguration[] tableConfigurations = rlService.getDb().getTableConfigurations();
            for (int i = 0; i < tableConfigurations.length; i++) {
                RLTableConfiguration tableConfiguration = tableConfigurations[i];
                SystemSchema.Table tab = new SystemSchema.Table();

                tab.setId(tableConfiguration.getTableId());
                tab.setTableId(tableConfiguration.getTableId());
                tab.getName().setString(tableConfiguration.getTableName());
                tab.getClz().setString(tableConfiguration.getRowClass().getName());

                rlService.getDb().getTableMutator(SystemSchema.Table.class).add((SystemSchema.Table) tab.toOffHeap());
            }

//            for (int i = 3; i < 100; i++ ) {
//                defUser.getUid().setString("a"+i);
//                defUser.getFullname().setString("admin user "+i);
//                defUser.getPassword().setString("a");
//                defUser.setCreated(System.currentTimeMillis());
//                defUser.setId(i);
//                rlService.getDb().getTableMutator(SystemSchema.User.class).add((SystemSchema.User) defUser.toOffHeap());
//            }
        }
    }

    public RLNodePingInfo createNodePingInfo() {
        RLNodePingInfo info = new RLNodePingInfo(rlService.getNodeMaskId(),maxOffHeapMemUsage- MallocBytezAllocator.alloced.get(),rem.getNodeId());
        // fixme: fill table row count
        return info;
    }

    public static void main( String arg[] ) throws Exception {
        RealLiveNode node = new RealLiveNode();
        node.connect();
    }
}
