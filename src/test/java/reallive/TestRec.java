package reallive;

import org.nustaq.model.Record;
import org.nustaq.model.Schema;

import java.util.Arrays;

/**
* Created by ruedi on 21.06.14.
*/
class TestRec extends Record {

    String name = "Bla";
    int x = 13;
    int arr[] = {1,2,3,4,5};

    public TestRec(Record originalRecord) {
        super(originalRecord);
    }

    public TestRec(String id, Schema schema) {
        super(id, schema);
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

    @Override
    public String toString() {
        return "TestRec{" +
                "name='" + name + '\'' +
                ", x=" + x +
                ", arr=" + Arrays.toString(arr) +
                '}';
    }
}
