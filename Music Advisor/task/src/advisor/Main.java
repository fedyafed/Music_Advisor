package advisor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {
    private Page<?> currentPage;
    private final SpotifyClient spotifyClient;
    private boolean running = true;

    public Main(SpotifyClient spotifyClient) {
        this.spotifyClient = spotifyClient;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String accessServer = "https://accounts.spotify.com";
        String resourceServer = "https://api.spotify.com";
        for (int i = 0; i < args.length; i += 2) {
            switch (args[i]) {
                case "-access":
                    accessServer = args[i + 1];
                    break;
                case "-resource":
                    resourceServer = args[i + 1];
                    break;
                case "-page":
                    Page.setItemsPerPage(Integer.parseInt(args[i + 1]));
                    break;
            }
        }

        SpotifyClient spotifyClient = new SpotifyClient(accessServer, resourceServer);
        new Main(spotifyClient).run();
    }

    public void run() throws IOException, InterruptedException {
        final Scanner scanner = new Scanner(System.in);

        do {
            try {
                final String command = scanner.nextLine();
                final String action = command.split(" ")[0];
                switch (action) {
                    case "auth":
                        spotifyClient.auth();
                        continue;
                    case "new":
                        currentPage = spotifyClient.getNewAlbumsPage();
                        break;
                    case "featured":
                        currentPage = spotifyClient.getFeaturedPlaylistPage();
                        break;
                    case "categories":
                        currentPage = spotifyClient.getCategoriesPage();
                        break;
                    case "playlists":
                        final String categoryName = command.split(" ", 2)[1];
                        currentPage = spotifyClient.getPlaylistsPage(categoryName);
                        break;
                    case "prev":
                        if (currentPage == null) {
                            continue;
                        }
                        currentPage.prev();
                        break;
                    case "next":
                        if (currentPage == null) {
                            continue;
                        }
                        currentPage.next();
                        break;
                    case "exit":
                        System.out.println("---GOODBYE!---");
                        running = false;
                        continue;
                }

                System.out.println(currentPage);
            } catch (ClientException e) {
                System.out.println(e.getMessage());
            }
        } while (running);
    }
}

class SpotifyClient {
    public static final String redirectUri = "http://localhost:" + Server.PORT;
    public static final String clientId = "444cc9f898d4428caa10708261481d49";
    public static final String clientSecret = "e7f62d6e7f444744b466b1abb2463a65";
    private static final HttpClient httpClient = HttpClient.newBuilder().build();
    public final String accessServer;
    public final String resourceServer;
    private String accessToken;

    public SpotifyClient(String accessServer, String resourceServer) {
        this.accessServer = accessServer;
        this.resourceServer = resourceServer;
    }

    public Page<Album> getNewAlbumsPage() throws InterruptedException, ClientException, IOException {
        return JsonConverter.albumConverter.parseJson(getResponse("new-releases"));
    }

    public Page<Playlist> getFeaturedPlaylistPage() throws InterruptedException, ClientException, IOException {
        return JsonConverter.playlistConverter.parseJson(getResponse("featured-playlists"));
    }

    public Page<Category> getCategoriesPage() throws InterruptedException, ClientException, IOException {
        return JsonConverter.categoryConverter.parseJson(getResponse("categories"));
    }

    public Page<Playlist> getPlaylistsPage(String categoryName) throws InterruptedException, ClientException, IOException {
        final String categoryId = getCategoriesPage().stream()
                .filter(category -> category.getName().equals(categoryName))
                .map(Category::getId)
                .findFirst()
                .orElseThrow(() -> new ClientException("Unknown category name."));
        return JsonConverter.playlistConverter.parseJson(getResponse(String.format("categories/%s/playlists", categoryId)));
    }

    public void auth() throws InterruptedException, IOException {
        String code;
        System.out.println("use this link to request the access code:");
        System.out.printf(accessServer + "/authorize?client_id=%s&redirect_uri=%s&response_type=code\n%n", clientId, redirectUri);
        final CountDownLatch latch = new CountDownLatch(1);
        try (Server server = new Server(latch)) {
            System.out.println("waiting for code...");
            latch.await();

            code = server.getCode();
            if (code == null) {
                System.out.println("Authorization code not found.");
                return;
            }

            System.out.println("code received");
        }

        System.out.println("making http request for access_token...");
        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(accessServer + "/api/token"))
                .POST(HttpRequest.BodyPublishers.ofString(String.format(
                        "grant_type=authorization_code&" +
                                "code=%s&" +
                                "redirect_uri=%s&" +
                                "client_id=%s&" +
                                "client_secret=%s",
                        code, redirectUri, clientId, clientSecret)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("response:");
        System.out.println(response.body());
        accessToken = JsonParser.parseString(response.body())
                .getAsJsonObject()
                .get("access_token")
                .getAsString();
    }

    private String getResponse(String s) throws IOException, InterruptedException, ClientException {
        if (accessToken == null) {
            throw new ClientException("Please, provide access for application.");
        }

        final HttpRequest newReleasesRequest = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .uri(URI.create(resourceServer + "/v1/browse/" + s))
                .GET()
                .build();
        final String response = httpClient.send(newReleasesRequest, HttpResponse.BodyHandlers.ofString()).body();

        final JsonObject error = JsonParser.parseString(response)
                .getAsJsonObject()
                .getAsJsonObject("error");
        if (error != null) {
            final String message = error.get("message").getAsString();
            throw new ClientException(message);
        }

        return response;
    }
}

class Page<T> extends ArrayList<T> {
    private static int itemsPerPage = 5;
    private int currentPage = 0;
    private int totalPages = 0;

