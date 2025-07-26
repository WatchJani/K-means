import mpi.MPI;
import mpi.MPIException;
import javax.json.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MPIPeer {
    private final int rank;
    private final List<Location> locations; // Each peer has its own subset of locations
    private final Server server; // Reference to server-like data structure

    public MPIPeer(int rank) {
        this.rank = rank;
        this.server = new Server(500);
        this.locations = new ArrayList<>();
    }

    public void run() throws MPIException {
        while (true) {
            // Receive command from rank 0
            char[] commandBuffer = new char[256];
            MPI.COMM_WORLD.Recv(commandBuffer, 0, commandBuffer.length, MPI.CHAR, 0, 1);
            String rawMessage = new String(commandBuffer).trim();
            if (rawMessage.equals("TERMINATE")) {
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
                case "LOCATION":
                    response = handleLocation(data);
                    break;
                default:
                    response = "ERROR Unknown command: " + command;
            }

            // Send response back to rank 0
            MPI.COMM_WORLD.Send(response.toCharArray(), 0, response.length(), MPI.CHAR, 0, 2);
        }
    }

    private String handleLocation(String data) {
        Payload payload = JsonPayloadParser.parsePayload(data);
        int start = payload.getStart();
        int end = payload.getEnd();

        List<Location> subList = server.locations.subList(start, end);
        return toJsonArray(subList);
    }

    private String toJsonArray(List<Location> locations) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (Location loc : locations) {
            JsonObjectBuilder objBuilder = Json.createObjectBuilder()
                    .add("name", loc.getName())
                    .add("capacity", loc.getCapacity())
                    .add("la", loc.getLa())
                    .add("lo", loc.getLo())
                    .add("color", loc.getColor());
            arrayBuilder.add(objBuilder);
        }

        StringWriter writer = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(writer)) {
            jsonWriter.writeArray(arrayBuilder.build());
        }

        return writer.toString();
    }

    private String handleKMeans(String data) {
        Payload payload = JsonPayloadParser.parsePayload(data);
        int start = payload.getStart();
        int end = payload.getEnd();
        Location[] centroids = payload.getCentroids();

        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        int size = end - start;
        int chunkSize = (size + numberOfThreads - 1) / numberOfThreads;

        int activeThreads = 0;
        for (int i = 0; i < numberOfThreads; i++) {
            int startIndex = i * chunkSize;
            if (startIndex >= size) break;
            activeThreads++;
        }

        // Prepare matrix for partial results
        List<List<PartialCentroid>> matrix = new ArrayList<>();
        for (int i = 0; i < centroids.length; i++) {
            matrix.add(Collections.synchronizedList(new ArrayList<>()));
        }

        CyclicBarrier barrier = new CyclicBarrier(activeThreads + 1);

        // Start ClosestPointTask threads
        for (int i = 0; i < activeThreads; i++) {
            int startIdx = start + i * chunkSize;
            int endIdx = Math.min(startIdx + chunkSize, end);

            List<Location> subList = server.locations.subList(startIdx, endIdx);
            System.out.println("Rank " + rank + ", Thread " + i + ": from " + startIdx + " to " + endIdx + ", subList size = " + subList.size());
            server.executor.submit(new ClosestPointTask(subList, centroids, barrier, matrix));
        }

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
            return "error";
        }

        // Aggregate partial centroids
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
}
