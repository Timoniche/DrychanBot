package com.drychan.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.drychan.client.model.chess.CreatedGame;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.drychan.utils.HttpUtils.buildUrlEncodedParamsFromMap;

public class LichessClient {

    private static final String CHALLENGE_URI = "https://lichess.org/api/challenge/open";
    private static final String RATED = "rated";
    private static final String CLOCK_LIMIT = "clock.limit";
    private static final String CLOCK_INCREMENT = "clock.increment";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LichessClient() {
        objectMapper = new ObjectMapper();
        httpClient = HttpClient.newHttpClient();
    }

    public CreatedGame createGame5Plus3() {
        Map<String, String> parameters = Map.of(
                RATED, "false",
                CLOCK_LIMIT, "300",
                CLOCK_INCREMENT, "3"
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(CHALLENGE_URI))
                    .headers("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(buildUrlEncodedParamsFromMap(parameters)))
                    .build();

            HttpResponse<?> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String gameJsonResponse = response.body().toString();
            return objectMapper.readValue(gameJsonResponse, CreatedGame.class);
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            return null;
        }
    }

}
