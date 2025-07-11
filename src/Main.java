import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        int accumulationSites = GetDialog(5000 ,"Number of accumulation point:");
        int NumberOfClusters = GetDialog(50, "Number of k cluster");

        List<Location> locations = new ArrayList<>();
        String filePath = "./src/germany.json";
        Location.loadLocations(filePath, locations, accumulationSites);

        //Chosing mod fir calculation
        int choice = GetDialog(1, "Select mode:\n1 - SingleThread\n2 - MultiThread\n3 - Distributed (not implemented)");

        KMeansAlgorithm cluster;

        switch (choice) {
            case 1:
                cluster = new KMeans(NumberOfClusters, locations);
                break;
            case 2:
                cluster = new ParallelKMeans(NumberOfClusters, locations);
                break;
            case 3:
                System.out.println("Distributed mode not implemented yet. Using SingleThread mode.");
                cluster = new DistributedKMeans(NumberOfClusters, locations);
                break;
            default:
                System.out.println("Invalid choice. Using SingleThread mode.");
                cluster = new KMeans(NumberOfClusters, locations);
        }

        long startTime = System.currentTimeMillis();
        long maxDurationTestTime = 60 * 1_000; // 60 sec

        int numberOfIteration = 3;

        for (int i = 0; i < numberOfIteration; i++) {
            if (!(System.currentTimeMillis() - startTime < maxDurationTestTime)) {
                System.out.println("Not enough time for testing...");
                break;
            }
            cluster.fit();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Average fit time: " + ((endTime - startTime) / numberOfIteration) + " ms");

        Location[] centroids = cluster.getCentroids();

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == javafx.concurrent.Worker.State.SUCCEEDED) {
                webEngine.executeScript(CreateJS(locations, centroids).toString()); //execute my js code
            }
        });

        File file = new File("src/map.html");
        webEngine.load(file.toURI().toString());

        Scene scene = new Scene(webView, 800, 600);
        primaryStage.setTitle("Map");
        primaryStage.setScene(scene);
        primaryStage.show();

        cluster.shutdown();
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

    private static int GetDialog(int defValue, String msg) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(defValue));
        dialog.setContentText(msg);

        String userInput = dialog.showAndWait().orElse(null);
        int result;

        if (userInput != null) {
            try {
                result = Integer.parseInt(userInput);
            } catch (NumberFormatException e) {
                result = defValue;
            }
        } else {
            result = defValue;
        }
        return result;
    }

    public static void main(String[] args) {
        launch(args);
    }
}