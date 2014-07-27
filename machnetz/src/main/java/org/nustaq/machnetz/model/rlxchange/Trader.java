package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.BGColor;
import org.nustaq.reallive.sys.annotations.ColOrder;
import org.nustaq.reallive.sys.annotations.DisplayWidth;
import org.nustaq.reallive.sys.annotations.RenderStyle;

/**
 * Created by ruedi on 18.07.14.
 *
 * name is key
 *
 */
public class Trader extends Record {

    @ColOrder(3)
    String email;
    @RenderStyle("Price") @BGColor("rgba(0,0,255,0.2)") @DisplayWidth("100px") @ColOrder(1)
    int cashBalance;
    @RenderStyle("Price") @BGColor("rgba(0,0,0,0.2)") @DisplayWidth("100px") @ColOrder(2)
    int margined; // amount of money locked by shorts

    public Trader() {
    }

    public Trader(String key, String email, int cashBalance) {
        super(key);
        this.email = email;
        this.cashBalance = cashBalance;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(int cashBalance) {
        this.cashBalance = cashBalance;
    }

    public int getMargined() {
        return margined;
    }

    public void setMargined(int margined) {
        this.margined = margined;
    }

    public int getAvaiableCash() {
        return cashBalance-margined;
    }
}
