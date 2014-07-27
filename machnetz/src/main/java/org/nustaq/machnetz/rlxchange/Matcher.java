package org.nustaq.machnetz.rlxchange;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.FutureLatch;
import org.nustaq.kontraktor.util.TicketMachine;
import org.nustaq.machnetz.model.rlxchange.*;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.RealLive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ruedi on 19.07.14.
 */
public class Matcher extends Actor<Matcher> {

    public static int MATCHER_ID = 1; // id of sending matcher
    private static final int MAX_ORDER_QTY = 9999;

    RealLive rl;

    RLTable<Order> orders;
    RLTable<Trade> trades;
    RLTable<Instrument> instruments;
    HashMap<String,InstrumentMatcher> matcherMap = new HashMap<>();
    TicketMachine tickets = new TicketMachine();

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
                        matcherMap.put(change.getRecordKey(), new InstrumentMatcher(self(),inst, orders, trades, m));
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

    public Future<String> $addOrder( Order ord ) {
        if ( ord.getLimitPrice() <= 0 || ord.getQty() < 1 || ord.getQty() > MAX_ORDER_QTY || ord.getLimitPrice() > 9999 ) {
            return new Promise("Invalid OrderPrice or Quantity");
        }
        Promise<String> result = new Promise<>();
        String positionKey = ord.getTraderKey() + "#" + ord.getInstrumentKey();

        tickets.getTicket(ord.getTraderKey()).then((finished, error) -> {

            FutureLatch latch = new FutureLatch(finished);

            Future<Asset> cash = rl.getTable("Asset").$get(ord.getTraderKey() + "#cash");
            Future<Asset> position = rl.getTable("Asset").$get(positionKey);

            yield(cash, position).then( (r,e) -> {

                Asset cashAsset = cash.getResult();
                Asset posAsset = position.getResult();

                if ( cashAsset == null ) {
                    result.receiveResult("fatal: no cash asset found",null);
                    finished.signal();
                    return;
                }
                if ( posAsset == null ) {
                    posAsset = new Asset(positionKey,0);
                }

                if (ord.isBuy()) {
                    if ( cashAsset.getAvaiable() < ord.getQty() * ord.getLimitPrice() ) // no need to check position
                    {
                        result.receiveResult("Not enough cash to place buy order", null);
                        finished.signal();
                        return;
                    }
                    // add margin
                    cashAsset.setMargined(cashAsset.getMargined()+ord.getQty() * ord.getLimitPrice());
                    ord.setCashMargin(ord.getQty() * ord.getLimitPrice());

                    rl.getTable("Asset").$put( cashAsset.getRecordKey(), cashAsset, MATCHER_ID );
                    orders.$add(ord, 0);

                    result.receiveResult(null, null);
                    finished.signal();

                } else { // Sell order
                    int positionQty = Math.max(0,posAsset.getAvaiable());
                    int sellOrdQty = ord.getQty(); // is NOT negative, fixme: should flag with negative qty instead isBuy
                    int toCashMarginQty = Math.max(sellOrdQty-positionQty,0);
                    int toInstrMarginQty = Math.max(sellOrdQty-toCashMarginQty,0);

                    if (cashAsset.getAvaiable() < toCashMarginQty * (1000-ord.getLimitPrice())) {
                        result.receiveResult("Not enough cash to cover short", null);
                        return;
                    }

                    if ( toCashMarginQty > 0 ) {
                        int cashMargin = toCashMarginQty * (1000-ord.getLimitPrice());
                        // lock cash margined part of order
                        cashAsset.setMargined( cashAsset.getMargined() + toCashMarginQty );
                        ord.setCashMargin( cashMargin );
                        rl.getTable("Asset").$put( cashAsset.getRecordKey(), cashAsset, MATCHER_ID );
                    }
                    if ( toInstrMarginQty > 0 ) {
                        // lock asset margined part of order
                        posAsset.setMargined(posAsset.getMargined()+toCashMarginQty);
                        ord.setPositionMargin(toInstrMarginQty);
                        rl.getTable("Asset").$put( posAsset.getRecordKey(), posAsset, MATCHER_ID );
                    }

                    orders.$add(ord, 0);

                    result.receiveResult(null, null);
                    finished.signal();
                }

            });

        });

        return result;
    }

    public void $processMatch( Order ord, int matchedQty, int matchPrice ) {
        tickets.getTicket(ord.getTraderKey()).then( (finished,e) -> {

            String positionKey = ord.getTraderKey() + "#" + ord.getInstrumentKey();

            Future<Asset> cash = rl.getTable("Asset").$get(ord.getTraderKey()+"#cash");
            Future<Asset> position = rl.getTable("Asset").$get(positionKey);

            yield(cash, position).then( (r,e1) -> {

                Asset cashAsset = cash.getResult();
                Asset posAsset = position.getResult();

                if ( cashAsset == null ) {
                    throw new RuntimeException("FATAL ERROR, cash asset not found.");
                }
                if ( posAsset == null ) {
                    posAsset = new Asset(positionKey,0);
                }

                if ( ord.isBuy() ) {
                    posAsset.setQty( posAsset.getQty() + matchedQty );
                    // release margin
                    cashAsset.setMargined( cashAsset.getMargined() - matchedQty * ord.getLimitPrice() );
                    // subtract price of match from cash
                    cashAsset.setQty( cashAsset.getQty() - matchedQty * matchPrice );

                    rl.getTable("Asset").$put( posAsset.getRecordKey(), posAsset, MATCHER_ID );
                    rl.getTable("Asset").$put( cashAsset.getRecordKey(), cashAsset, MATCHER_ID );

                    finished.signal();
                } else {
                    finished.signal();
                }

            });
        });
    }

    public Future<String> $delOrder( Order ord ) {
        return new Promise("void");
    }

}
