package reallive.testclient;

import org.nustaq.reallive.client.ReplicatedSet;
import org.nustaq.reallive.impl.RLSchema;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.Record;

import java.util.ArrayList;

/**
 * Created by ruedi on 06.07.14.
 */
public class TClient {

    public static class TCRecord extends Record {
        String instrument;

        double bidPrc, askPrc;
        int    bidQty, askQty;

        public String getInstrument() {
            return instrument;
        }

        public void setInstrument(String instrument) {
            this.instrument = instrument;
        }

        public double getBidPrc() {
            return bidPrc;
        }

        public void setBidPrc(double bidPrc) {
            this.bidPrc = bidPrc;
        }

        public double getAskPrc() {
            return askPrc;
        }

        public void setAskPrc(double askPrc) {
            this.askPrc = askPrc;
        }

        public int getBidQty() {
            return bidQty;
        }

        public void setBidQty(int bidQty) {
            this.bidQty = bidQty;
        }

        public int getAskQty() {
            return askQty;
        }

        public void setAskQty(int askQty) {
            this.askQty = askQty;
        }
    }


    public static class ClientActor extends Actor<ClientActor> {

        ReplicatedSet<TCRecord> replica = new ReplicatedSet<>();
        int changeCount = 0;

        public void $run(RLTable<TCRecord> table ) {
            table.getStream().subscribe((r) -> r.getAskPrc() > 1.8, (change) -> {
//                System.out.println(change);
                if ( (changeCount++%5000) == 0 ) {
                    System.out.println("-------------------------------------------");
                    replica.dump();
                    System.out.println("-------------------------------------------");
                }
                replica.onChangeReceived(change);
            });
        }

    }

    public static class TCMutator extends Actor<TCMutator> {
        ArrayList<Future<String>> recids = new ArrayList();

        public Future $init(RLTable<TCRecord> table ) {
            for ( int i = 0; i < 100; i++) {
                TCRecord recordForAdd = table.createRecordForAdd();
                recordForAdd.setAskPrc(1+Math.random());
                recordForAdd.setBidPrc(0+Math.random());
                recordForAdd.setBidQty(100);
                recordForAdd.setAskQty(110);
                recordForAdd.setInstrument("FDS "+i);
                recids.add(recordForAdd.$apply());
            }
            Promise res = new Promise();
            yieldList(recids).then((r,e) -> {
                res.receiveResult("void",null);
            });
            return res;
        }

        public void $run(RLTable<TCRecord> table) {
            for (int i = 0; i < recids.size(); i++) {
                Future<String> stringFuture = recids.get(i);
                TCRecord recordForUpdate = table.createRecordForUpdate(stringFuture.getResult(), false);
                recordForUpdate.setAskPrc(1+Math.random());
                recordForUpdate.setBidPrc(0+Math.random());
                recordForUpdate.$apply();
            }
            delayed(10, () -> self().$run(table));
        }

    }

    public static void main( String arg[] ) {
        RLSchema schema = new RLSchema();
        schema.createTable( "mkt", TCRecord.class );
        RLTable<TCRecord> table = schema.getTable("mkt");

        TCMutator mutator = Actors.AsActor(TCMutator.class);
        ClientActor client = Actors.AsActor(ClientActor.class);

        mutator.$init(table).then( (r,e) -> {
            client.$run(table);
            mutator.$run(table);
        });


    }
}
