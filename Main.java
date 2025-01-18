import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {
        @Override
        public void start(Stage primaryStage) {
            int accumulationSites = GetDialog(15000 ,"Number of accumulation point:");
            int NumberOfClusters = GetDialog(50, "Number of k cluster");

            Location[] locations = new Location[accumulationSites];
            String filePath = "./src/germany.json";
            Location.loadLocations(filePath, locations);

//          for (int i = 0; i < accumulationSites; i ++){
//              System.out.println(i + " " + locations[i].getCapacity() + " " + locations[i].getLa() + " " + locations[i].getLo());
//          }

            KMeans cluster = new KMeans(NumberOfClusters, locations);

            long startTime = System.currentTimeMillis();
            long maxDurationTestTime = 60 * 1_000;

            int numberOfIteration = 3;

            for (int i = 0; i < numberOfIteration; i++){
                if (!(System.currentTimeMillis() - startTime < maxDurationTestTime)){
                    System.out.println("not enough time for testing...");
                    break;
                }

                cluster.fit();
            }

            long endTime = System.currentTimeMillis();
            System.out.println((endTime - startTime) / numberOfIteration);

            Location[] centroids = cluster.getCentroids();
//            for (int i =0; i < centroids.length; i++){
//                System.out.println(centroids[i].getCapacity());
//            }

            //WebView is a component that enables the display of HTML content within JavaFX applications
            WebView webView = new WebView();

            //WebEngine enables loading, displaying and interacting with web content
            WebEngine webEngine = webView.getEngine();

            //listener (listener) to the change of state of the LoadWorker object.
            //LoadWorker is used to monitor the progress of loading content into WebEngine.
            //stateProperty() allows monitoring the current page loading state.
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == javafx.concurrent.Worker.State.SUCCEEDED) {
                    webEngine.executeScript(CreateJS(locations, centroids).toString()); //execute my js code
                }
            });

            File file = new File("src/map.html");
            webEngine.load(file.toURI().toString()); //loads the file that is on the disk and displays it in the WebView

            Scene scene = new Scene(webView, 800, 600);
            primaryStage.setTitle("Leaflet Map in JavaFX");
            primaryStage.setScene(scene);
            primaryStage.show();
        }

    private static StringBuilder CreateJS(Location[] locations, Location[] centroids){
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



        //L.circleMarker([45.2671, 19.8335], { radius: 10, color: 'blue', fillColor: 'blue', fillOpacity: 0.8 })
        //.addTo(map)
        //          .bindPopup('Center A - Capacity: 100 - Lo: 19.8335 - La: 45.2671');

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

        return javascriptCode;
    }

    private static int GetDialog(int defValue, String msg) {
        TextInputDialog dialog = new TextInputDialog( String.valueOf(defValue));
        dialog.setContentText(msg);

        //opens a dialog and returns a string or nil if nothing is entered
        String userInput = dialog.showAndWait().orElse(null);
        int accumulationSites;

        if (userInput != null) {
            try {
                accumulationSites = Integer.parseInt(userInput);
            } catch (NumberFormatException e) {
                accumulationSites = defValue;
            }
        } else {
            accumulationSites = defValue;
        }
        return accumulationSites;
    }

    public static void main(String[] args)  {
            launch(args);
        }
}