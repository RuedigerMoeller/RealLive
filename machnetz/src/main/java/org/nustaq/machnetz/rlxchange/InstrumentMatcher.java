package org.nustaq.machnetz.rlxchange;

import org.nustaq.machnetz.model.rlxchange.Instrument;
import org.nustaq.machnetz.model.rlxchange.Order;
import org.nustaq.machnetz.model.rlxchange.Trade;
import org.nustaq.reallive.ChangeBroadcast;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.client.SortedReplicatedSet;

/**
 * Created by ruedi on 19.07.14.
 */
public class InstrumentMatcher {

    RLTable<Order> orders;
    RLTable<Trade> trades;
    Instrument instrument;
    SortedReplicatedSet<Order> buySet;
    SortedReplicatedSet<Order> sellSet;

    public InstrumentMatcher(Instrument record, RLTable<Order> orders, RLTable<Trade> trades) {
        this.instrument = record;
        this.orders = orders;
        this.trades = trades;
        init();
    }

    public void init() {
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
        if ( change.getRecord().getClassInfo() == null ) {
            System.out.println("POK");
        }
        if ( change.getRecord().isBuy() ) {
            buySet.onChangeReceived(change);
        } else {
            sellSet.onChangeReceived(change);
        }
        if ( buySet.isSnaphotFinished() && sellSet.isSnaphotFinished() ) {
            match();
        }
    }

    public void match() {
        if ( buySet.getSize() > 0 && sellSet.getSize() > 0 ) {
            Order bestBuy = buySet.getTreeSet().first();
            Order bestSell = sellSet.getTreeSet().first();
            if ( bestBuy.getLimitPrice() >= bestSell.getLimitPrice() ) {
                Trade forAdd = trades.createForAdd();
                orders.prepareForUpdate(bestBuy);
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
                    orders.$remove(bestBuy.getRecordKey());
                } else {
                    bestBuy.$apply();
                }

                bestSell.setQty(bestSell.getQty()-trdqty);
                if ( bestSell.getQty() == 0) {
                    orders.$remove(bestSell.getRecordKey());
                } else {
                    bestSell.$apply();
                }
                forAdd.$apply();
                // do not loop, will be triggered by bcasts
            }
        }
    }

    public void snapDone(ChangeBroadcast<Order> change) {
        buySet.onChangeReceived(change);
        sellSet.onChangeReceived(change);
        if ( buySet.isSnaphotFinished() && sellSet.isSnaphotFinished() ) {
            match();
        }
    }
}
