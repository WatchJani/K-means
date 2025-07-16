import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ClosestPointTask implements Runnable {
    private final List<Location> locationsSubset;
    private final Location[] centroids;
    private final CyclicBarrier barrier;
    private final List<List<PartialCentroid>> matrix;

    public ClosestPointTask(List<Location> locationsSubset, Location[] centroids, CyclicBarrier barrier, List<List<PartialCentroid>> matrix) {
        this.locationsSubset = locationsSubset;
        this.centroids = centroids;
        this.barrier = barrier;
        this.matrix = matrix;
    }

    private int findClosestCentroid(Location loc) {
        double minDist = Double.MAX_VALUE;
        int closest = 0;
        for (int i = 0; i < centroids.length; i++) {
            double dist = loc.distance(centroids[i]);
            if (dist < minDist) {
                minDist = dist;
                closest = i;
            }
        }
        return closest;
    }

    private Location calculateCentroid(List<Location> cluster, String color) {
        double sumLa = 0, sumLo = 0, sumCapacity = 0;
        for (Location location : cluster) {
            sumLa += location.getLa();
            sumLo += location.getLo();
            sumCapacity += location.getCapacity();
        }

        int size = cluster.size();
        if (size == 0) {
            return new Location("Centroid", 0, 0, 0, color);
        }

        return new Location("Centroid", sumCapacity / size, sumLa / size, sumLo / size, color);
    }

    @Override
    public void run() {
        List<List<Location>> clusters = new java.util.ArrayList<>();
        for (int i = 0; i < centroids.length; i++) {
            clusters.add(new java.util.ArrayList<>());
        }

        // Dodeli tačke najbližem centroidu i postavi boju
        for (Location loc : locationsSubset) {
            int closest = findClosestCentroid(loc);
            loc.setColor(centroids[closest].getColor());
            clusters.get(closest).add(loc);
        }

        // Izračunaj delimične centre za svaki klaster i dodaj ih u matrix
        for (int i = 0; i < centroids.length; i++) {
            Location partialCentroid = calculateCentroid(clusters.get(i), centroids[i].getColor());
            int count = clusters.get(i).size();

            // sinhronizacija na nivou liste u matrix (koja je Collections.synchronizedList)
            matrix.get(i).add(new PartialCentroid(partialCentroid, count));
        }

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    }
}