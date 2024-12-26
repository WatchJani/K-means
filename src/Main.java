import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int accumulationSites = 500;
        Location[] locations = new Location[accumulationSites];

        String filePath = "./src/germany.json";
        Location.loadLocations(filePath, locations);

        for (int i = 0; i < accumulationSites; i ++){
            System.out.println(i + " " + locations[i].getCapacity());
        }

        int NumberOfClusters = 10;
        KMeans cluster = new KMeans(NumberOfClusters, locations);
        cluster.fit();

        Location[] centroids = cluster.getCentroids();
        for (int i =0; i < centroids.length; i++){
            System.out.println(centroids[i].getCapacity());
        }


    }
}