    public Page(Collection<? extends T> collection) {
        super(collection);
        if (!isEmpty()) {
            currentPage = 1;
            totalPages = (int) Math.ceil((double) size() / itemsPerPage);
        }
    }

    public static void setItemsPerPage(int itemsPerPage) {
        Page.itemsPerPage = itemsPerPage;
    }

    public void next() throws ClientException {
        if (currentPage == totalPages) {
            throw new ClientException("No more pages.");
        }
        currentPage++;
    }

    public void prev() throws ClientException {
        if (currentPage <= 1) {
            throw new ClientException("No more pages.");
        }
        currentPage--;
    }

    @Override
    public String toString() {
        return stream()
                .skip((currentPage - 1) * itemsPerPage)
                .limit(itemsPerPage)
                .map(Objects::toString)
                .collect(Collectors.joining("\n\n", "",
                        String.format("\n\n---PAGE %d OF %d---", currentPage, totalPages)));
    }
}

class JsonConverter<T> {
    public static final JsonConverter<String> artistConverter = new JsonConverter<>(JsonConverter::getName);
    public static final JsonConverter<Album> albumConverter = new JsonConverter<>(
            "albums",
            album -> new Album(getName(album),
                    artistConverter.parseJsonList(album.getAsJsonObject().getAsJsonArray("artists")),
                    getLink(album))
    );
    public static final JsonConverter<Playlist> playlistConverter = new JsonConverter<>(
            "playlists",
            album -> new Playlist(getName(album), getLink(album))
    );
    public static final JsonConverter<Category> categoryConverter = new JsonConverter<>(
            "categories",
            album -> new Category(getName(album), getId(album))
    );

    private final Function<JsonElement, T> itemConverter;
    private String arrayField;

    public JsonConverter(Function<JsonElement, T> itemConverter) {
        this.itemConverter = itemConverter;
    }

    private JsonConverter(String arrayField, Function<JsonElement, T> itemConverter) {
        this.itemConverter = itemConverter;
        this.arrayField = arrayField;
    }

    public List<T> parseJsonList(JsonArray jsonArray) {
        List<T> result = new ArrayList<>();
        jsonArray.forEach(item -> result.add(itemConverter.apply(item)));
        return result;
    }

    public Page<T> parseJson(String jsonString) {
        final JsonArray jsonArray = JsonParser.parseString(jsonString)
                .getAsJsonObject()
                .getAsJsonObject(arrayField)
                .getAsJsonArray("items");
        return new Page<>(parseJsonList(jsonArray));
    }

    private static String getName(JsonElement item) {
        return item.getAsJsonObject()
                .get("name")
                .getAsString();
    }

    private static String getId(JsonElement item) {
        return item.getAsJsonObject()
                .get("id")
                .getAsString();
    }

    private static String getLink(JsonElement item) {
        return item.getAsJsonObject()
                .getAsJsonObject("external_urls")
                .get("spotify")
                .getAsString();
    }
}

class Playlist {
    private final String name;
    private final String link;

    public Playlist(String name, String link) {
        this.name = name;
        this.link = link;
    }

    @Override
    public String toString() {
        return String.format("%s\n%s", name, link);
    }
}

class Album {
    private final String name;
    private final List<String> artists;
    private final String link;

    public Album(String name, List<String> artists, String link) {
        this.name = name;
        this.artists = artists;
        this.link = link;
    }

    @Override
    public String toString() {
        return String.format("%s\n%s\n%s",
                name,
                artists.stream()
                        .collect(Collectors.joining(", ", "[", "]")),
                link);
    }
}

class Category {
    private final String name;
    private final String id;

    public Category(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }
}

class ClientException extends Exception {
    public ClientException(String message) {
        super(message);
    }
}

class Server implements Closeable {
    public static final int PORT = 8080;
    private HttpServer httpServer;
    private String code;

    public Server(CountDownLatch latch) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        httpServer.start();

        httpServer.createContext("/",
                exchange -> {
                    final String key = "code=";
                    final String query = exchange.getRequestURI().getQuery();
                    if (query != null) {
                        code = Arrays.stream(query.split("&"))
                                .filter(str -> str.startsWith(key))
                                .map(str -> str.substring(key.length()))
                                .findFirst()
                                .orElse(null);
                    }

                    String message;
                    if (code == null) {
//                        message = "Not found authorization code. Try again.";
                        message = "Authorization code not found. Try again.";
                    } else {
                        message = "Got the code. Return back to your program.";
                        latch.countDown();
                    }

                    exchange.sendResponseHeaders(code == null ? 500 : 200, message.length());
                    try (OutputStream responseBody = exchange.getResponseBody()) {
                        responseBody.write(message.getBytes());
                    }
                }
        );
    }

    @Override
    public void close() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    public String getCode() {
        return code;
    }
}
