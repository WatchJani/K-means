import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Location {
    private String name;
    private double capacity;
    private String la;
    private String lo;

    public Location(String name, double capacity, String la, String lo) {
        this.name = name;
        this.capacity = capacity;
        this.la = la;
        this.lo = lo;
    }

    public String getName() {
        return name;
    }

    public double getCapacity() {
        return capacity;
    }

    public String getLa() {
        return la;
    }

    public String getLo() {
        return lo;
    }

    public static Location[] loadLocationsFromJson(String filePath) throws IOException {
        try (JsonReader reader = Json.createReader(new FileReader(filePath))) {
            JsonArray jsonArray = reader.readArray();
            List<Location> locations = new ArrayList<>();

            for (JsonObject jsonObject : jsonArray.getValuesAs(JsonObject.class)) {
                String name = jsonObject.getString("name");
                double capacity = jsonObject.getJsonNumber("capacity").doubleValue();
                String la = jsonObject.getString("la");
                String lo = jsonObject.getString("lo");
                locations.add(new Location(name, capacity, la, lo));
            }

            return locations.toArray(new Location[0]);
        }
    }
}
