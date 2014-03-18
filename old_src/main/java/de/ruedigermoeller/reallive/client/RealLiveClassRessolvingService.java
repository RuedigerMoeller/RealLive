package de.ruedigermoeller.reallive.client;

import de.ruedigermoeller.fastcast.remoting.FCFutureResultHandler;
import de.ruedigermoeller.fastcast.remoting.FCTopicService;
import de.ruedigermoeller.fastcast.remoting.RemoteMethod;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ruedi on 12/6/13.
 */
public class RealLiveClassRessolvingService extends FCTopicService {

    @RemoteMethod(1)
    public void getClass(String className, FCFutureResultHandler<byte[]> result) {
        System.out.println("remote asks for class "+className);
        String classAsPath = className.replace('.', '/') + ".class";
        InputStream stream = getClass().getClassLoader().getResourceAsStream(classAsPath);
        try {
            result.sendResult(IOUtils.toByteArray(stream));
            System.out.println("replied to remote class "+className);
        } catch (IOException e) {
            e.printStackTrace();
            result.sendResult(null);
        }
    }


}
