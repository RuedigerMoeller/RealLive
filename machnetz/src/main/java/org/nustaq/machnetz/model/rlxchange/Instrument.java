package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.Record;

/**
 * Created by ruedi on 18.07.14.
 *
 * mnemonic is key
 */
public class Instrument extends Record {

    String description;
    long expiryDate;
    String expiryDateString;

    public Instrument() {
    }

    public Instrument(String key, String description, long expiryDate, String expiryDateString) {
        super(key);
        this.description = description;
        this.expiryDate = expiryDate;
        this.expiryDateString = expiryDateString;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getExpiryDateString() {
        return expiryDateString;
    }

    public void setExpiryDateString(String expiryDateString) {
        this.expiryDateString = expiryDateString;
    }

}
