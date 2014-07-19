package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;

/**
 * Created by ruedi on 18.07.14.
 *
 * name is key
 *
 */
public class Trader extends Record {

    String email;
    int cashBalance;

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
}
