package org.nustaq.machnetz.rlxchange;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.TicketMachine;
import org.nustaq.machnetz.model.rlxchange.*;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.RealLive;

import java.util.HashMap;

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

    long orderIdCnt = System.currentTimeMillis(); // FIXME: adjust after restart from persistence ?

    String createOrderId() {
        return "MA:"+orderIdCnt++;
    }

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

            Future<Asset> cash = rl.getTable("Asset").$get(ord.getTraderKey() + "#cash");
            Future<Asset> position = rl.getTable("Asset").$get(positionKey);

            yield(cash, position).then( (r,e) -> {

                Asset cashAsset = cash.getResult();
                Asset posAsset = position.getResult();

                if ( cashAsset == null ) {
                    result.receive("fatal: no cash asset found", null);
                    finished.signal();
                    return;
                }
                if ( posAsset == null ) {
                    posAsset = new Asset(positionKey,0);
                }

                ord._setRecordKey(createOrderId());
                matcherMap.get(ord.getInstrumentKey()).addOrder(rl.getTable("Asset"), orders, ord, cashAsset, posAsset)
                    .then((res,err) -> {
                        result.receive(err != null ? err : res, null);
                        finished.signal();
                    });
            });

        });

        return result;
    }

    // order must not be changed, just as input
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

                int prevQty = posAsset.getAvaiable();
                if ( ord.isBuy() ) {
                    posAsset.setQty( posAsset.getQty() + matchedQty );
                    // release cash margin of order
                    cashAsset.setMargined( cashAsset.getMargined() - matchedQty * ord.getLimitPrice() );
                    // subtract price of match from cash
                    cashAsset.setQty( cashAsset.getQty() - matchedQty * matchPrice );
                    // adjust open buy
                    posAsset.setOpenBuyQty(posAsset.getOpenBuyQty()-matchedQty);

                } else {
                    // add cash gained
                    cashAsset.setQty( cashAsset.getQty() + matchedQty * matchPrice );

                    // free part of initally reserved margin when order was added
                    cashAsset.setMargined( cashAsset.getMargined() - (1000-ord.getLimitPrice()) * matchedQty );

                    // adjust asset position
                    posAsset.setQty( posAsset.getQty() - matchedQty );
                    posAsset.setOpenSellQty( posAsset.getOpenSellQty() - matchedQty );
                }
                int currMargin = cashAsset.getMargined();
                if ( prevQty < 0 ) {
                    currMargin = currMargin - Math.abs(prevQty) * 1000;
                }
                if ( posAsset.getQty() < 0 ) {
                    currMargin = currMargin + Math.abs(posAsset.getQty()) * 1000;
                }
                cashAsset.setMargined(currMargin);

                rl.getTable("Asset").$put( posAsset.getRecordKey(), posAsset, MATCHER_ID );
                rl.getTable("Asset").$put( cashAsset.getRecordKey(), cashAsset, MATCHER_ID );
                finished.signal();

            });
        });
    }

    public Future<String> $delOrder( Order ord ) {
        Promise result = new Promise();
        tickets.getTicket(ord.getTraderKey()).then( (sigFin, e) -> {
            matcherMap.get(ord.getInstrumentKey()).delOrder(ord).then((r,err) -> {
                if ( r == null ) {
                    result.receive("Order " + ord.getRecordKey() + " not found.", null);
                    sigFin.signal();
                    return;
                }
                if ( ord.getQty() != r.getQty() ) {
                    result.receive("Partial Order deleted. Has already been matched partially: " + (ord.getQty() - r.getQty()), null);
                    orders.$remove(ord.getRecordKey(),MATCHER_ID);
                    orderDeletionBalanceUpdate(ord);
                    sigFin.signal();
                    return;
                }
                result.receive((ord.isBuy() ? "Buy" : "Sell") + " Order deleted. [" + ord.getInstrumentKey() + " " + ord.getQty() + "@" + (ord.getLimitPrice() / 100) + "]", null);
                orders.$remove(ord.getRecordKey(),MATCHER_ID);
                orderDeletionBalanceUpdate(ord);
                sigFin.signal();
            });
        });
        return result;
    }

    private void orderDeletionBalanceUpdate(Order ord) {
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

            int prevQty = posAsset.getAvaiable();
            if ( ord.isBuy() ) {
                // release cash margin of order
                cashAsset.setMargined( cashAsset.getMargined() - ord.getQty() * ord.getLimitPrice() );
                // adjust open buy
                posAsset.setOpenBuyQty(posAsset.getOpenBuyQty()-ord.getQty());
            } else {
                // free part of initally reserved margin when order was added
                cashAsset.setMargined( cashAsset.getMargined() - (1000-ord.getLimitPrice()) * ord.getQty() );
                // adjust asset position
                posAsset.setOpenSellQty( posAsset.getOpenSellQty() - ord.getQty() );
            }
            int currMargin = cashAsset.getMargined();
            if ( prevQty < 0 ) {
                currMargin = currMargin - Math.abs(prevQty) * 1000;
            }
            if ( posAsset.getQty() < 0 ) {
                currMargin = currMargin + Math.abs(posAsset.getQty()) * 1000;
            }
            cashAsset.setMargined(currMargin);

            rl.getTable("Asset").$put( posAsset.getRecordKey(), posAsset, MATCHER_ID );
            rl.getTable("Asset").$put( cashAsset.getRecordKey(), cashAsset, MATCHER_ID );
        });
    }

}
