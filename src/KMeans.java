import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KMeans {
    private int k;
    private Location[] centroids;
    private  Random random = new Random();

    public KMeans(int k, Location[] locations) {
        this.k = k;
        centroids = new Location[k];

        for (int i = 0; i < k; i++) {
            centroids[i] = locations[i];
            centroids[i].setColor(generateRandomColor(random));
        }
    }

    public void fit(Location[] locations) {
        long startTime = System.currentTimeMillis();
        long duration = 5000;

        while (System.currentTimeMillis() - startTime < duration) {
            List<List<Location>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                clusters.add(new ArrayList<>());
            }

            for (Location location : locations) {
                int closestCentroid = findClosestCentroid(location);
                location.setColor(centroids[closestCentroid].getColor());

              clusters.get(closestCentroid).add(location);
            }

            for (int i = 0; i < k; i++) {
                Location newCentroid = calculateCentroid(clusters.get(i), centroids[i].getColor());
                centroids[i] = newCentroid;
            }
        }
    }

    private static String generateRandomColor(Random random) {
        int red = random.nextInt(256);  // 0-255
        int green = random.nextInt(256);  // 0-255
        int blue = random.nextInt(256);  // 0-255

        // Pretvaramo brojeve u hex format i formatiramo kao #RRGGBB
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private int findClosestCentroid(Location location) {
        double minDistance = Double.MAX_VALUE;
        int closestCentroid = 0;
        for (int i = 0; i < k; i++) {
            double distance = location.distance(centroids[i]);
            if (distance < minDistance) {
                minDistance = distance;
                closestCentroid = i;
            }
        }

        return closestCentroid;
    }

    private Location calculateCentroid(List<Location> cluster, String color) {
        double sumLa = 0, sumLo = 0, sumCapacity = 0;
        for (Location location : cluster) {
            sumLa += location.getLa();
            sumLo += location.getLo();
            sumCapacity += location.getCapacity();
        }

        int size = cluster.size();
        return new Location("Centroid", sumCapacity / size, sumLa / size, sumLo / size, color);
    }

    public Location[] getCentroids() {
        return centroids;
    }
}