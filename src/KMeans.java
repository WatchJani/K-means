import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KMeans {
    private int k;
    private Location[] centroids;
    private  Random random = new Random();
    private Location[] locations;

    public KMeans(int k, Location[] locations) {
        this.k = k;
        this.centroids = new Location[k];
        this.locations = locations;

        this.centroids[0] = locations[random.nextInt(locations.length)];
        this.centroids[0].setColor(generateRandomColor(random));

        for (int i = 1; i < k; i++) {
            double[] distances = new double[locations.length];
            double totalDistance = 0;

            for (int j = 0; j < locations.length; j++) {
                distances[j] = findMinDistanceToCentroids(locations[j], i);
                totalDistance += distances[j];
            }


            double randomDistance = random.nextDouble() * totalDistance;
            double cumulativeDistance = 0;
            for (int j = 0; j < locations.length; j++) {
                cumulativeDistance += distances[j];
                if (cumulativeDistance >= randomDistance) {
                    this.centroids[i] = locations[j];
                    this.centroids[i].setColor(generateRandomColor(random));
                    break;
                }
            }
        }
    }

    private double findMinDistanceToCentroids(Location location, int centroidCount) {
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < centroidCount; i++) {
            double distance = location.distance(centroids[i]);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    public void fit() {
       for(int i = 0; i < 20; i++) {
            List<List<Location>> clusters = new ArrayList<>();

            for (int j = 0; j < k; j++) {
                clusters.add(new ArrayList<>());
            }

            for (Location location : locations) {
                int closestCentroid = findClosestCentroid(location);
                location.setColor(centroids[closestCentroid].getColor());

                clusters.get(closestCentroid).add(location);
            }

            for (int j = 0; j < k; j++) {
                centroids[j] = calculateCentroid(clusters.get(j), centroids[j].getColor());
               // System.out.println(clusters.get(j).size());
            }
        }
    }

    private static String generateRandomColor(Random random) {
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

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