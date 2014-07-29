package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.*;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by ruedi on 18.07.14.
 *
 * Key is orderId
 *
 */
public class Order extends Record {

    @ColOrder(2) @DisplayWidth("120px")
    String instrumentKey;

    @ColOrder(1)
    @RenderStyle("BS") @DisplayWidth("60px") @DisplayName("B/S")
    boolean buy; // else sell

    @ColOrder(3)
    @RenderStyle("Price") @DisplayWidth("80px")
    int limitPrice;
    @ColOrder(4)
    @RenderStyle("Qty")
    int qty;

    String text;

    @Hidden
    String traderKey;

    @Hidden
    long creationTime;

    String creationTimeString;

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
        setTimeStringFrom(creationTime);
    }

    public String getCreationTimeString() {
        return creationTimeString;
    }

    public void setCreationTimeString(String creationTimeString) {
        this.creationTimeString = creationTimeString;
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

    public void setTimeStringFrom(long timeStringFrom) {
        this.creationTimeString = DateFormat.getDateTimeInstance().format(new Date(timeStringFrom));
    }
}
