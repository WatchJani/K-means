import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedKMeansClient {
    private final String[] host;
    private final int[] port;
    private final AtomicInteger current = new AtomicInteger(0);


    public DistributedKMeansClient(String[] host, int[] port) {
        this.host = host;
        this.port = port;
    }

    public Location sendCommandAndReceiveResponse(String command, int number, double rangeStart, double rangeEnd) throws IOException, ClassNotFoundException {
        int index = current.getAndUpdate(i -> (i + 1) % host.length); // Safe circle
        try (Socket socket = new Socket(host[index], port[index])) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());


            out.writeObject(command);
            out.writeObject(number);
            out.writeObject(rangeStart);
            out.writeObject(rangeEnd);
            out.flush();


            return (Location) in.readObject();
        }
    }
}
