public class Payload {
    private int start;
    private int end;
    private Location[] centroids;

    public int getStart() { return start; }
    public int getEnd() { return end; }
    public Location[] getCentroids() { return centroids; }

    public void setStart(int start) { this.start = start; }
    public void setEnd(int end) { this.end = end; }
    public void setCentroids(Location[] centroids) { this.centroids = centroids; }
}