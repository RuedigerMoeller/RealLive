package org.nustaq.machnetz.model;

import org.nustaq.machnetz.model.rlxchange.*;
import org.nustaq.reallive.sys.SysMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ruedi on 11.07.14.
 */
public class TestModel extends SysMeta {

    @Override
    public List<Class> getClasses() {
        List<Class> classes = super.getClasses();

        classes.add(Instrument.class);
        classes.add(Market.class);
        classes.add(Order.class);
        classes.add(Trade.class);
        classes.add(Trader.class);
        classes.add(Position.class);
        classes.add(Asset.class);

        return classes;
    }
}
