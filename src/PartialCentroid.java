import javax.json.*;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class PartialCentroid {
    public final Location centroid;
    public final int count;

    public PartialCentroid(Location centroid, int count) {
        this.centroid = centroid;
        this.count = count;
    }
}
