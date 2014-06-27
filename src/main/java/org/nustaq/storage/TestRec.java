package org.nustaq.storage;

import org.nustaq.model.Record;
import org.nustaq.model.Schema;
import org.nustaq.model.Table;

import java.util.Arrays;

/**
* Created by ruedi on 21.06.14.
*/
public class TestRec extends Record {

    String name = "Bla";
    String other = "Bla1";
    String another = "Bla3";

    int x = 13;
    int arr[] = {1,2,3,4,5};

    public TestRec() {
        super();
    }

    public TestRec(Record originalRecord) {
        super(originalRecord);
    }

    public TestRec(String id, Table table) {
        super(id, table);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int[] getArr() {
        return arr;
    }

    public void setArr(int[] arr) {
        this.arr = arr;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public String getAnother() {
        return another;
    }

    public void setAnother(String another) {
        this.another = another;
    }

    @Override
    public String toString() {
        return "TestRec{" +
                   "name='" + name + '\'' +
                   ", other='" + other + '\'' +
                   ", another='" + another + '\'' +
                   ", x=" + x +
                   ", arr=" + Arrays.toString(arr) +
                   '}';
    }

}
