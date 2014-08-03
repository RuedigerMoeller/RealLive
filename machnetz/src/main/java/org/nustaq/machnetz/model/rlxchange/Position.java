package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.KeyLen;
import org.nustaq.reallive.sys.annotations.RenderStyle;
import org.nustaq.reallive.sys.annotations.Virtual;

/**
 * Created by ruedi on 23.07.14.
 */
@Virtual
public class Position extends Record {

    String instrKey;

    @RenderStyle("Price")
    int avgPrice;
    @RenderStyle("Price")
    int sumPrice;
    @RenderStyle("Qty")
    int qty;

    public int getSumPrice() {
        return sumPrice;
    }

    public void setSumPrice(int sumPrice) {
        this.sumPrice = sumPrice;
    }

    public String getInstrKey() {
        return instrKey;
    }

    public void setInstrKey(String instrKey) {
        this.instrKey = instrKey;
    }

    public int getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(int avgPrice) {
        this.avgPrice = avgPrice;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public void updateAvg() {
        if ( qty == 0 )
            avgPrice = 0;
        else
            avgPrice = sumPrice/qty;
    }
}
