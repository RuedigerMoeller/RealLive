package org.nustaq.machnetz.rlxchange;

import org.nustaq.kontraktor.Actor;
import org.nustaq.machnetz.model.rlxchange.Instrument;
import org.nustaq.machnetz.model.rlxchange.Market;
import org.nustaq.machnetz.model.rlxchange.Order;
import org.nustaq.machnetz.model.rlxchange.Trade;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.RealLive;
import org.nustaq.reallive.client.ReplicatedSet;

import java.util.HashMap;

/**
 * Created by ruedi on 19.07.14.
 */
public class Matcher extends Actor<Matcher> {

    public static int MATCHER_ID = 1; // id of sending matcher

    RealLive rl;

    RLTable<Order> orders;
    RLTable<Trade> trades;
    RLTable<Instrument> instruments;
    HashMap<String,InstrumentMatcher> matcherMap = new HashMap<>();

    public void $init(RealLive rl) {
        Thread.currentThread().setName("Matcher");
        checkThread();
        this.rl = rl;

        orders = rl.getTable("Order");
        trades = rl.getTable("Trade");
        RLTable<Market> market = rl.getTable("Market");
        instruments = rl.getTable("Instrument");


        // sharding could be done using an instrument level filter below
        instruments.stream().each((change) -> {
            checkThread();
            if (change.isAdd()) {
                Instrument inst = change.getRecord();
                market.$get(inst.getRecordKey()).then((m, e) -> {
                    checkThread();
                    if (m != null) {
                        market.prepareForUpdate(m);
                        instruments.prepareForUpdate(inst);
                        matcherMap.put(change.getRecordKey(), new InstrumentMatcher(inst, orders, trades, m));
                        System.out.println("PUT MATCHER size: "+matcherMap.size()+" id "+System.identityHashCode(matcherMap));
                    }
                });
            } else if ( change.isSnapshotDone() ) {
                checkThread();
                orders.stream().subscribe(null,(ordchange) -> {
                    checkThread();
                    if ( ordchange.getRecord() != null ) {
                        InstrumentMatcher instrumentMatcher = matcherMap.get(ordchange.getRecord().getInstrumentKey());
                        if ( instrumentMatcher == null ) {
                            System.out.println("fatal: no matcher found for " + ordchange.getRecord());
                            System.out.println("matcherMap "+matcherMap);
                        }
                        instrumentMatcher.onARUChange(ordchange);
                    }
                    else if ( ordchange.isSnapshotDone() ) {
                        matcherMap.values().forEach((matcher) -> matcher.snapDone(ordchange) );
                    } else {
                        System.out.println("ignored change message "+ordchange);
                    }
                });
            }
        });

    }
}
