import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedKMeansClient {
    private final String[] host;
    private final int[] port;
    private final AtomicInteger current = new AtomicInteger(0);


    public int getServerCount() {
        return port.length;
    }

    public DistributedKMeansClient(String[] host, int[] port) {
        this.host = host;
        this.port = port;
    }

    public String sendCommandAndReceiveResponse(String command, String payload) throws IOException {
        int index = current.getAndUpdate(i -> (i + 1) % host.length); // round robin po hostovima

        try (Socket socket = new Socket(host[index], port[index]);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.write(command + " " + payload);
            writer.newLine();
            writer.flush();

            String response = reader.readLine();
            //System.out.println(response);
            return response;
        }
    }
}
