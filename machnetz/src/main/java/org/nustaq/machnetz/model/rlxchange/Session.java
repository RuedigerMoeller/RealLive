package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.KeyLen;

/**
 * Created by ruedi on 27.07.14.
 */
@KeyLen(40) // traderid + sessionid
public class Session extends Record {

    String traderKey;
    String loginTime; // js ..

    int subscriptions;


}
