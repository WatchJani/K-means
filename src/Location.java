import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class Location {
    private final String name;
    private double capacity;
    private double la;
    private double lo;
    private String color;
    private static final Random random = new Random(12345L);

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

    public void setLo(double lo){
        this.lo = lo;
    }

    public void setLa(double la){
        this.la = la;
    }

    public double distance(Location other) {
        double latDiff = this.la - other.la;
        double lonDiff = this.lo - other.lo;
        double capacityDiff = this.capacity - other.capacity;
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff + capacityDiff * capacityDiff);
    }

    //load locations from a JSON file
    public static void loadLocations(String filePath, List<Location> locations, int locationSize) {
        try (JsonReader reader = Json.createReader(new FileReader(filePath))) {
            int counter = 0;

            JsonArray jsonArray = reader.readArray();
            for (JsonObject jsonObject : jsonArray.getValuesAs(JsonObject.class)) {
                if (counter >= locationSize) {
                    break;
                }

                String name = jsonObject.getString("name");
                double capacity = jsonObject.getJsonNumber("capacity").doubleValue();
                double la = Double.parseDouble(jsonObject.getString("la"));
                double lo = Double.parseDouble(jsonObject.getString("lo"));


                locations.add(new Location(name, capacity / 1000, la, lo));
                counter++;
            }


            //do thaht just in case if we dont have enough accumulation poin
            while (counter < locationSize) {
                String name = "Location_" + (counter + 1);
                double capacity = 116024 *  random.nextDouble();
                double la = 48 + (54 - 48) * random.nextDouble();
                double lo = 8 + (13 - 8) * random.nextDouble();

                locations.add(new Location(name, capacity / 1000, la, lo));
                counter++;
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }
}
