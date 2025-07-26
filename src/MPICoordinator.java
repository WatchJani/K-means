import mpi.MPI;
import mpi.MPIException;
import javax.json.*;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MPICoordinator {
    private final Random random = new Random(12345L); // Fixed seed
    private final List<Location> locations;
    private Location[] centroids;

    public MPICoordinator(int k, List<Location> locations) throws MPIException {
        this.locations = locations;
        this.centroids = new Location[k];

        for (int i = 0; i < k; i++) {
            this.centroids[i] = locations.get(random.nextInt(locations.size()));
            this.centroids[i].setColor(generateRandomColor());
        }
    }

    public void shutdown() throws MPIException {
        if (MPI.COMM_WORLD.Rank() == 0) {
            // Send termination signal to all peers
            for (int i = 1; i < MPI.COMM_WORLD.Size(); i++) {
                MPI.COMM_WORLD.Send("TERMINATE".toCharArray(), 0, "TERMINATE".length(), MPI.CHAR, i, 1);
            }
        }
        MPI.Finalize();
    }

    private String createJsonPayload(int start, int end, Location[] centroidsList) {
        JsonArrayBuilder centroidsBuilder = Json.createArrayBuilder();
        for (Location centroid : centroidsList) {
            JsonObject centroidJson = Json.createObjectBuilder()
                    .add("name", centroid.getName())
                    .add("capacity", centroid.getCapacity())
                    .add("la", centroid.getLa())
                    .add("lo", centroid.getLo())
                    .add("color", centroid.getColor())
                    .build();
            centroidsBuilder.add(centroidJson);
        }

        JsonObject jsonPayload = Json.createObjectBuilder()
                .add("start", start)
                .add("end", end)
                .add("centroids", centroidsBuilder)
                .build();

        return jsonPayload.toString();
    }

    public void fit() throws MPIException {
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (size == 1) {
            System.out.println("Only one process available. At least two are required (1 coordinator + 1 worker).");
            return;
        }

        if (rank == 0) {
            // === Coordinator logic ===
            int serverCount = size - 1;
            int locationCount = locations.size();
            int partitionSize = (locationCount + serverCount - 1) / serverCount;

            // === 1. Broadcast location count ===
            char[] payload = new char[64];
            String countStr = Integer.toString(locationCount);
            System.arraycopy(countStr.toCharArray(), 0, payload, 0, countStr.length());
            MPI.COMM_WORLD.Bcast(payload, 0, payload.length, MPI.CHAR, 0);

            // === 2. Receive OK responses from all workers ===
            char[] dummySend = new char[3]; // unused by coordinator
            char[] gatherResponses = new char[serverCount * 3];
            MPI.COMM_WORLD.Gather(dummySend, 0, 3, MPI.CHAR, gatherResponses, 0, 3, MPI.CHAR, 0);

            for (int i = 0; i < serverCount; i++) {
                String response = new String(gatherResponses, i * 3, 3).trim();
                if (!response.equals("OK")) {
                    System.err.println("Error from peer " + (i + 1) + ": " + response);
                }
            }

            // === 3. K-Means iterations ===
            int maxIterations = 100;
            for (int iter = 0; iter < maxIterations; iter++) {
                List<List<PartialCentroid>> matrix = new ArrayList<>();
                for (int i = 0; i < centroids.length; i++) {
                    matrix.add(new ArrayList<>());
                }

                // === 3.1 Send partitioned data to workers ===
                for (int i = 0; i < serverCount; i++) {
                    int start = i * partitionSize;
                    int end = Math.min(start + partitionSize, locationCount);
                    if (start >= end) continue;

                    String json = createJsonPayload(start, end, centroids);
                    String message = "KMEANS " + json;
                    char[] msg = message.toCharArray();

                    MPI.COMM_WORLD.Send(msg, 0, msg.length, MPI.CHAR, i + 1, 1);
                }

                // === 3.2 Receive partial results ===
                for (int i = 0; i < serverCount; i++) {
                    int start = i * partitionSize;
                    int end = Math.min(start + partitionSize, locationCount);
                    if (start >= end) continue;

                    char[] buffer = new char[4096];
                    MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.CHAR, i + 1, 2);

                    String response = new String(buffer).trim();
                    List<PartialCentroid> partials = parsePartialCentroids(response);
                    for (int k = 0; k < partials.size(); k++) {
                        matrix.get(k).add(partials.get(k));
                    }
                }

                // === 3.3 Recalculate centroids ===
                boolean changed = false;
                Location[] newCentroids = new Location[centroids.length];
                for (int i = 0; i < centroids.length; i++) {
                    Location oldC = centroids[i];
                    Location newC = calculateWeightedCentroid(matrix.get(i), oldC.getColor());
                    if (!areEqual(oldC, newC)) changed = true;
                    newCentroids[i] = newC;
                }
                centroids = newCentroids;

                if (!changed) {
                    System.out.println("Convergence achieved at iteration " + iter);
                    break;
                }
            }

            // === 4. Send LOCATION command to recolor points ===
            for (int i = 0; i < serverCount; i++) {
                int start = i * partitionSize;
                int end = Math.min(start + partitionSize, locationCount);
                if (start >= end) continue;

                String json = createJsonPayload(start, end, centroids);
                String message = "LOCATION " + json;
                char[] msg = message.toCharArray();

                MPI.COMM_WORLD.Send(msg, 0, msg.length, MPI.CHAR, i + 1, 1);
            }

            // === 5. Receive updated points and replace local ones ===
            for (int i = 0; i < serverCount; i++) {
                int start = i * partitionSize;
                int end = Math.min(start + partitionSize, locationCount);
                if (start >= end) continue;

                char[] buffer = new char[8192];
                MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.CHAR, i + 1, 2);
                String response = new String(buffer).trim();
                List<Location> updated = parseLocations(response);
                for (int j = start; j < end; j++) {
                    locations.set(j, updated.get(j - start));
                }
            }

        } else {
            // === Worker logic ===
           // MPIPeer peer = new MPIPeer(rank);
           // peer.run();
        }
    }

    public static List<Location> parseLocations(String jsonResponse) {
        List<Location> locations = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(jsonResponse))) {
            JsonArray jsonArray = reader.readArray();
            for (JsonObject obj : jsonArray.getValuesAs(JsonObject.class)) {
                String name = obj.getString("name");
                double capacity = obj.getJsonNumber("capacity").doubleValue();
                double la = obj.getJsonNumber("la").doubleValue();
                double lo = obj.getJsonNumber("lo").doubleValue();
                String color = obj.getString("color");
                Location location = new Location(name, capacity, la, lo, color);
                locations.add(location);
            }
        }
        return locations;
    }

    public Location calculateWeightedCentroid(List<PartialCentroid> partials, String color) {
        double sumLa = 0, sumLo = 0, sumCapacity = 0;
        int totalCount = 0;

        for (PartialCentroid pc : partials) {
            sumLa += pc.centroid.getLa() * pc.count;
            sumLo += pc.centroid.getLo() * pc.count;
            sumCapacity += pc.centroid.getCapacity() * pc.count;
            totalCount += pc.count;
        }

        if (totalCount == 0) {
            return new Location("Centroid", 0, 0, 0, color);
        }

        return new Location("Centroid", sumCapacity / totalCount, sumLa / totalCount, sumLo / totalCount, color);
    }

    public static List<PartialCentroid> parsePartialCentroids(String json) {
        List<PartialCentroid> list = new ArrayList<>();
        try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            JsonObject root = jsonReader.readObject();
            JsonArray centroidsArray = root.getJsonArray("centroids");
            for (JsonObject c : centroidsArray.getValuesAs(JsonObject.class)) {
                double la = c.getJsonNumber("la").doubleValue();
                double lo = c.getJsonNumber("lo").doubleValue();
                double capacity = c.getJsonNumber("capacity").doubleValue();
                int count = c.getInt("count");
                String color = c.getString("color");
                Location location = new Location("Centroid", capacity, la, lo, color);
                PartialCentroid pc = new PartialCentroid(location, count);
                list.add(pc);
            }
        }
        return list;
    }

    private boolean areEqual(Location a, Location b) {
        final double different = 0.000001;
        return Math.abs(a.getLa() - b.getLa()) < different &&
                Math.abs(a.getLo() - b.getLo()) < different &&
                Math.abs(a.getCapacity() - b.getCapacity()) < different;
    }

    private String generateRandomColor() {
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public Location[] getCentroids() {
        return centroids;
    }
}
