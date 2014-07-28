package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.KeyLen;

/**
 * Created by ruedi on 27.07.14.
 *
 * Trader#Instrument
 *
 */
@KeyLen(20+20+1)
public class Asset extends Record {

    int qty;
    int margined;
    int openBuyQty;
    int openSellQty;

    public Asset(String key, int qty) {
        super(key);
        this.qty = qty;
    }

    public int getOpenBuyQty() {
        return openBuyQty;
    }

    public void setOpenBuyQty(int openBuyQty) {
        this.openBuyQty = openBuyQty;
    }

    public int getOpenSellQty() {
        return openSellQty;
    }

    public void setOpenSellQty(int openSellQty) {
        this.openSellQty = openSellQty;
    }

    public int getMargined() {
        return margined;
    }

    public void setMargined(int margined) {
        this.margined = margined;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public int getAvaiable() {
        return qty-margined;
    }
}
