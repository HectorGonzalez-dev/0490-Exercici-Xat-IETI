package com.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatService {

    // Model configuration
    private static final String MODEL_NAME = "Qwen38";
    private static final String MODEL_URL = "https://agents.ieti.site/v1/chat/completions";
    private static final String MODEL_KEY = loadApiKey();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /* 
    public static void main(String[] args) {
        ChatService service = new ChatService();
        service.streamMessage("An interesting fact in just one sentence.", fragment -> System.out.print(fragment)).join();
    }
    */

    public CompletableFuture<Void> streamMessage(String message, Consumer<String> onLine) {
        try {

            String jsonBody = buildRequestJson(message, true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODEL_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + MODEL_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            CompletableFuture<HttpResponse<InputStream>> response =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());

            return response.thenApply(httpResponse -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpResponse.body(), StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("\"delta\"")) {
                            JSONObject parsedLine = parseEventPayload(line);
                            if (parsedLine.has("content")) {
                                onLine.accept(parsedLine.getString("content"));
                            }
                        }
                    }
                    return null;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            System.err.println("Error al enviar el mensaje: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<String> sendMessage(String message) {
        try {

            String jsonBody = buildRequestJson(message, false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODEL_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + MODEL_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                             .thenApply(response -> extractText(response))
                             .exceptionally(ex -> "No se pudo obtener respuesta. (" + ex.getMessage() + ")");

        } catch (Exception e) {
            System.err.println("Error al enviar el mensaje: " + e.getMessage());
            return null;
        }
    }

    private JSONObject parseEventPayload(String line) {
        JSONObject delta = new JSONObject(line.substring("data: ".length()))
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("delta");
        return delta;
    }

    private String extractText(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.err.println("Error HTTP: " + response.statusCode());
            System.err.println("Detalle: " + response.body());
            return null;
        }

        JSONObject json = new JSONObject(response.body());

        String textResponse = json.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

        return textResponse;
    }

    private String buildRequestJson(String message, boolean stream) {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", message));

        JSONObject body = new JSONObject()
                .put("model", MODEL_NAME)
                .put("messages", messages)
                .put("stream", stream);

        return body.toString();
    }

    private static String loadApiKey() {
        Properties properties = new Properties();

        try (InputStream input = ChatService.class.getResourceAsStream("/config.properties")) {

            if (input == null) {
                throw new IllegalStateException("No se encuentra config.properties");
            }

            properties.load(input);

            String key = properties.getProperty("api.key");

            if (key == null || key.isBlank()) {
                throw new IllegalStateException("La API key no está configurada");
            }

            return key.trim();

        } catch (IOException e) {
            throw new IllegalStateException("Error al leer config.properties", e);
        }
    }

}
