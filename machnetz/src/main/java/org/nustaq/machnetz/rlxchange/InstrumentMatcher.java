package org.nustaq.machnetz.rlxchange;

import org.nustaq.machnetz.model.rlxchange.Instrument;
import org.nustaq.machnetz.model.rlxchange.Market;
import org.nustaq.machnetz.model.rlxchange.Order;
import org.nustaq.machnetz.model.rlxchange.Trade;
import org.nustaq.reallive.ChangeBroadcast;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.client.SortedReplicatedSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by ruedi on 19.07.14.
 */
public class InstrumentMatcher {

    RLTable<Order> orders;
    RLTable<Trade> trades;
    Market market;
    Instrument instrument;
    SortedReplicatedSet<Order> buySet;
    SortedReplicatedSet<Order> sellSet;

    public InstrumentMatcher(Instrument record, RLTable<Order> orders, RLTable<Trade> trades, Market market) {
        this.instrument = record;
        this.orders = orders;
        this.trades = trades;
        this.market = market;
        init();
    }

    public void init() {
        checkThread();
        buySet = new SortedReplicatedSet<>((a,b) -> {
           if ( a.getLimitPrice() == b.getLimitPrice() )
               return (int)(a.getCreationTime() - b.getCreationTime());
            return b.getLimitPrice()-a.getLimitPrice();
        });
        sellSet = new SortedReplicatedSet<>((a,b) -> {
            if ( a.getLimitPrice() == b.getLimitPrice() )
                return (int)(a.getCreationTime() - b.getCreationTime());
            return a.getLimitPrice()-b.getLimitPrice();
        });

    }

    public void onARUChange(ChangeBroadcast<Order> change) {
        checkThread();
        // filter out self induced stuff
        if ( change.getOriginator() != Matcher.MATCHER_ID ) {
            if ( change.getOriginator() != 2 ) {
                System.out.println("unexpected change source "+change);
            }
            if ( change.getRecord().isBuy() ) {
                buySet.onChangeReceived(change);
            } else {
                sellSet.onChangeReceived(change);
            }
        }
        if ( buySet.isSnaphotFinished() && sellSet.isSnaphotFinished() ) {
            match();
        }
    }


    int tradesCreated = 0;
    Thread t = null;
    private void checkThread() {
        if ( t == null )
            t = Thread.currentThread();
        else if ( t != Thread.currentThread() )
            throw new RuntimeException("Wrong thread:"+t.getName());
    }

    public void match() {
        checkThread();
        if ( market.getRecordKey().equals("Germany")) {
            dumpOB();
        }
        while ( buySet.getSize() > 0 && sellSet.getSize() > 0 ) {
            Order bestBuy;
            Order bestSell;
            try {
                bestBuy = buySet.getTreeSet().first();
                bestSell = sellSet.getTreeSet().first();
            } catch (NoSuchElementException nse) {
                System.out.println("NSE sizes "+buySet.getSize()+" "+sellSet.getSize());
                return;
            }
            if ( bestBuy.getLimitPrice() >= bestSell.getLimitPrice() ) {
                Trade forAdd = trades.createForAdd();
                orders.prepareForUpdate(bestBuy); // mutates record !
                orders.prepareForUpdate(bestSell);

                forAdd.setTradeTime(System.currentTimeMillis());
                forAdd.setBuyOrderId(bestBuy.getRecordKey());
                forAdd.setSellOrderId(bestSell.getRecordKey());

                int trdprice = Math.min(bestBuy.getLimitPrice(), bestSell.getLimitPrice());
                int trdqty = Math.min(bestBuy.getQty(),bestSell.getQty());
                forAdd.setTradePrice(trdprice);
                forAdd.setTradeQty(trdqty);

                bestBuy.setQty(bestBuy.getQty()-trdqty);
                if ( bestBuy.getQty() == 0) {
                    orders.$remove(bestBuy.getRecordKey(), Matcher.MATCHER_ID);
                    buySet.unsafeRemove(bestBuy.getRecordKey()); // remove this order (avoid double match by async changes coming in
                } else {
                    bestBuy.$apply(Matcher.MATCHER_ID);
                    // order is modified locally anyway => cache in sync
                }

                bestSell.setQty(bestSell.getQty()-trdqty);
                if ( bestSell.getQty() == 0) {
                    orders.$remove(bestSell.getRecordKey(), Matcher.MATCHER_ID);
                    sellSet.unsafeRemove(bestSell.getRecordKey()); // remove this order (avoid double match by async changes coming in
                } else {
                    bestSell.$apply(Matcher.MATCHER_ID);
                    // order is modified locally anyway => cache in sync
                }
                forAdd.$apply(Matcher.MATCHER_ID);
                tradesCreated++;
//                if ( tradesCreated > 1000 )
//                    System.out.println("POK");
                // do not loop, will be triggered by bcasts
            } else {
                break; // nothing to match
            }
        }
        updateBstBidAsk();
    }

    private void dumpOB() {
        System.out.println("-------------------------------------------------------");
        System.out.println("Orderbook for "+market.getRecordKey());
        List<Order> buys = new ArrayList<>(buySet.getTreeSet());
        List<Order> sells = new ArrayList<>(sellSet.getTreeSet());
        for (int i = 0; i < Math.min(sells.size(),buys.size()); i++) {
            Order s = sells.get(i);
            Order b = buys.get(i);
            System.out.println("BUY/SELL "+b.getQty()+"\t"+b.getLimitPrice()+"\t \t"+s.getLimitPrice()+"\t"+s.getQty());
        }
    }

    private void updateBstBidAsk() {
        // compute top of book
        int buyQuan = 0;
        int buyPrc = 0;
        for (Iterator<Order> iterator = buySet.getTreeSet().iterator(); iterator.hasNext(); ) {
            Order next = iterator.next();
            if ( buyPrc == 0 )
                buyPrc = next.getLimitPrice();
            if ( buyPrc == next.getLimitPrice() ) {
                buyQuan += next.getQty();
            } else
                break;
        }
        int sellQuan = 0;
        int sellPrc = 0;
        for (Iterator<Order> iterator = sellSet.getTreeSet().iterator(); iterator.hasNext(); ) {
            Order next = iterator.next();
            if ( sellPrc == 0 )
                sellPrc = next.getLimitPrice();
            if ( sellPrc == next.getLimitPrice() ) {
                sellQuan += next.getQty();
            } else
                break;
        }
        market.setAsk(sellPrc);
        market.setAskQty(sellQuan);
        market.setBid(buyPrc);
        market.setBidQty(buyQuan);
        market.$apply(Matcher.MATCHER_ID);
    }

    public void snapDone(ChangeBroadcast<Order> change) {
        checkThread();
        buySet.onChangeReceived(change);
        sellSet.onChangeReceived(change);
        if ( buySet.isSnaphotFinished() && sellSet.isSnaphotFinished() ) {
            match();
        }
    }
}
