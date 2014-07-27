package org.nustaq.machnetz.rlxchange;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
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
        // if there are unfinished transactions for this trader, delay next order
        if ( traderUpdateSerializer.get(ord.getTraderKey()) != null ) {
            // note: a reject would take place here ..
            delayed(5, () -> {
                self().$addOrder(ord).then((r, e) -> result.receiveResult(r, e));
            });
            return result;
        }
        // dummy ta for locking
        addToTraderTaQ(new TraderBalanceTransaction(0,0,ord.getTraderKey()));

        rl.getTable("Trader").$get(ord.getTraderKey()).then((trader, error) -> {
            try {
                if (error != null) {
                    result.receiveResult("Trader " + ord.getTraderKey() + " not found.", null);
                    return;
                }
                Trader tr = (Trader) trader;
                int money = tr.getAvaiableCash();
                if (!ord.isBuy()) {
                    if (money < ord.getQty() * (1000-ord.getLimitPrice())) {
                        result.receiveResult("Not enough cash to cover short", null);
                        return;
                    }
                    addToTraderTaQ(new TraderBalanceTransaction(0, ord.getQty() * (1000-ord.getLimitPrice()), tr.getRecordKey()));
                    rl.getTable("Order").$add(ord, 0);
                    result.receiveResult(null, null);
                } else {
                    if (money < ord.getQty() * ord.getLimitPrice()) {
                        result.receiveResult("Not enough cash to place buy order", null);
                        return;
                    }
                    addToTraderTaQ(new TraderBalanceTransaction(0, ord.getQty() * ord.getLimitPrice(), tr.getRecordKey()));
                    rl.getTable("Order").$add(ord, 0);
                    result.receiveResult(null, null);
                }
            } finally {
                rawUpdateBalance(ord.getTraderKey()); // clears lock
            }
        });

        return result;
    }

    public Future<String> $delOrder( Order ord ) {
        return new Promise("void");
    }

    public static class TraderBalanceTransaction {
        int amountToAdd;
        int marginToAdd;
        String traderKey;

        public TraderBalanceTransaction(int amountToAdd, int marginToAdd, String traderKey) {
            this.amountToAdd = amountToAdd;
            this.marginToAdd = marginToAdd;
            this.traderKey = traderKey;
        }

        public int getAmountToAdd() {
            return amountToAdd;
        }

        public void setAmountToAdd(int amountToAdd) {
            this.amountToAdd = amountToAdd;
        }

        public String getTraderKey() {
            return traderKey;
        }

        public void setTraderKey(String traderKey) {
            this.traderKey = traderKey;
        }

        public int getMarginToAdd() {
            return marginToAdd;
        }

        public void setMarginToAdd(int marginToAdd) {
            this.marginToAdd = marginToAdd;
        }

        @Override
        public String toString() {
            return "TraderBalanceTransaction{" +
                "amountToAdd=" + amountToAdd +
                ", marginToAdd=" + marginToAdd +
                ", traderKey='" + traderKey + '\'' +
                '}';
        }
    }


    HashMap<String,List<TraderBalanceTransaction>> traderUpdateSerializer = new HashMap<>();
    public void $updateBalance(TraderBalanceTransaction newTrade) {
        checkThread();
        if (addToTraderTaQ(newTrade))
            return;
        String traderKey = newTrade.getTraderKey();
        rawUpdateBalance(traderKey);
    }

    private void rawUpdateBalance(String traderKey) {
        rl.getTable("Trader").$get(traderKey).then((tr,err) -> {
            Trader trader = (Trader) tr;
            if ( tr == null ) {
                System.out.println("SEVERE ERROR: TRADER NOT FOUND "+ traderKey);
            } else {
                rl.getTable("Trader").prepareForUpdate(trader);
                int prevBlocked = trader.getMargined();
                int prevCash = trader.getCashBalance();
                boolean dump = trader.getRecordKey().equals("Ruedi");
                if ( dump ) {
                    System.out.println("PRE LOOP blocked/cash "+prevBlocked+" "+prevCash);
                }
                List<TraderBalanceTransaction> tasProc = traderUpdateSerializer.get(trader.getRecordKey());
                if ( tasProc != null && tasProc.size() > 0 ) {
                    for (int i = 0; i < tasProc.size(); i++) {
                        TraderBalanceTransaction trade = tasProc.get(i);
                        if ( dump ) {
                            System.out.println("PROCESS TA "+trade);
                        }
                        trader.setCashBalance(trade.getAmountToAdd() + trader.getCashBalance());
                        trader.setMargined(trade.getMarginToAdd() + trader.getMargined());
                    }
                    trader.$apply(0);
                    traderUpdateSerializer.remove(trader.getRecordKey());
                }
                if ( dump ) {
                    System.out.println("POST LOOP blocked/cash " + trader.getMargined() + " " + trader.getCashBalance());
                }
            }
        });
    }

    private boolean addToTraderTaQ(TraderBalanceTransaction newTrade) {
        List<TraderBalanceTransaction> tas = traderUpdateSerializer.get(newTrade.getTraderKey());
        if ( tas != null ) {
            tas.add(newTrade);
            return true;
        } else {
            tas = new ArrayList<>();
            tas.add(newTrade);
            traderUpdateSerializer.put(newTrade.getTraderKey(),tas);
        }
        return false;
    }
}
