import java.util.*;
import java.util.concurrent.*;

public class ParallelKMeans implements KMeansAlgorithm {
    private int k;
    private Location[] centroids;
    private List<Location> locations;
    private Random random = new Random();
    private ExecutorService executor = Executors.newCachedThreadPool();

    public ParallelKMeans(int k, List<Location> locations) {
        //number of groups
        this.k = k;
        //new list of centroids
        this.centroids = new Location[k];
        //my json
        this.locations = locations;

        for (int i = 0; i < k; i++) {
            this.centroids[i] = locations.get(random.nextInt(locations.size()));
            this.centroids[i].setColor(generateRandomColor());
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void fit() {
        for (int iteration = 0; iteration < 100; iteration++) {
            // find the closest centroid for every location
            int numberOfThreads = Runtime.getRuntime().availableProcessors();
            CyclicBarrier barrierClosest = new CyclicBarrier(numberOfThreads + 1);

            int size = locations.size();
            int chunkSize = (size + numberOfThreads - 1) / numberOfThreads;

            for (int i = 0; i < numberOfThreads; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, size);
                List<Location> subList = locations.subList(start, end);

                executor.submit(new ClosestPointTask(subList, centroids, k, barrierClosest));
            }

            // wait to all threads finish them work
            try {
                barrierClosest.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Group by location by color
            Map<String, Integer> colorToIndex = new HashMap<>();
            for (int i = 0; i < centroids.length; i++) {
                colorToIndex.put(centroids[i].getColor(), i);
            }

            List<List<Location>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                clusters.add(new ArrayList<>());
            }

            for (Location loc : locations) {
                Integer index = colorToIndex.get(loc.getColor());
                if (index != null) {
                    clusters.get(index).add(loc);
                }
            }

            // Calculate new centroids center
            CyclicBarrier barrierCentroids = new CyclicBarrier(k + 1);
            for (int i = 0; i < k; i++) {
                List<Location> cluster = clusters.get(i);
                String color = centroids[i].getColor();
                executor.submit(new CentroidTask(cluster, color, centroids, i, barrierCentroids));
            }

            try {
                barrierCentroids.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public Location[] getCentroids() {
        return centroids;
    }

    private String generateRandomColor() {
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}