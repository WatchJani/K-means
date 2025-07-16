import java.util.*;
import java.util.concurrent.*;

public class ParallelKMeans implements KMeansAlgorithm {
    private Location[] centroids;
    private List<Location> locations;
    private final Random random = new Random(12345L); // fiksni seed
    private ExecutorService executor = Executors.newCachedThreadPool();

    public ParallelKMeans(Location[] centroid, List<Location> locations) {
        //number of groups
        //new list of centroids
        this.centroids = centroid;
        //my json
        this.locations = locations;
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void fit() {
        final int maxIterations = 100;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            int numberOfThreads = Runtime.getRuntime().availableProcessors();
            int size = locations.size();

            int chunkSize = (size + numberOfThreads - 1) / numberOfThreads;

            // Koliko će niti zapravo biti pokrenuto (one koje imaju podlistu)
            int activeThreads = 0;
            for (int i = 0; i < numberOfThreads; i++) {
                int start = i * chunkSize;
                if (start >= size) break;
                activeThreads++;
            }

            List<List<PartialCentroid>> matrix = new ArrayList<>();
            for (int i = 0; i < centroids.length; i++) {
                matrix.add(Collections.synchronizedList(new ArrayList<>()));
            }

            CyclicBarrier barrier = new CyclicBarrier(activeThreads + 1); // +1 za glavnu nit

            for (int i = 0; i < activeThreads; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, size);
                List<Location> subList = locations.subList(start, end);
                executor.submit(new ClosestPointTask(subList, centroids, barrier, matrix));
            }

            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Na osnovu delimičnih centroida, racunamo pravi centar centroida
            for (int i = 0; i < centroids.length; i++) {
                centroids[i] = calculateWeightedCentroid(matrix.get(i), centroids[i].getColor());
            }
        }
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

    @Override
    public Location[] getCentroids() {
        return centroids;
    }


}