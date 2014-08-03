package org.nustaq.machnetz.model.rlxchange;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.Hidden;
import org.nustaq.reallive.sys.annotations.InMem;
import org.nustaq.reallive.sys.annotations.KeyLen;

/**
 * Created by ruedi on 27.07.14.
 */
@InMem @KeyLen(16) // sessionid
public class Session extends Record {

    String traderKey;
    String loginTime; // js ..

    int subscriptions;
    int requests;
    int bcasts;

    @Hidden
    long lastPing;

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public String getTraderKey() {
        return traderKey;
    }

    public void setTraderKey(String traderKey) {
        this.traderKey = traderKey;
    }

    public String getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(String loginTime) {
        this.loginTime = loginTime;
    }

    public int getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(int subscriptions) {
        this.subscriptions = subscriptions;
    }

    public int getBcasts() {
        return bcasts;
    }

    public void setBcasts(int bcasts) {
        this.bcasts = bcasts;
    }

    public long getLastPing() {
        return lastPing;
    }

    public void setLastPing(long lastPing) {
        this.lastPing = lastPing;
    }
}
