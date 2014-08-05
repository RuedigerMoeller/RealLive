package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;

/**
 * Created by ruedi on 04.08.2014.
 */
public class MarketPlace extends Record {

    String admin;
    String marketPlaceName;

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public String getMarketPlaceName() {
        return marketPlaceName;
    }

    public void setMarketPlaceName(String marketPlaceName) {
        this.marketPlaceName = marketPlaceName;
    }
}
