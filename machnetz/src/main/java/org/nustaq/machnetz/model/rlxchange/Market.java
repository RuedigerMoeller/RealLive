package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.*;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by ruedi on 18.07.14.
 */
public class Market extends Record {

    @RenderStyle("Price") @BGColor("rgba(0,0,255,0.2)") @DisplayWidth("60px") @ColOrder(2)
    int bid;
    @RenderStyle("Price") @BGColor("rgba(255,0,0,0.2)") @DisplayWidth("60px") @ColOrder(3)
    int ask;
    @RenderStyle("Qty") @ColOrder(1) @DisplayWidth("60px")
    int bidQty;
    @RenderStyle("Qty") @ColOrder(4) @DisplayWidth("60px")
    int askQty;

    @RenderStyle("Price") @ColOrder(5) @DisplayWidth("60px")
    int lastPrc;
    @RenderStyle("Qty") @ColOrder(6) @DisplayWidth("60px")
    int lastQty;

    @Hidden
    long lastMatch;
    @DisplayWidth("150px") @DisplayName("Time")
    String lastMatchTimeUTC;
    String state = "TRADE";

    public Market() {
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Market(String key, int bid, int ask, int bidQty, int askQty, int lastPrc, int lastQty, long lastMatch, String lastMatchTimeUTC) {
        super(key);
        this.bid = bid;
        this.ask = ask;
        this.bidQty = bidQty;
        this.askQty = askQty;
        this.lastPrc = lastPrc;
        this.lastQty = lastQty;
        this.lastMatch = lastMatch;
        this.lastMatchTimeUTC = lastMatchTimeUTC;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public int getAsk() {
        return ask;
    }

    public void setAsk(int ask) {
        this.ask = ask;
    }

    public int getBidQty() {
        return bidQty;
    }

    public void setBidQty(int bidQty) {
        this.bidQty = bidQty;
    }

    public int getAskQty() {
        return askQty;
    }

    public void setAskQty(int askQty) {
        this.askQty = askQty;
    }

    public int getLastPrc() {
        return lastPrc;
    }

    public void setLastPrc(int lastPrc) {
        this.lastPrc = lastPrc;
    }

    public int getLastQty() {
        return lastQty;
    }

    public void setLastQty(int lastQty) {
        this.lastQty = lastQty;
    }

    public long getLastMatch() {
        return lastMatch;
    }

    public void setLastMatch(long lastMatch) {
        this.lastMatch = lastMatch; setLastTradeStringFrom(lastMatch);
    }

    public String getLastMatchTimeUTC() {
        return lastMatchTimeUTC;
    }

    public void setLastMatchTimeUTC(String lastMatchTimeUTC) {
        this.lastMatchTimeUTC = lastMatchTimeUTC;
    }

    public void setLastTradeStringFrom(long timeStringFrom) {
        this.lastMatchTimeUTC = DateFormat.getDateTimeInstance().format(new Date(timeStringFrom));
    }

}
