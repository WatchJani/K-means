import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

public class JsonPayloadParser {
    public static Payload parsePayload(String json) {
        Payload payload = new Payload();

        try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            JsonObject jsonObject = jsonReader.readObject();

            // Čitanje start i end
            int start = jsonObject.getInt("start");
            int end = jsonObject.getInt("end");

            payload.setStart(start);
            payload.setEnd(end);

            // Čitanje niza centroids
            JsonArray centroidsArray = jsonObject.getJsonArray("centroids");
            Location[] centroids = new Location[centroidsArray.size()];

            for (int i = 0; i < centroidsArray.size(); i++) {
                JsonObject locJson = centroidsArray.getJsonObject(i);

                String name = locJson.getString("name");
                double capacity = locJson.getJsonNumber("capacity").doubleValue();
                double la = locJson.getJsonNumber("la").doubleValue();
                double lo = locJson.getJsonNumber("lo").doubleValue();
                String color = locJson.getString("color");

                centroids[i] = new Location(name, capacity, la, lo, color);
            }

            payload.setCentroids(centroids);
        }

        return payload;
    }
}