package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.RenderStyle;

import java.util.Date;

/**
 * Created by ruedi on 18.07.14.
 *
 * Key is orderId
 *
 */
public class Order extends Record {

    String instrumentKey;

    @RenderStyle("BS")
    boolean buy; // else sell

    @RenderStyle("Price")
    int limitPrice;
    @RenderStyle("Qty")
    int qty;

    String traderKey;
    String originatingOrderId; // partial matches
    String text;

    long creationTime;
    transient String creationTimeString;

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public String getCreationTimeString() {
        if ( creationTimeString == null )
            creationTimeString = new Date(creationTime).toString();
        return creationTimeString;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

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
