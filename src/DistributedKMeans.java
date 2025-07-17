import javax.json.*;
import java.io.IOException;
import java.io.StringReader;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.*;
import java.util.concurrent.*;

public class DistributedKMeans implements KMeansAlgorithm {
    private final Random random = new Random(12345L); // fiksni seed
    private final List<Location> locations;
    private Location[] centroids;

    private final DistributedKMeansClient networkCluster = new DistributedKMeansClient(
            new String[] { "127.0.0.1", },
            new int[]    { 7777 }
    );

    public DistributedKMeans(int k, List<Location> locations) {
        this.locations = locations;
        this.centroids = new Location[k];

        for (int i = 0; i < k; i++) {
            this.centroids[i] = locations.get(random.nextInt(locations.size()));
            this.centroids[i].setColor(generateRandomColor());
        }
    }

    public void  shutdown(){

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


    @Override
    public void fit() {
        int serverCount = networkCluster.getServerCount();

        if (serverCount == 0) {
            System.out.println("Nema dostupnih servera.");
            return;
        }

        String jsonPayload = Integer.toString(locations.size()); // Broj lokacija koje želiš da server učita

        ExecutorService executor = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(serverCount);

        for (int i = 0; i < serverCount; i++) {
            executor.submit(() -> {
                try {
                    networkCluster.sendCommandAndReceiveResponse("NUMBER", jsonPayload);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for servers", e);
        }

        int maxIterations = 100;
        for (int iter = 0; iter < maxIterations; iter++) {
            int partitionSize = (locations.size() + serverCount - 1) / serverCount;

            int counter = 0;

            for (int i = 0; i < serverCount; i++) {
                int start = i * partitionSize;
                int end = Math.min(start + partitionSize, locations.size());

                if (start >= end) {
                    break;
                }
                counter++;
            }

            CountDownLatch latch2 = new CountDownLatch(counter);
            List<List<PartialCentroid>> matrix = new ArrayList<>();
            for (int i = 0; i < centroids.length; i++) {
                matrix.add(Collections.synchronizedList(new ArrayList<>()));
            }

            for (int i = 0; i < serverCount; i++) {
                int start = i * partitionSize;
                int end = Math.min(start + partitionSize, locations.size());
                if (start >= end) {break;}

                String jsonPayload2 = createJsonPayload(start, end, centroids);

                executor.submit(() -> {
                    try {
                        String response = networkCluster.sendCommandAndReceiveResponse("KMEANS", jsonPayload2);
                        List<PartialCentroid> partials = parsePartialCentroids(response);

                        for (int k = 0; k < partials.size(); k++) {
                            matrix.get(k).add(partials.get(k));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            //System.out.println("check");
                            latch2.countDown();  //
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            try {
                latch2.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for servers", e);
            }

            for (int i = 0; i < centroids.length; i++) {
                centroids[i] = calculateWeightedCentroid(matrix.get(i), centroids[i].getColor());
            }

        }
        executor.shutdown();
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

    private String generateRandomColor() {
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return String.format("#%02X%02X%02X", r, g, b);
    }


    @Override
    public Location[] getCentroids() {
        return centroids;
    }
}