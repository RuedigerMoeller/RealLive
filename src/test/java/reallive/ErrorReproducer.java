package reallive;

import net.openhft.collections.SharedHashMapBuilder;

import java.io.*;
import java.util.Map;

/**
 * Created by ruedi on 25.06.14.
 */
public class ErrorReproducer implements Serializable {

    String dummy;

    public ErrorReproducer() {
        int count = (int) (Math.random()*100);
        for (int i = 0; i < count; i++) {
            dummy+="POK";
        }
    }

    public static void main(String arg[]) throws IOException {
        Map map = new SharedHashMapBuilder().entrySize(256).minSegments(100).actualEntriesPerSegment(5 * 1000).create(new File("/tmp/test"+Math.random()+".bin"), String.class, StupidCycle.class);
    }

}
