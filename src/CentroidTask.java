import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CentroidTask implements Runnable {
    private final List<Location> cluster;
    private final String color;
    private final Location[] centroids;
    private final int index;
    private final CyclicBarrier barrier;

    public CentroidTask(List<Location> cluster, String color, Location[] centroids, int index, CyclicBarrier barrier) {
        this.cluster = cluster;
        this.color = color;
        this.centroids = centroids;
        this.index = index;
        this.barrier = barrier;
    }

    private Location calculateCentroid(List<Location> cluster, String color) {
        if (cluster.isEmpty()) {
            return new Location("Centroid", 0, Double.NaN, Double.NaN, color);
        }
        double sumLa = 0, sumLo = 0, sumCap = 0;
        for (Location loc : cluster) {
            sumLa += loc.getLa();
            sumLo += loc.getLo();
            sumCap += loc.getCapacity();
        }
        int size = cluster.size();
        return new Location("Centroid", sumCap / size, sumLa / size, sumLo / size, color);
    }

    @Override
    public void run() {
        Location newCentroid = calculateCentroid(cluster, color);
        synchronized (centroids) {
            centroids[index] = newCentroid;
        }

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    }
}