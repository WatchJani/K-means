import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ClosestPointTask implements Runnable {
    private final List<Location> locationsSubset;
    private final Location[] centroids;
    private final CyclicBarrier barrier;
    private final int k;

    public ClosestPointTask(List<Location> locationsSubset, Location[] centroids, int k, CyclicBarrier barrier) {
        this.locationsSubset = locationsSubset;
        this.centroids = centroids;
        this.k = k;
        this.barrier = barrier;
    }

    // Funkcija za pronalazak najbližeg centroida, isto kao kod tebe
    private int findClosestCentroid(Location loc) {
        double minDist = Double.MAX_VALUE;
        int closest = 0;
        for (int i = 0; i < k; i++) {
            double dist = loc.distance(centroids[i]);
            if (dist < minDist) {
                minDist = dist;
                closest = i;
            }
        }
        return closest;
    }

    @Override
    public void run() {
        for (Location loc : locationsSubset) {
            int closest = findClosestCentroid(loc);
            // Set color on color of centroids
            loc.setColor(centroids[closest].getColor());
        }


        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    }
}
