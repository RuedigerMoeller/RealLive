package de.ruedigermoeller.reallive.client.wsgate.protocol;

import de.ruedigermoeller.reallive.client.wsgate.BasicDsonMsg;

/**
 * Created by ruedi on 01.01.14.
 */
public class RemRowMsg extends BasicDsonMsg {
    long rowId;

    public RemRowMsg(long rowId) {
        this.rowId = rowId;
    }

}
