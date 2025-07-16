import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KMeans implements KMeansAlgorithm{
    private int k;
    private Location[] centroids;
    private final Random random = new Random(12345L); // fiksni seed
    private List<Location> locations;

    public KMeans(int k, List<Location> locations) {
        this.k = k;
        this.centroids = new Location[k];
        this.locations = locations;

        for (int i = 0; i < k; i++) {
            // Uzmi nasumičnu lokaciju iz liste
            Location randomLocation = locations.get(random.nextInt(locations.size()));
            this.centroids[i] = randomLocation;
            this.centroids[i].setColor(generateRandomColor(random));
        }
    }

    public void fit() {
       for(int i = 0; i < 100; i++) {
           //[][]
            List<List<Location>> clusters = new ArrayList<>();

            for (int j = 0; j < k; j++) {
                clusters.add(new ArrayList<>());
            }

            //go through all locations
            for (Location location : locations) {
                int closestCentroid = findClosestCentroid(location);
                location.setColor(centroids[closestCentroid].getColor()); //set color of closest centroid

                clusters.get(closestCentroid).add(location); //get array of thaht cluster and add in this group that location
            }

            for (int j = 0; j < k; j++) {
                //from group of clusters get all location and calculate new cluster position
                centroids[j] = calculateCentroid(clusters.get(j), centroids[j].getColor());
               // System.out.println(clusters.get(j).size());
            }
        }
    }

    private static String generateRandomColor(Random random) {
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        // format for make random color #RRGGBB
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

    public void  shutdown(){

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