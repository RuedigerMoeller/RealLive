package org.nustaq.machnetz.rlxchange;

import org.nustaq.kontraktor.Actor;
import org.nustaq.machnetz.model.rlxchange.Instrument;
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

    RealLive rl;

    RLTable<Order> orders;
    RLTable<Trade> trades;
    RLTable<Instrument> instruments;
    ReplicatedSet<Instrument> instrSet;
    HashMap<String,InstrumentMatcher> matcherMap = new HashMap<>();

    public void $init(RealLive rl) {
        this.rl = rl;

        orders = rl.getTable("Order");
        trades = rl.getTable("Trade");
        instruments = rl.getTable("Instrument");

        instrSet = new ReplicatedSet<Instrument>();

        // sharding could be done using an instrument level filter below
        instruments.stream().filter(null, (change) -> {
            if (change.isAdd()) {
                matcherMap.put(change.getRecordKey(), new InstrumentMatcher(change.getRecord(),orders,trades));
            }
            instrSet.onChangeReceived(change);
        });

        // just start matching after instruments are loaded
        instrSet.onFinished(() -> {
            orders.stream().subscribe(null,(change) -> {
                if ( change.getRecord() != null ) {
                    matcherMap.get(change.getRecord().getInstrumentKey()).onARUChange(change);
                }
                else if ( change.isSnapshotDone() ) {
                    matcherMap.values().forEach((matcher) -> matcher.snapDone(change) );
                } else {
                    System.out.println("ignored change message "+change);
                }
            });
        });

    }
}
