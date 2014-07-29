package org.nustaq.machnetz.rlxchange;

import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.machnetz.model.rlxchange.*;
import org.nustaq.reallive.ChangeBroadcast;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.RealLive;
import org.nustaq.reallive.Record;
import org.nustaq.reallive.client.SortedReplicatedSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by ruedi on 19.07.14.
 */
public class InstrumentMatcher { // ready to be be an actor if needed

    RLTable<Order> orders;
    RLTable<Trade> trades;
    Market market;
    Instrument instrument;
    SortedReplicatedSet<Order> buySet;
    SortedReplicatedSet<Order> sellSet;
    Matcher matcher;

    public InstrumentMatcher(Matcher matcher, Instrument record, RLTable<Order> orders, RLTable<Trade> trades, Market market) {
        this.matcher = matcher;
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

    // return null or error
    public Future<String> addOrder(RLTable<Asset> assets, RLTable<Order> orders, Order order, Asset cashAsset, Asset positionAsset) {
        Promise<String> res = new Promise();

        if ( order.isBuy() ) {
            // cost of match must be added to cash margin
            cashAsset.setMargined( cashAsset.getMargined() +  order.getQty() * order.getLimitPrice() );
            // add to open order Qty
            positionAsset.setOpenBuyQty( positionAsset.getOpenBuyQty() + order.getQty() );

            // can we ensure there is a short position regardless of open orders ?
            int worstCaseQty = positionAsset.getAvaiable() + positionAsset.getOpenBuyQty() + order.getQty();
            if (cashAsset.getAvaiable() < 0 && worstCaseQty <= 0 ) {
                res.receiveResult(null, "Not enough cash avaiable to place Buy order.");
                return res;
            }

            // directly add
            buySet.onChangeReceived( ChangeBroadcast.NewAdd("Order", order, 0) );
            orders.$put( order.getRecordKey(), order, Matcher.MATCHER_ID ); // => will be ignored then

            assets.$put(cashAsset.getRecordKey(),cashAsset, Matcher.MATCHER_ID);
            assets.$put(positionAsset.getRecordKey(),positionAsset, Matcher.MATCHER_ID);

            res.signal();

        } else { // sell
            // cost of match must be added to cash margin
            cashAsset.setMargined( cashAsset.getMargined() +  order.getQty() * (1000-order.getLimitPrice()) );
            // add to open order Qty
            positionAsset.setOpenSellQty( positionAsset.getOpenSellQty() + order.getQty() );

            // can we ensure there is a long position regardless of open orders ?
            int worstCaseQty = positionAsset.getAvaiable() - positionAsset.getOpenSellQty() - order.getQty();
            if ( cashAsset.getAvaiable() < 0 && worstCaseQty >= 0 ) {
                res.receiveResult(null, "Not enough cash avaiable to place Sell order.");
                return res;
            }

            // directly add
            sellSet.onChangeReceived( ChangeBroadcast.NewAdd("Order", order, 0) );
            orders.$put( order.getRecordKey(), order, Matcher.MATCHER_ID ); // => will be ignored then

            assets.$put(cashAsset.getRecordKey(),cashAsset, Matcher.MATCHER_ID);
            assets.$put(positionAsset.getRecordKey(),positionAsset, Matcher.MATCHER_ID);

            res.signal();
        }

        return res;
    }

    public void onARUChange(ChangeBroadcast<Order> change) {
        checkThread();
        // filter out self induced stuff
        if ( change.getOriginator() != Matcher.MATCHER_ID ) {
//            if ( change.getOriginator() != 2 ) {
//                System.out.println("unexpected change source "+change);
//            }
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
        int matchPrc = 0;
        int matchQty = 0;
        if ( market.getRecordKey().equals("USA")) {
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
                Trade newTrade = trades.createForAdd();
                orders.prepareForUpdate(bestBuy); // mutates record !
                orders.prepareForUpdate(bestSell);

                newTrade.setInstrumentKey(instrument.getRecordKey());
                newTrade.setBuyTraderKey(bestBuy.getTraderKey());
                newTrade.setSellTraderKey(bestSell.getTraderKey());
                newTrade.setTradeTime(System.currentTimeMillis());
                newTrade.setBuyOrderId(bestBuy.getRecordKey());
                newTrade.setSellOrderId(bestSell.getRecordKey());

                int trdprice = Math.min(bestBuy.getLimitPrice(), bestSell.getLimitPrice());
                int trdqty = Math.min(bestBuy.getQty(),bestSell.getQty());
                newTrade.setTradePrice(trdprice);
                newTrade.setTradeQty(trdqty);
                matchQty = trdqty;
                matchPrc = trdprice;

                instrument.setVolumeTraded(instrument.getVolumeTraded()+matchQty*trdprice);
                instrument.setContractsTraded(instrument.getContractsTraded()+matchQty);
                instrument.$apply(Matcher.MATCHER_ID);

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
                newTrade.$apply(Matcher.MATCHER_ID);
                int volume = newTrade.getTradeQty() * newTrade.getTradePrice();
                matcher.$processMatch(bestSell, matchQty, matchPrc);
                matcher.$processMatch(bestBuy, matchQty, matchPrc);
                tradesCreated++;
//                if ( tradesCreated > 1000 )
//                    System.out.println("POK");
                // do not loop, will be triggered by bcasts
            } else {
                break; // nothing to match
            }
        }
        updateBstBidAsk(matchPrc,matchQty);
    }

    private void dumpOB() {
        System.out.println("-------------------------------------------------------"+Thread.currentThread().getName());
        System.out.println("Orderbook for "+market.getRecordKey());
        List<Order> buys = new ArrayList<>(buySet.getTreeSet());
        List<Order> sells = new ArrayList<>(sellSet.getTreeSet());
        for (int i = 0; i < Math.min(sells.size(),buys.size()); i++) {
            Order s = sells.get(i);
            Order b = buys.get(i);
            System.out.println("BUY/SELL "+b.getQty()+"\t"+b.getLimitPrice()+"\t \t"+s.getLimitPrice()+"\t"+s.getQty());
        }
    }

    private void updateBstBidAsk(int matchPrc, int matchQty) {
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
        if ( matchPrc > 0 ) {
            market.setLastPrc(matchPrc);
            market.setLastQty(matchQty);
            market.setLastMatch(System.currentTimeMillis());
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

    public Future<Order> delOrder(Order ord) {
        if ( ord.isBuy() ) {
            for (Iterator<Order> iterator = buySet.getTreeSet().iterator(); iterator.hasNext(); ) {
                Order next = iterator.next();
                if ( next.getRecordKey().equals(ord.getRecordKey()) ) {
                    buySet.unsafeRemove(next.getRecordKey());
                    return new Promise<>(next);
                }
            }
        } else {
            for (Iterator<Order> iterator = sellSet.getTreeSet().iterator(); iterator.hasNext(); ) {
                Order next = iterator.next();
                if ( next.getRecordKey().equals(ord.getRecordKey()) ) {
                    sellSet.unsafeRemove(next.getRecordKey());
                    return new Promise<>(next);
                }
            }
        }
        return new Promise(null);
    }
}
