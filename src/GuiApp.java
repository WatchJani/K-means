import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiApp extends Application {
    private int brojKlastera;
    private int brojLokacija;

    public static void loadLocations(String filePath, List<Location> locations) {
        try (JsonReader reader = Json.createReader(new FileReader(filePath))) {

            JsonArray jsonArray = reader.readArray();
            for (JsonObject jsonObject : jsonArray.getValuesAs(JsonObject.class)) {
                String name = jsonObject.getString("name");
                double capacity = jsonObject.getJsonNumber("capacity").doubleValue();
                double la = jsonObject.getJsonNumber("la").doubleValue();
                double lo = jsonObject.getJsonNumber("lo").doubleValue();
                String color = jsonObject.getString("color");

                locations.add(new Location(name, capacity, la, lo, color));
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }


    public void start(Stage primaryStage) {
        List<String> args = getParameters().getRaw();

        if (args.size() < 2) {
            System.err.println("Nedovoljno argumenata! Očekivano: <brojKlastera> <brojLokacija>");
            return;
        }

        try {
            brojKlastera = Integer.parseInt(args.get(0));
            brojLokacija = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            System.err.println("Argumenti nisu validni brojevi.");
            return;
        }

        System.out.println("Primljeni argumenti:");
        System.out.println("Broj klastera: " + brojKlastera);
        System.out.println("Broj lokacija: " + brojLokacija);

        String currentDir = System.getProperty("user.dir");
        System.out.println("Trenutni direktorijum: " + currentDir);

        List<Location> locations = new ArrayList<>();
        String filePath = "/home/janko/89221073_k-means/K-means/src/GUI_Location.json";
        loadLocations(filePath, locations);

        List<Location> centroidsList = new ArrayList<>();
        filePath = "/home/janko/89221073_k-means/K-means/src/GUI_Centroid.json";
        loadLocations(filePath, centroidsList);

        Location[] centroids = centroidsList.toArray(new Location[0]);

            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();

            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    webEngine.executeScript(CreateJS(locations, centroids).toString()); //execute my js code
                }
            });

            File file = new File("/home/janko/89221073_k-means/K-means/src/map.html");
            webEngine.load(file.toURI().toString());

            Scene scene = new Scene(webView, 800, 600);
            primaryStage.setTitle("Map");
            primaryStage.setScene(scene);
            primaryStage.show();
    }

    private static StringBuilder CreateJS(List<Location> locations, Location[] centroids){
        StringBuilder javascriptCode = new StringBuilder();

        if (locations != null) {
            for (Location location : locations) {
                javascriptCode.append("L.circleMarker([")
                        .append(location.getLa())
                        .append(", ")
                        .append(location.getLo())
                        .append("], { radius: 1, color: '")
                        .append(location.getColor())
                        .append("', fillColor: '")
                        .append(location.getColor())
                        .append("', fillOpacity: 0.8 })")
                        .append(".addTo(map)")
                        .append(".bindPopup('")
                        .append(location.getName())
                        .append(" - Capacity: ")
                        .append(location.getCapacity())
                        .append(" - Lo: ")
                        .append(location.getLo())
                        .append(" - La: ")
                        .append(location.getLa())
                        .append("');\n");
            }
        }

        if (centroids != null) {
            for (Location centroid : centroids) {
                if (Double.isNaN(centroid.getLo())) {
                    continue;
                }
                javascriptCode.append("L.circleMarker([")
                        .append(centroid.getLa())
                        .append(", ")
                        .append(centroid.getLo())
                        .append("], { radius: 10, color: '")
                        .append(centroid.getColor())
                        .append("', fillColor: '")
                        .append(centroid.getColor())
                        .append("', fillOpacity: 0.8 })")
                        .append(".addTo(map)")
                        .append(".bindPopup('")
                        .append(centroid.getName())
                        .append(" - Capacity: ")
                        .append(centroid.getCapacity())
                        .append(" - Lo: ")
                        .append(centroid.getLo())
                        .append(" - La: ")
                        .append(centroid.getLa())
                        .append("');\n");
            }
        }

        return javascriptCode;
    }

    public static void main(String[] args) {
        launch(args);
    }
}