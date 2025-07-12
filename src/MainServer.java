public class MainServer {
    public static void main(String[] args) {
        int port = 7777;
        Server server = new Server(port);
        server.start();
    }
}
