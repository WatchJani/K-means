import javax.json.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Peer implements Runnable {
    private Socket socket;
    private BufferedReader peerReader;
    private BufferedWriter peerWriter;
    private final Server server;

    public Peer(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;

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
        Payload payload = JsonPayloadParser.parsePayload(data);
        int start = payload.getStart();
        int end = payload.getEnd();
        Location[] centroids = payload.getCentroids();

        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        int size = end - start;
        int chunkSize = (size + numberOfThreads - 1) / numberOfThreads;

        System.out.println("size: " + size);
        System.out.println("chunk size: " + chunkSize);


        int activeThreads = 0;
        for (int i = 0; i < numberOfThreads; i++) {
            int startIndex = i * chunkSize;
            if (startIndex >= size) break;
            activeThreads++;
        }

        // Priprema matrice za delimične rezultate
        List<List<PartialCentroid>> matrix = new ArrayList<>();
        for (int i = 0; i < centroids.length; i++) {
            matrix.add(Collections.synchronizedList(new ArrayList<>()));
        }

        CyclicBarrier barrier = new CyclicBarrier(activeThreads + 1);

        // Pokretanje ClosestPointTask niti
        for (int i = 0; i < numberOfThreads; i++) {
            int startIdx = start + i * chunkSize;
            int endIdx = Math.min(startIdx + chunkSize, end);

            List<Location> subList = server.locations.subList(startIdx, endIdx);
            System.out.println("Thread " + i + ": from " + startIdx + " to " + endIdx + ", subList size = " + subList.size());
            server.executor.submit(new ClosestPointTask(subList, centroids, barrier, matrix));
        }

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
            return "erropr";
        }

        // djelomicni centroide
        JsonArrayBuilder reducedCentroids = Json.createArrayBuilder();
        for (int i = 0; i < centroids.length; i++) {
            List<PartialCentroid> partials = matrix.get(i);
            double sumLa = 0, sumLo = 0, sumCapacity = 0;
            int totalCount = 0;

            for (PartialCentroid pc : partials) {
                sumLa += pc.centroid.getLa() * pc.count;
                sumLo += pc.centroid.getLo() * pc.count;
                sumCapacity += pc.centroid.getCapacity() * pc.count;
                totalCount += pc.count;
            }

            Location reduced = totalCount == 0 ? new Location("Centroid", 0, 0, 0, centroids[i].getColor())
                    : new Location(
                    "Centroid",
                    sumCapacity / totalCount,
                    sumLa / totalCount,
                    sumLo / totalCount,
                    centroids[i].getColor());

            JsonObject json = Json.createObjectBuilder()
                    .add("name", reduced.getName())
                    .add("capacity", reduced.getCapacity())
                    .add("la", reduced.getLa())
                    .add("lo", reduced.getLo())
                    .add("color", reduced.getColor())
                    .add("count", totalCount)
                    .build();
            reducedCentroids.add(json);
        }

        JsonObject result = Json.createObjectBuilder()
                .add("centroids", reducedCentroids)
                .build();

        return result.toString();
    }

    @Override
    public void run() {
        while (true) {
            String rawMessage = waitForMessage();
            if (rawMessage == null) {
                System.out.println("Connection to peer lost...");
                break;
            }

            String[] parts = rawMessage.split(" ", 2);
            String command = parts[0].toUpperCase();
            String data = parts.length > 1 ? parts[1] : "";

            String response;
            switch (command) {
                case "NUMBER":
                    int num = Integer.parseInt(data.trim());
                    server.loadLocationsFromDisk(num);
                    response = "OK";
                    break;
                case "KMEANS":
                    response = handleKMeans(data);
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