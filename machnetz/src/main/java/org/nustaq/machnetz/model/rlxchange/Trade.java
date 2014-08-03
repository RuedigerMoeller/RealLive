package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.*;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by ruedi on 18.07.14.
 */
public class Trade extends Record {

    @Hidden
    String buyTraderKey;
    @Hidden
    String sellTraderKey;

    @Hidden
    String buyOrderId;
    @Hidden
    String sellOrderId;

    @Virtual @ColOrder(3)
    @RenderStyle("BS") @DisplayWidth("60px") @DisplayName("B/S")
    boolean isBuy;

    @Hidden
    long tradeTimeStamp;

    @ColOrder(5) @DisplayWidth("120px")
    String instrumentKey;

    @RenderStyle("Price") @ColOrder(10) @DisplayWidth("80px") @BGColor("rgba(0,0,0,0.2)")
    int tradePrice;
    @ColOrder(15)
    @RenderStyle("Qty") @DisplayWidth("60px") @DisplayName("Qty")
    int tradeQty;

    @DisplayWidth("160px") @DisplayName("Time")  @ColOrder(20)
    String tradeTime;

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public void setBuyOrderId(String buyOrderId) {
        this.buyOrderId = buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
    }

    public void setSellOrderId(String sellOrderId) {
        this.sellOrderId = sellOrderId;
    }

    public long getTradeTimeStamp() {
        return tradeTimeStamp;
    }

    public void setTradeTimeStamp(long tradeTimeStamp) {
        this.tradeTimeStamp = tradeTimeStamp;
        this.tradeTime = DateFormat.getDateTimeInstance().format(new Date(tradeTimeStamp));
    }

    public String getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(String tradeTime) {
        this.tradeTime = tradeTime;
    }

    public int getTradePrice() {
        return tradePrice;
    }

    public void setTradePrice(int tradePrice) {
        this.tradePrice = tradePrice;
    }

    public int getTradeQty() {
        return tradeQty;
    }

    public void setTradeQty(int tradeQty) {
        this.tradeQty = tradeQty;
    }

    public String getBuyTraderKey() {
        return buyTraderKey;
    }

    public void setBuyTraderKey(String buyTraderKey) {
        this.buyTraderKey = buyTraderKey;
    }

    public String getSellTraderKey() {
        return sellTraderKey;
    }

    public void setSellTraderKey(String sellTraderKey) {
        this.sellTraderKey = sellTraderKey;
    }

    public boolean isBuy() {
        return isBuy;
    }

    public void setBuy(boolean isBuy) {
        this.isBuy = isBuy;
    }

    public String getInstrumentKey() {
        return instrumentKey;
    }

    public void setInstrumentKey(String instrumentKey) {
        this.instrumentKey = instrumentKey;
    }


}
