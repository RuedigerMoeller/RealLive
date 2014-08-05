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
    String markets[];

    public Trader() {
    }

    public Trader(String key, String email) {
        super(key);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
