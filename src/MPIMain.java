import mpi.MPI;
import mpi.MPIException;

import javax.json.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MPIMain {
    private static final Random random = new Random(12345L);

    //from json to location
    public static List<Location> parseLocations(String jsonResponse) {
        List<Location> locations = new ArrayList<>();

        try (JsonReader reader = Json.createReader(new StringReader(jsonResponse))) {
            JsonArray jsonArray = reader.readArray();

            for (JsonObject obj : jsonArray.getValuesAs(JsonObject.class)) {
                String name = obj.getString("name");
                double capacity = obj.getJsonNumber("capacity").doubleValue();
                double la = obj.getJsonNumber("la").doubleValue();
                double lo = obj.getJsonNumber("lo").doubleValue();
                String color = obj.getString("color");

                Location location = new Location(name, capacity, la, lo, color);
                locations.add(location);
            }
        }

        return locations;
    }

    //write location to file
    public static void writeLocationsToFile(List<Location> locations, String filename) {
        String json = toJsonArray(locations);

        try (FileWriter fileWriter = new FileWriter(filename)) {
            fileWriter.write(json);
            System.out.println("its written in file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //convert location to json
    private static String toJsonArray(List<Location> locations) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (Location loc : locations) {
            JsonObjectBuilder objBuilder = Json.createObjectBuilder()
                    .add("name", loc.getName())
                    .add("capacity", loc.getCapacity())
                    .add("la", loc.getLa())
                    .add("lo", loc.getLo())
                    .add("color", loc.getColor());
            arrayBuilder.add(objBuilder);
        }

        StringWriter writer = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(writer)) {
            jsonWriter.writeArray(arrayBuilder.build());
        }

        return writer.toString();
    }

    //are u ready for stop
    private static boolean areEqual(Location a, Location b) {
        final double different = 0.000001;
        return Math.abs(a.getLa() - b.getLa()) < different &&
                Math.abs(a.getLo() - b.getLo()) < different &&
                Math.abs(a.getCapacity() - b.getCapacity()) < different;
    }

    //new centroid center
    public static Location calculateWeightedCentroid(List<PartialCentroid> partials, String color) {
        double sumLa = 0, sumLo = 0, sumCapacity = 0;
        int totalCount = 0;

        for (PartialCentroid pc : partials) {
            sumLa += pc.centroid.getLa() * pc.count;
            sumLo += pc.centroid.getLo() * pc.count;
            sumCapacity += pc.centroid.getCapacity() * pc.count;
            totalCount += pc.count;
        }

        if (totalCount == 0) {
            return new Location("Centroid", 0, 0, 0, color);
        }

        return new Location("Centroid", sumCapacity / totalCount, sumLa / totalCount, sumLo / totalCount, color);
    }


    private static String generateRandomColor(Random random) {
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        // format for make random color #RRGGBB
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    //from data to json
    private static String createJsonPayload(int start, int end, Location[] centroidsList) {
        JsonArrayBuilder centroidsBuilder = Json.createArrayBuilder();
        for (Location centroid : centroidsList) {
            JsonObject centroidJson = Json.createObjectBuilder()
                    .add("name", centroid.getName())
                    .add("capacity", centroid.getCapacity())
                    .add("la", centroid.getLa())
                    .add("lo", centroid.getLo())
                    .add("color", centroid.getColor())
                    .build();
            centroidsBuilder.add(centroidJson);
        }

        JsonObject jsonPayload = Json.createObjectBuilder()
                .add("start", start)
                .add("end", end)
                .add("centroids", centroidsBuilder)
                .build();

        return jsonPayload.toString();
    }

    // from json to object
    public static List<PartialCentroid> parsePartialCentroids(String json) {
        List<PartialCentroid> list = new ArrayList<>();

        try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            JsonObject root = jsonReader.readObject();
            JsonArray centroidsArray = root.getJsonArray("centroids");

            for (JsonObject c : centroidsArray.getValuesAs(JsonObject.class)) {
                double la = c.getJsonNumber("la").doubleValue();
                double lo = c.getJsonNumber("lo").doubleValue();
                double capacity = c.getJsonNumber("capacity").doubleValue();
                int count = c.getInt("count");
                String color = c.getString("color");

                Location location = new Location("Centroid", capacity, la, lo, color);
                PartialCentroid pc = new PartialCentroid(location, count);
                list.add(pc);
            }
        }

        return list;
    }

    public static void main(String[] args) throws MPIException {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (args.length < 3) {
            if (rank == 0) {
                System.out.println("Usage: MPIMain <number of cluster> <number of location> <should i show GUI>");
            }

            MPI.Finalize();
            return;
        }

        int NumberOfClusters = Integer.parseInt(args[args.length - 3]);
        int accumulationSites = Integer.parseInt(args[args.length - 2]);
        int ShowGUI = Integer.parseInt(args[args.length - 1]);

        //master
        if (rank == 0) {
            //load data for calculation
            List<Location> locations = new ArrayList<>();
            String filePath = "/home/janko/89221073_k-means/K-means/src/germany.json";
            Location.loadLocations(filePath, locations, accumulationSites);

            //create reandom centroids
            Location[] centroids = new Location[NumberOfClusters];
            for (int i = 0; i < NumberOfClusters; i++) {
                Location randomLocation = locations.get(random.nextInt(locations.size()));
                centroids[i] = randomLocation;
                centroids[i].setColor(generateRandomColor(random));
            }

            //set TIME from calculation
            long startTime = System.currentTimeMillis();
            long maxDurationTestTime = 60 * 1_000; // 60 sec

            int numberOfIteration = 3;

            for (int f = 0; f < numberOfIteration; f++) {
                //copy just because of same calculation, becouse random operation. sometime will be different amount of iteration
                Location[] copyCentroids = Arrays.copyOf(centroids, centroids.length);

                if (!(System.currentTimeMillis() - startTime < maxDurationTestTime)) {
                    System.out.println("Not enough time for testing...");
                    break;
                }

                //send to another nodes to prepare them set of data
                int[] message = new int[]{1, accumulationSites}; // 1 = NUMBER komanda
                for (int i = 1; i < size; i++) {
                    MPI.COMM_WORLD.Send(message, 0, message.length, MPI.INT, i, 0);
                }

                MPI.COMM_WORLD.Barrier();

                int maxIterations = 100;

                for (int iter = 0; iter < maxIterations; iter++) {
                    //set list of list for final calculation of centroid
                    List<List<PartialCentroid>> matrix = new ArrayList<>();
                    for (int i = 0; i < centroids.length; i++) {
                        matrix.add(Collections.synchronizedList(new ArrayList<>()));
                    }

                    //logic for dividing job
                    int blockSize = (locations.size() + size - 2) / (size - 1);

                    for (int i = 1; i < size; i++) {
                        int start = (i - 1) * blockSize;
                        int end = Math.min(start + blockSize, locations.size());
                        if (start >= end) {
                            break;
                        }

                        //create payload for job
                        String jsonPayload2 = createJsonPayload(start, end, copyCentroids);
                        byte[] jsonBytes = jsonPayload2.getBytes();
                        int length = jsonBytes.length;

                        //send command
                        int[] kmeansCommand = new int[]{2};
                        MPI.COMM_WORLD.Send(kmeansCommand, 0, kmeansCommand.length, MPI.INT, i, 0);

                        //send length of payload
                        MPI.COMM_WORLD.Send(new int[]{length}, 0, 1, MPI.INT, i, 1);
                        //send payload
                        MPI.COMM_WORLD.Send(jsonBytes, 0, length, MPI.BYTE, i, 2);
                    }

                    //wait and recive all data from privies send
                    for (int i = 1; i < size; i++) {
                        //get result length
                        int[] resultLengthBuffer = new int[1];
                        MPI.COMM_WORLD.Recv(resultLengthBuffer, 0, 1, MPI.INT, i, 3);
                        int resultLength = resultLengthBuffer[0];

                        //get result
                        byte[] resultDataBuffer = new byte[resultLength];
                        MPI.COMM_WORLD.Recv(resultDataBuffer, 0, resultLength, MPI.BYTE, i, 4);

                        //parse result
                        String resultJson = new String(resultDataBuffer);
                        List<PartialCentroid> partials = parsePartialCentroids(resultJson);
                        //insert in matrix list for final calculation centroid center
                        for (int k = 0; k < partials.size(); k++) {
                            matrix.get(k).add(partials.get(k));
                        }
                    }

                    boolean changed = false;
                    Location[] newCentroids = new Location[centroids.length];

                    //calculate new centroid and check if he change position from the last time
                    for (int i = 0; i < centroids.length; i++) {
                        Location oldCentroid = copyCentroids[i];
                        Location newCentroid = calculateWeightedCentroid(matrix.get(i), oldCentroid.getColor());

                        if (!areEqual(oldCentroid, newCentroid)) {
                            changed = true;
                        }

                        newCentroids[i] = newCentroid;
                    }
                    //set centroid from global state, for new iteration
                    copyCentroids = newCentroids;

                    if (!changed) {
                        System.out.println("Converged at iteration " + iter);
                        break;
                    }
                }

                //part for update location
                //send info for getting location color for GUI representation
                int blockSize = (locations.size() + size - 2) / (size - 1);
                for (int i = 1; i < size; i++) {
                    int start = (i - 1) * blockSize;
                    int end = Math.min(start + blockSize, locations.size());
                    if (start >= end) {
                        break;
                    }

                    //payload is similar like up
                    String jsonPayload2 = createJsonPayload(start, end, copyCentroids);

                    byte[] jsonBytes = jsonPayload2.getBytes();
                    int length = jsonBytes.length;

                    //activate command
                    int[] kmeansCommand = new int[]{3};
                    MPI.COMM_WORLD.Send(kmeansCommand, 0, kmeansCommand.length, MPI.INT, i, 0);

                    //send length
                    MPI.COMM_WORLD.Send(new int[]{length}, 0, 1, MPI.INT, i, 1);

                    //send payload
                    MPI.COMM_WORLD.Send(jsonBytes, 0, length, MPI.BYTE, i, 2);
                }

                //recieving data
                for (int i = 1; i < size; i++) {
                    int start = (i - 1) * blockSize;
                    int end = Math.min(start + blockSize, locations.size());
                    if (start >= end) {
                        break;
                    }

                    int[] resultLengthBuffer = new int[1];
                    MPI.COMM_WORLD.Recv(resultLengthBuffer, 0, 1, MPI.INT, i, 3);
                    int resultLength = resultLengthBuffer[0];

                    byte[] resultDataBuffer = new byte[resultLength];
                    MPI.COMM_WORLD.Recv(resultDataBuffer, 0, resultLength, MPI.BYTE, i, 4);

                    String resultJson = new String(resultDataBuffer);
                    List<Location> responseLocations = parseLocations(resultJson);

                    for (int j = start; j < end; j++) {
                        locations.set(j, responseLocations.get(j - start));
                    }
                }

                //put data on disk
                writeLocationsToFile(locations, "GUI_Location.json");

                //put centroids on disk
                List<Location> centroidList = Arrays.asList(copyCentroids);
                writeLocationsToFile(centroidList, "GUI_Centroid.json");
            }

            //get testing time
            long endTime = System.currentTimeMillis();
            System.out.println("Average fit time: " + ((endTime - startTime) / numberOfIteration) + " ms");

            //shout down mpi processes
            for (int i = 1; i < size; i++) {
                int[] stopCommand = new int[]{9};
                MPI.COMM_WORLD.Send(stopCommand, 0, stopCommand.length, MPI.INT, i, 0);
            }

        } else{
                List<Location> locations = new ArrayList<>();
                ExecutorService executor = Executors.newCachedThreadPool();

                while (true) {
                    int[] commandMessage = new int[3];
                    MPI.COMM_WORLD.Recv(commandMessage, 0, 4, MPI.INT, 0, 0);

                    int command = commandMessage[0];

                    switch (command) {
                        case 1: // NUMBER
                            int numberOfLocation = commandMessage[1];
                            String filePath = "/home/janko/89221073_k-means/K-means/src/germany.json";
                            Location.loadLocations(filePath, locations, numberOfLocation);
                            MPI.COMM_WORLD.Barrier();
                            break;

                        case 2: // KMEANS
                            int[] lengthBuffer = new int[1];
                            MPI.COMM_WORLD.Recv(lengthBuffer, 0, 1, MPI.INT, 0, 1);
                            int length = lengthBuffer[0];

                            byte[] dataBuffer = new byte[length];
                            MPI.COMM_WORLD.Recv(dataBuffer, 0, length, MPI.BYTE, 0, 2);

                            String data = new String(dataBuffer);
                            Payload payload = JsonPayloadParser.parsePayload(data);

                            int start = payload.getStart();
                            int end = payload.getEnd();
                            Location[] centroids = payload.getCentroids();

                            int numberOfThreads = Runtime.getRuntime().availableProcessors();
                            int partSize = end - start;
                            int chunkSize = (length + numberOfThreads - 1) / numberOfThreads;

                            int activeThreads = 0;
                            for (int i = 0; i < numberOfThreads; i++) {
                                int startIndex = i * chunkSize;
                                if (startIndex >= partSize) break;
                                activeThreads++;
                            }

                            List<List<PartialCentroid>> matrix = new ArrayList<>();
                            for (int i = 0; i < centroids.length; i++) {
                                matrix.add(Collections.synchronizedList(new ArrayList<>()));
                            }

                            CyclicBarrier barrier = new CyclicBarrier(activeThreads + 1);

                            for (int i = 0; i < activeThreads; i++) {
                                int startIdx = start + i * chunkSize;
                                int endIdx = Math.min(startIdx + chunkSize, end);

                                List<Location> subList = locations.subList(startIdx, endIdx);
                                executor.submit(new ClosestPointTask(subList, centroids, barrier, matrix));
                            }

                            try {
                                barrier.await();
                            } catch (InterruptedException | BrokenBarrierException e) {
                                Thread.currentThread().interrupt();
                            }

                            JsonArrayBuilder reducedCentroids = Json.createArrayBuilder();
                            for (int i = 0; i < centroids.length; i++) {
                                List<PartialCentroid> partials = matrix.get(i);
                                double sumLa = 0, sumLo = 0, sumCapacity = 0;
                                int totalCount = 0;

                                for (PartialCentroid pc : partials) {
                                    sumLa += pc.centroid.getLa() * pc.count;
                                    sumLo += pc.centroid.getLo() * pc.count;
                                    sumCapacity += pc.centroid.getCapacity() * pc.count;
                                    totalCount += pc.count;
                                }

                                Location reduced = totalCount == 0
                                        ? new Location("Centroid", 0, 0, 0, centroids[i].getColor())
                                        : new Location(
                                        "Centroid",
                                        sumCapacity / totalCount,
                                        sumLa / totalCount,
                                        sumLo / totalCount,
                                        centroids[i].getColor());

                                JsonObject json = Json.createObjectBuilder()
                                        .add("name", reduced.getName())
                                        .add("capacity", reduced.getCapacity())
                                        .add("la", reduced.getLa())
                                        .add("lo", reduced.getLo())
                                        .add("color", reduced.getColor())
                                        .add("count", totalCount)
                                        .build();
                                reducedCentroids.add(json);
                            }

                            JsonObject result = Json.createObjectBuilder()
                                    .add("centroids", reducedCentroids)
                                    .build();

                            String resultString = result.toString();
                            byte[] resultBytes = resultString.getBytes();

                            int[] resultLength = new int[]{resultBytes.length};
                            MPI.COMM_WORLD.Send(resultLength, 0, 1, MPI.INT, 0, 3);  // tag 3 = length of result

                            MPI.COMM_WORLD.Send(resultBytes, 0, resultBytes.length, MPI.BYTE, 0, 4);  // tag 4 = actual result
                            break;
                        case 3:
                            // 1. Get length of payload
                            int[] lengthBuffer3 = new int[1];
                            MPI.COMM_WORLD.Recv(lengthBuffer3, 0, 1, MPI.INT, 0, 1);
                            int length3 = lengthBuffer3[0];

                            // 2. Recive payload (start, end, centroids)
                            byte[] dataBuffer3 = new byte[length3];
                            MPI.COMM_WORLD.Recv(dataBuffer3, 0, length3, MPI.BYTE, 0, 2);

                            String payloadJson = new String(dataBuffer3);
                            Payload payload3 = JsonPayloadParser.parsePayload(payloadJson);

                            int start3 = payload3.getStart();
                            int end3 = payload3.getEnd();

                            // 3. Get location from offset (start, end)
                            List<Location> subList = locations.subList(start3, end3);

                            // 4. Comvert to json
                            String subListJson = toJsonArray(subList);
                            byte[] responseBytes = subListJson.getBytes();

                            // 5. Send length and data
                            MPI.COMM_WORLD.Send(new int[]{responseBytes.length}, 0, 1, MPI.INT, 0, 3);
                            MPI.COMM_WORLD.Send(responseBytes, 0, responseBytes.length, MPI.BYTE, 0, 4);

                            break;
                        case 9: // STOP
                            System.out.println("Rank " + rank + " shutdown.");
                            MPI.Finalize();
                            return;

                        default:
                            System.out.println("What is this: " + command);
                    }
                }
            }

        MPI.Finalize();

        if (rank == 0 && ShowGUI > 0) {
            System.out.println("Start GUI app...");

            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "java",
                        "--module-path", "/home/janko/89221073_k-means/openjfx-24.0.1_linux-x64_bin-sdk/javafx-sdk-24.0.1/lib",
                        "--add-modules", "javafx.controls,javafx.fxml,javafx.web",
                        "-cp", ".:/home/janko/89221073_k-means/mpj-v0_44/lib/mpj.jar:/home/janko/89221073_k-means/javax.json-1.0.4.jar",
                        "GuiApp"
                );
                pb.inheritIO();
                pb.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.exit(0);
    }
}