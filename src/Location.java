import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

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
    public static void loadLocations(String filePath, Location[] locations) throws IOException {
        try (JsonReader reader = Json.createReader(new FileReader(filePath))) {
                int counter = 0;
                int locationSize =  locations.length;

                // Čitanje postojećih Location objekata iz JSON fajla
                while (counter < locationSize) {
                    try {
                        JsonObject jsonObject = reader.readObject();  // Čitanje jednog objekta iz JSON fajla
                        String name = jsonObject.getString("name");
                        double capacity = jsonObject.getJsonNumber("capacity").doubleValue();
                        double la = Double.parseDouble(jsonObject.getString("la"));  // Latitude
                        double lo = Double.parseDouble(jsonObject.getString("lo"));  // Longitude
                        
                        locations[counter] = new Location(name, capacity, la, lo);
                        counter++;
                    } catch (Exception e) {
                        // Ako nije bilo više objekata, izlazimo iz petlje
                        break;
                    }
                }

                // Ako nedostaje objekata, generišemo nasumične Location objekte
                Random random = new Random();
                while (counter < locationSize) {
                    String name = "Location_" + (counter + 1); // Generisanje imena lokacije
                    double capacity = 50.0 + (200.0 - 50.0) * random.nextDouble(); // Nasumična kapacitet
                    double la = -90 + (90 - (-90)) * random.nextDouble();  // Nasumična latituda
                    double lo = -180 + (180 - (-180)) * random.nextDouble(); // Nasumična longitud

                    locations[counter] = new Location(name, capacity, la, lo);
                    counter++;
            }
        }
    }
}
