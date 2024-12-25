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
    private double la;
    private double lo;
    private String color;

    public Location(String name, double capacity, double la, double lo) {
        this.name = name;
        this.capacity = capacity;
        this.la = la;
        this.lo = lo;
        this.color ="red";
    }

    public Location(String name, double capacity, double la, double lo, String color) {
        this.name = name;
        this.capacity = capacity;
        this.la = la;
        this.lo = lo;
        this.color = color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getLa() {
        return la;
    }

    public double getLo() {
        return lo;
    }

    public double distance(Location other) {
        double latDiff = this.la - other.la;
        double lonDiff = this.lo - other.lo;
        double capacityDiff = this.capacity - other.capacity;
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff + capacityDiff * capacityDiff);
    }

    // Static method to load locations from a JSON file
    public static Location[] loadLocationsFromJson(String filePath) throws IOException {
        try (JsonReader reader = Json.createReader(new FileReader(filePath))) {
            JsonArray jsonArray = reader.readArray();
            List<Location> locations = new ArrayList<>();

            for (JsonObject jsonObject : jsonArray.getValuesAs(JsonObject.class)) {
                String name = jsonObject.getString("name");
                double capacity = jsonObject.getJsonNumber("capacity").doubleValue();

                double la = Double.parseDouble(jsonObject.getString("la"));  // Latitude as double
                double lo = Double.parseDouble(jsonObject.getString("lo"));  // Longitude as double
                locations.add(new Location(name, capacity, la, lo));
            }

            return locations.toArray(new Location[0]);
        }
    }
}
