package de.ruedigermoeller.reallive.query;

import de.ruedigermoeller.heapoff.structs.FSTStruct;
import de.ruedigermoeller.reallive.facade.collection.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 03.11.13
 * Time: 00:31
 * To change this template use File | Settings | File Templates.
 */

/**
 * utility to reduce clutter. Use with import static de.ruedigermoeller.reallive.query.RLQueryHelper.*
 */
public class RLQueryHelper {

    /**
     *
     * @param anonymous
     * @return [Field,oldValue,..]
     */
    public static ArrayList nullOuterThis(Object anonymous) {
        ArrayList res = new ArrayList();
        if ( anonymous == null )
            return res;
        Field[] fields = anonymous.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if ( field.getName().startsWith("this$") ) {
                field.setAccessible(true);
                try {
                    res.add(field);
                    res.add(field.get(anonymous));
                    field.set(anonymous,null);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return res;
    }

    /**
     * reset refs to outer classes from arraylist
     * @param arrayList
     * @param anon
     */
    public static void restoreOuterThis(ArrayList arrayList, Object anon) {
        for (int i = 0; i < arrayList.size(); i+=2) {
            Field f = (Field) arrayList.get(i);
            Object val = arrayList.get(i+1);
            try {
                f.set(anon,val);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static class DumpTable extends Select {
        public DumpTable(RLChangeSource source) {
            super(source, alwaysTrue());
        }

        @Override
        public void added(long version, RLRow added) {
            System.out.println(added);
        }

        @Override
        public void queryFinished(Object error) {
            if ( error == null ) {
                System.out.println("-done-");
            } else
            {
                System.out.println("query error");
            }
            super.queryFinished(error);
        }
    }

    public static class Select<T extends RLRow> extends RLChangeTargetAdapter<T> implements RLRowMatcher<T> {
        CountDownLatch latch = new CountDownLatch(1);
        RLRowMatcher<T> matcher;
        Object error;

        public Select(RLChangeSource<T> source, RLRowMatcher<T> matcher) {
            this.matcher = matcher;
            nullOuterThis(matcher);
            source.select( matcher, this );
        }

        final public boolean matches(T row) {
            return matcher.matches(row);
        }

        @Override
        public void queryFinished(Object error) {
            if ( error != null )
                this.error = error;
            latch.countDown();
        }

        /**
         * @return null if successfull
         */
        public Object getError() {
            return error;
        }

        public void waitForQueryFinished() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void dumpTable( RLChangeSource s ) {
        new DumpTable(s).waitForQueryFinished();
    }

    public static <T extends RLRow> int count(RLChangeSource<T> s, final RLRowMatcher<T> filter) {
        final AtomicInteger count = new AtomicInteger(0);
        new Select<T>(s,filter) {
            @Override
            public void added(long version, T added) {
                count.incrementAndGet();
            }
        }.waitForQueryFinished();
        return count.get();
    }

    public static RLRowMatcher alwaysTrue() {
        return new RLRowMatcher() {
            @Override
            public boolean matches(RLRow row) {
                return true;
            }
        };
    }

    public static <T extends RLRow> List<T> asList( RLChangeSource<T> s, final boolean copy /*in case in process*/ ) {
        return asList(s,alwaysTrue(),copy);
    }

    public static <T extends RLRow> List<T> asList( RLChangeSource<T> s, final RLRowMatcher<T> matcher0, final boolean copy /*in case in process*/ ) {
        final ArrayList<T> result = new ArrayList<>();
        new Select<T>(s,matcher0) {
            @Override
            public void added(long version, T added) {
                if ( copy ) {
                    result.add((T) ((FSTStruct)added).createCopy());
                } else {
                    result.add((T) ((FSTStruct)added).detach());
                }
            }
        }.waitForQueryFinished();
        return result;
    }

    public static class Subscribe<T extends RLRow> extends RLChangeTargetAdapter<T> implements RLRowMatcher<T> {
        final RLChangeSource<T> source;
        RLSubscription subs;
        volatile boolean unsubscribed = false;

        RLRowMatcher<T> matcher;

        public Subscribe(RLChangeSource<T> source, RLRowMatcher<T> matcher) {
            this.source = source;
            this.matcher = matcher;
            nullOuterThis(matcher);
            subs = source.subscribe(matcher, this);
            if ( unsubscribed ) // in case single threaded
                subs.unsubscribe();
        }

        public final boolean matches(T row) {
            return matcher.matches(row);
        }

        public void unsubscribe() {
            if ( subs == null ) { // single threaded use
                unsubscribed = true;
            } else {
                subs.unsubscribe();
            }
        }

    }

}
