package org.nustaq.machnetz;

import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kson.Kson;
import org.nustaq.machnetz.model.rlxchange.Order;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by ruedi on 06.08.14.
 */
public class MachNetzAdmin {

    static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    static Object readObjectFromStream(DataInputStream inputStream) throws IOException, ClassNotFoundException {
        int len = inputStream.readInt();
        byte buffer[] = new byte[len]; // this could be reused !
        while (len > 0)
            len -= inputStream.read(buffer, buffer.length - len, len);
        return conf.getObjectInput(buffer).readObject();
    }

    static void writeObjectToStream(DataOutputStream outputStream, Object toWrite) throws IOException {
        FSTObjectOutput objectOutput = conf.getObjectOutput(); // could also do new with minor perf impact
        objectOutput.writeObject(toWrite);
        outputStream.writeInt(objectOutput.getWritten());
        outputStream.write(objectOutput.getBuffer(), 0, objectOutput.getWritten());
        objectOutput.flush();
    }

    public static class AdminClient {

        Socket clientSocket;
        DataOutputStream outputStream;
        DataInputStream inputStream;

        public AdminClient() throws IOException {
            clientSocket = new Socket("localhost", 8886);
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            inputStream  = new DataInputStream(clientSocket.getInputStream());
        }

        public Object send( Object message ) throws Exception {
            writeObjectToStream(outputStream, message);
            // get response
            return readObjectFromStream(inputStream);
        }

        public void close() throws IOException {
            clientSocket.close();
        }
    }

    public static interface ServerListener {
        public Future $processMessage(Object message);
    }

    public static class AdminServer {
        ServerSocket welcomeSocket;

        public AdminServer() throws IOException {
            welcomeSocket = new ServerSocket(8886);
            System.out.println("server running on "+welcomeSocket.getLocalPort());
        }

        public void start( ServerListener listener /* expect actor */ ) throws IOException {
            while( true ) {
                Socket connectionSocket = welcomeSocket.accept();
                DataOutputStream outputStream = new DataOutputStream(connectionSocket.getOutputStream());
                DataInputStream inputStream = new DataInputStream(connectionSocket.getInputStream());
                new Thread(() -> {
                    try {
                        while (true) {
                            // read object
                            Object read = readObjectFromStream(inputStream);
                            System.out.println("received "+read);
                            listener.$processMessage(read).then((r, e) -> {
                                try {
                                    // write response
                                    writeObjectToStream(outputStream, read);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            });
                        }
                    } catch (EOFException ex) {
                        // client terminated
                        System.out.println("client terminated");
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }

    }

    public static void main(String argv[]) throws Exception
    {
        Order order = new Order();
        order._setRecordKey("BL:AAA");
        order.setText("Text");
        order.setCreationTime(System.currentTimeMillis());
        order.setBuy(true);
        order.setInstrumentKey("Germany");
        order.setLimitPrice(1000);
        order.setQty(12);
        System.out.println( new Kson().writeObject(order) );

        if ( argv.length == 0 ) {
            AdminServer tcpServer = new AdminServer();
            tcpServer.start( (message) -> {
                System.out.println("Message:"+message);
                return new Promise("Hello");
            });
        } else {
            AdminClient cl = new AdminClient();
            Object result = cl.send(argv[0]);
            System.out.println("result "+result);
        }
    }

}
