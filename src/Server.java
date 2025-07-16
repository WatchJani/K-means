import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Thread {
    private final int port;

    public List<Location> locations = new ArrayList<>();

    public ExecutorService executor = Executors.newCachedThreadPool();

    public Server(int port) {
        this.port = port;
    }

    public synchronized void loadLocationsFromDisk(int number) {
        String filePath = "./src/germany.json";
        Location.loadLocations(filePath, locations, number);

    }


    @Override
    public void run() {
        Thread.currentThread().setName("Server");
        ServerSocket ss;

        try {
            ss = new ServerSocket(port);

        } catch (IOException e) {
            System.out.println("Couldn't start server");
            return;
        }

        System.out.println("server start to work");

        while (true) {
            Socket newPeer;
            try {
                newPeer = ss.accept();
            } catch (IOException e) {
                System.out.println("Couldn't establish connection with peer");
                continue;
            }

            System.out.println("---- New connection: ----");
            System.out.println("-> Local IP: " + newPeer.getLocalAddress());
            System.out.println("-> Local Port: " + newPeer.getLocalPort());
            System.out.println("-> Remote IP: " + newPeer.getInetAddress());
            System.out.println("-> Remote Port: " + newPeer.getPort());
            System.out.println("-------------------------");

            try {
                executor.submit(new Peer(newPeer, this));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}