import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server extends Thread {
    private final int port;
    private ExecutorService executor = Executors.newCachedThreadPool();

    public Server(int port) {
        this.port = port;
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

            executor.submit(new Peer(newPeer));
        }
    }
}