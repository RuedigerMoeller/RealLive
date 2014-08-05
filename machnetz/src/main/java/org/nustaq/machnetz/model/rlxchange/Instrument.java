package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.*;

/**
 * Created by ruedi on 18.07.14.
 *
 * mnemonic is key
 */
@KeyLen(40) // instr template id # marketid
public class Instrument extends Record {

    @ColOrder(3)
    String description;
    @ColOrder(5) @DisplayWidth("50px")
    long expiryDate;
    @ColOrder(4)
    String expiryDateString;
    @RenderStyle("Qty") @ColOrder(1) @BGColor("rgba(0,0,0,0.2)") @DisplayWidth("110px")
    int contractsTraded;
    @RenderStyle("Price") @ColOrder(2) @BGColor("rgba(0,0,255,0.2)") @DisplayWidth("110px")
    int volumeTraded;

    String marketPlace;
    String instrumentName;

    public Instrument() {
    }

    public Instrument(String key, String description, long expiryDate, String expiryDateString) {
        super(key);
        this.description = description;
        this.expiryDate = expiryDate;
        this.expiryDateString = expiryDateString;
    }

    public String getMarketPlace() {
        return marketPlace;
    }

    public void setMarketPlace(String marketPlace) {
        this.marketPlace = marketPlace;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    public int getContractsTraded() {
        return contractsTraded;
    }

    public void setContractsTraded(int contractsTraded) {
        this.contractsTraded = contractsTraded;
    }

    public int getVolumeTraded() {
        return volumeTraded;
    }

    public void setVolumeTraded(int volumeTraded) {
        this.volumeTraded = volumeTraded;
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
