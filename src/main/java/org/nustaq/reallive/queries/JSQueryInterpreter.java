package org.nustaq.reallive.queries;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.ScriptFunction;

import javax.script.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static javax.script.ScriptContext.ENGINE_SCOPE;

/**
 * Created by ruedi on 18.07.14.
 */
public class JSQueryInterpreter {

    ScriptEngineManager engineManager;
    ScriptEngine nashorn;

    public static class ScriptPredicate implements Predicate {

        String query;

        public ScriptPredicate(String query) throws ScriptException {
            this.query = query;
        }

        @Override
        public boolean test(Object o) {
            return false;
        }
    }

    public JSQueryInterpreter() {
        init();
    }

    public void init() {
        engineManager = new ScriptEngineManager();
        nashorn = engineManager.getEngineByName("nashorn");
    }

    ScriptPredicate createQuery(String query) throws ScriptException {
        ScriptContext myContext = new SimpleScriptContext();
        Bindings bindings = nashorn.createBindings();
        myContext.setBindings(bindings, ENGINE_SCOPE);
        nashorn.eval("function test"+"(it) { return "+query+"; }", myContext);
        return new ScriptPredicate(query) {
            @Override
            public boolean test(Object o) {
                Object res;
                try {
                    Object fun = bindings.get("test");
                    res = ((ScriptObjectMirror)fun).call(bindings,o);
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                if ( res instanceof Boolean )
                    return ((Boolean) res).booleanValue();
                return false;
            }
        };
    }

    public static class TestClz {
        String name = "none";
        float balance = 14.55f;

        public TestClz(String name, float balance) {
            this.name = name;
            this.balance = balance;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public float getBalance() {
            return balance;
        }

        public void setBalance(float balance) {
            this.balance = balance;
        }
    }

    public static void main(String arg[]) throws ScriptException, NoSuchMethodException {
        JSQueryInterpreter in = new JSQueryInterpreter();
        in.init();


        ScriptPredicate query = in.createQuery("it.balance > 0 && it.name == 'Hallo'");
        ScriptPredicate query1 = in.createQuery("it.balance < 0");
        TestClz hallo = new TestClz("Hallo", 3.0f);
//        while( true )
        {

            long tim = System.currentTimeMillis();
            for (int i = 0; i < 1000 * 1000; i++) {
                if (!query.test(hallo)) {
                    System.out.println("POK");
                }
            }

            long dur = System.currentTimeMillis() - tim;
            System.out.println("DUR " + dur);
        }

        System.out.println(query.test(hallo));
        System.out.println(query1.test(hallo));

    }



}
