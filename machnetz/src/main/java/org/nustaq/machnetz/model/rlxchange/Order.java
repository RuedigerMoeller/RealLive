package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;

/**
 * Created by ruedi on 18.07.14.
 *
 * Key is orderId
 *
 */
public class Order extends Record {

    String instrumentKey;

    boolean buy; // else sell

    int limitPrice;
    int qty;

    String traderKey;
    String originatingOrderId; // partial matches

    public String getInstrumentKey() {
        return instrumentKey;
    }

    public void setInstrumentKey(String instrumentKey) {
        this.instrumentKey = instrumentKey;
    }

    public boolean isBuy() {
        return buy;
    }

    public void setBuy(boolean buy) {
        this.buy = buy;
    }

    public int getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(int limitPrice) {
        this.limitPrice = limitPrice;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public String getTraderKey() {
        return traderKey;
    }

    public void setTraderKey(String traderKey) {
        this.traderKey = traderKey;
    }

    public String getOriginatingOrderId() {
        return originatingOrderId;
    }

    public void setOriginatingOrderId(String originatingOrderId) {
        this.originatingOrderId = originatingOrderId;
    }
}
