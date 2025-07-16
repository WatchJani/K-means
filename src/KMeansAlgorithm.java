public interface KMeansAlgorithm {
    void fit();
    Location[] getCentroids();
    void shutdown();
}
