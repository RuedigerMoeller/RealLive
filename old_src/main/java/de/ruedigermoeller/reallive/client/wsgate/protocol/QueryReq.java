package de.ruedigermoeller.reallive.client.wsgate.protocol;

import de.ruedigermoeller.reallive.client.wsgate.BasicDsonMsg;
import de.ruedigermoeller.reallive.facade.collection.RLRow;
import de.ruedigermoeller.reallive.facade.collection.RLRowMatcher;

import java.io.Serializable;

/**
 * Created by ruedi on 25.12.13.
 */
public class QueryReq extends BasicDsonMsg implements Serializable, RLRowMatcher {

    boolean subscribe;
    String table;
    Expression having;

    public boolean isSubscribe() {
        return subscribe;
    }

    public void setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public Expression getHaving() {
        return having;
    }

    public void setHaving(Expression having) {
        this.having = having;
    }

    @Override
    public boolean matches(RLRow row) {
        if ( having == null )
            return true;
        return having.matches(row);
    }
}
