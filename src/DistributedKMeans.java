import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class DistributedKMeans implements KMeansAlgorithm {
    private final int k;
    private final List<Location> locations;
    private Location[] centroids;
    private final Random random = new Random();

    private final DistributedKMeansClient networkCluster = new DistributedKMeansClient(
            new String[] { "127.0.0.1", },
            new int[]    { 7777 }
    );

    public DistributedKMeans(int k, List<Location> locations) {
        this.k = k;
        this.locations = locations;
        this.centroids = new Location[k];
        for (int i = 0; i < k; i++) {
            this.centroids[i] = locations.get(random.nextInt(locations.size()));
            this.centroids[i].setColor(generateRandomColor());
        }
    }

    public void  shutdown(){

    }

    @Override
    public void fit() {
        try {
            System.out.println("work");
            Location location = networkCluster.sendCommandAndReceiveResponse("CLOSEST", 0, 0 , 0);
            System.out.println("Primljen: " + location);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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