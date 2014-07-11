package org.nustaq.machnetz.model;

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

        classes.add(TestRecord.class);

        return classes;
    }
}
