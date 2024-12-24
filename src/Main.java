import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        String filePath = "./src/germany.json";
        Location[] locations = null;

        try {
            locations = Location.loadLocationsFromJson(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (locations != null) {
            for (Location location : locations) {
                System.out.println("Name: " + location.getName() +
                        ", Capacity: " + location.getCapacity() +
                        ", Latitude: " + location.getLa() +
                        ", Longitude: " + location.getLo());
            }
        }


        StringBuilder javascriptCode = new StringBuilder();
        if (locations != null) {
            for (int i = 0; i < locations.length; i+=4) {
                    javascriptCode.append("L.circleMarker([")
                            .append(locations[i].getLa())  // Latitude
                            .append(", ")
                            .append(locations[i].getLo())  // Longitude
                            .append("], { radius: 1 })")
                            .append(".addTo(map)")
                            .append(".bindPopup('")
                            .append(locations[i].getName())  // Name
                            .append(" - Capacity: ")
                            .append(locations[i].getCapacity())  // Capacity
                            .append(" - Lo: ")
                            .append(locations[i].getLo())
                            .append(" - La: ")
                            .append(locations[i].getLa())
                            .append("');\n");
            }
        }

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == javafx.concurrent.Worker.State.SUCCEEDED) {
                webEngine.executeScript(javascriptCode.toString());
            }
        });

        File file = new File("src/map.html");
        webEngine.load(file.toURI().toString());

        Scene scene = new Scene(webView, 800, 600);
        primaryStage.setTitle("Leaflet Map in JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args)  {
        launch(args);
    }
}
