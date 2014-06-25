package reallive;

import net.openhft.collections.SharedHashMapBuilder;
import org.nustaq.model.Record;

import java.io.*;
import java.util.Map;

/**
 * Created by ruedi on 25.06.14.
 */
public class StupidCycle implements Externalizable {

    int dummy;
    Object cycle[] = { this };

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(dummy);
        out.writeObject(cycle);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        dummy = in.readInt();
        cycle = (Object[]) in.readObject();
    }

    public static void main(String arg[]) throws IOException {
        Map map = new SharedHashMapBuilder().entrySize(64).minSegments(10000).actualEntriesPerSegment(5 * 1000).create(new File("/tmp/test.bin"), String.class, StupidCycle.class);
        map.put("Test",new StupidCycle());

        StupidCycle cycle = (StupidCycle) map.get("Test");
        System.out.println(cycle);
    }

}
