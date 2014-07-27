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

    public Asset(String key, int qty) {
        super(key);
        this.qty = qty;
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
