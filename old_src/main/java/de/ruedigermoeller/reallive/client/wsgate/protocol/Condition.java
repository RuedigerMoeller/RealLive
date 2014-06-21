package de.ruedigermoeller.reallive.client.wsgate.protocol;

import de.ruedigermoeller.heapoff.structs.structtypes.StructString;
import de.ruedigermoeller.reallive.facade.collection.RLRow;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by ruedi on 25.12.13.
 */
public class Condition extends Expression implements Serializable {

    static Method notFound;

    public static void UnFound() {};
    static {
        try {
            notFound = Condition.class.getDeclaredMethod("UnFound",new Class[0]);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    String field;
    String contains;
    Object greater;
    Object lesser;
    Object equals;
    Object greaterEq;
    Object lesserEq;
    Expression or;
    Expression and;

    transient Method get;
    @Override
    public boolean matches(RLRow row) {
        if ( field == null )
            return negated; // false
        if ( get == notFound )
            return negated;
        if ( get == null ) {
            String mName = null;
            try {
                mName = "$get"+Character.toUpperCase(field.charAt(0)) + field.substring(1);
                get = row.getClass().getMethod(mName);
            } catch (NoSuchMethodException e) {
                get = notFound;
                System.out.println("could not locate method "+mName+" in class "+row.getClass().getName());
            }
        }
        try {
            boolean matches = false;
            Object value = get.invoke(row,new Object[0] );
            if ( contains != null ) {
                matches = evalContains(value, contains);
            }
            if ( greater != null ) {
                matches = evalGreater(value, greater);
            }
            if ( lesser != null ) {
                matches = evalLesser(value, lesser);
            }
            if ( equals != null ) {
                matches = evalEquals(value, equals);
            }
            if ( greaterEq != null ) {
                matches = evalGreaterEq(value, greaterEq);
            }
            if ( lesserEq != null ) {
                matches = evalLesserEq(value, lesserEq);
            }
            if ( or != null ) {
                matches = matches || or.matches(row);
            }
            if ( and != null ) {
                matches = matches && and.matches(row);
            }
            return matches ? ! negated : negated;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e); // end query by throwing ex
        }
    }

    protected boolean evalLesserEq(Object value, Object lesserEq) {
        return false;
    }

    protected boolean evalGreaterEq(Object value, Object greaterEq) {
        return false;
    }

    protected boolean evalEquals(Object value, Object equals) {
        if ( value instanceof StructString ) {
            return ((StructString) value).compareToString(equals.toString()) == 0;
        }
        return value.equals(equals);
    }

    protected boolean evalLesser(Object value, Object lesser) {
        return false;
    }

    protected boolean evalGreater(Object value, Object contains) {
        return false;
    }

    protected boolean evalContains(Object value, String contains) {
        return value.toString().indexOf(contains) >= 0;
    }
}
