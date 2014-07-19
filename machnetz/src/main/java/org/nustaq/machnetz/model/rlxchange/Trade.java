package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;

/**
 * Created by ruedi on 18.07.14.
 */
public class Trade extends Record {

    String buyOrderId;
    String sellOrderId;

    long tradeTime;
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
}
