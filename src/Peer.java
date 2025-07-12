import java.io.*;
import java.net.Socket;
import java.net.Socket;

public class Peer implements Runnable {
    private Socket socket;
    private BufferedReader peerReader;
    private BufferedWriter peerWriter;

    public Peer(Socket socket) throws IOException {
        this.socket = socket;

        InputStreamReader isr = new InputStreamReader(socket.getInputStream());
        OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());

        peerReader = new BufferedReader(isr);
        peerWriter = new BufferedWriter(osw);
    }

    public String waitForMessage() {
        try {
            return peerReader.readLine();
        } catch (IOException e) {
            System.out.println("Could not read message from peer: " + e.getMessage());
            return null;
        }
    }

    public void sendMessage(String message) {
        try {
            peerWriter.write(message + "\n");
            peerWriter.flush();
        } catch (IOException e) {
            System.out.println("Could not send message to peer..." + e.getMessage());
        }
    }

    private String handleKMeans(String data) {
        return "KMEANS_RESULT (todo)";
    }

    private String handleClosest(String data) {
        return "CLOSEST_RESULT (todo)";
    }


    @Override
    public void run() {
        while (true) {
            String rawMessage = waitForMessage();
            if (rawMessage == null) {
                System.out.println("Connection to peer lost...");
                break;
            }

            System.out.println("Received: " + rawMessage);


            String[] parts = rawMessage.split(" ", 2);
            String command = parts[0].toUpperCase();
            String data = parts.length > 1 ? parts[1] : "";

            String response;
            switch (command) {
                case "KMEANS":
                    response = handleKMeans(data);
                    break;
                case "CLOSEST":
                    response = handleClosest(data);
                    break;
                default:
                    response = "ERROR Unknown command: " + command;
            }

            sendMessage(response); // Send back msg to sender
        }

        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Failed to close socket: " + e.getMessage());
        }
    }
}