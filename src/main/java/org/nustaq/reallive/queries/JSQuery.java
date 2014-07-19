package org.nustaq.reallive.queries;

import javax.script.ScriptException;
import java.util.function.Predicate;

/**
 * Created by ruedi on 18.07.14.
 */
public class JSQuery implements Predicate {

    public static ThreadLocal<JSQueryInterpreter> queryInterpreter = new ThreadLocal<>();

    transient JSQueryInterpreter.ScriptPredicate pred;

    String query;
    boolean alwaysTrue = false;

    public JSQuery(String query) {
        this.query = query;
        if ( query == null || query.trim().length() == 0 ) {
            alwaysTrue = true;
        }
    }

    @Override
    public boolean test(Object o) {
        if ( alwaysTrue )
            return true;
        if ( pred == null ) {
            if ( queryInterpreter.get() == null ) {
                JSQueryInterpreter in = new JSQueryInterpreter();
                queryInterpreter.set(in);
            }
            try {
                pred = queryInterpreter.get().createQuery(query);
            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
        }
        return pred.test(o);
    }
}
