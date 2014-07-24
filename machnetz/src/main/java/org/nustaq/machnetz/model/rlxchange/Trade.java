package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.Hidden;
import org.nustaq.reallive.sys.annotations.RenderStyle;

/**
 * Created by ruedi on 18.07.14.
 */
public class Trade extends Record {

    @Hidden
    String buyTraderKey;
    @Hidden
    String sellTraderKey;

    String buyOrderId;
    String sellOrderId;

    long tradeTime;

    String instrumentKey;

    public String getInstrumentKey() {
        return instrumentKey;
    }

    public void setInstrumentKey(String instrumentKey) {
        this.instrumentKey = instrumentKey;
    }

    @RenderStyle("Price")
    int tradePrice;
    @RenderStyle("Qty")
    int tradeQty;

    String tradeTimeStringUTC; // js ...

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

    public long getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(long tradeTime) {
        this.tradeTime = tradeTime;
    }

    public String getTradeTimeStringUTC() {
        return tradeTimeStringUTC;
    }

    public void setTradeTimeStringUTC(String tradeTimeStringUTC) {
        this.tradeTimeStringUTC = tradeTimeStringUTC;
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
}
