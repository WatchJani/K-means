import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {
        @Override
        public void start(Stage primaryStage)throws Exception {
            int accumulationSites = 12000;
            Location[] locations = new Location[accumulationSites];

            String filePath = "./src/germany.json";
            Location.loadLocations(filePath, locations);

//            for (int i = 0; i < accumulationSites; i ++){
//                System.out.println(i + " " + locations[i].getCapacity() + " " + locations[i].getLa() + " " + locations[i].getLo());
//            }

            int NumberOfClusters = 50;
            KMeans cluster = new KMeans(NumberOfClusters, locations);
            cluster.fit();

            Location[] centroids = cluster.getCentroids();
//            for (int i =0; i < centroids.length; i++){
//                System.out.println(centroids[i].getCapacity());
//            }


            StringBuilder javascriptCode = new StringBuilder();
            if (locations != null) {
                for (int i = 0; i < locations.length; i++) {
                    javascriptCode.append("L.circleMarker([")
                            .append(locations[i].getLa())
                            .append(", ")
                            .append(locations[i].getLo())
                            .append("], { radius: 1, color: '")
                            .append(locations[i].getColor())
                            .append("', fillColor: '")
                            .append(locations[i].getColor())
                            .append("', fillOpacity: 0.8 })")
                            .append(".addTo(map)")
                            .append(".bindPopup('")
                            .append(locations[i].getName())
                            .append(" - Capacity: ")
                            .append(locations[i].getCapacity())
                            .append(" - Lo: ")
                            .append(locations[i].getLo())
                            .append(" - La: ")
                            .append(locations[i].getLa())
                            .append("');\n");
                }
            }

            if (centroids != null) {
                for (Location centroid : centroids) {
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