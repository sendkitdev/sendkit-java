package dev.sendkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SendKit {

    private static final String DEFAULT_BASE_URL = "https://api.sendkit.dev";

    final HttpClient httpClient;
    final String baseUrl;
    final String apiKey;
    final Gson gson;

    private final Emails emails;

    public SendKit(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    public SendKit(String apiKey, String baseUrl) {
        this(apiKey, baseUrl, HttpClient.newHttpClient());
    }

    public SendKit(String apiKey, String baseUrl, HttpClient httpClient) {
        String key = (apiKey == null || apiKey.isEmpty())
                ? System.getenv().getOrDefault("SENDKIT_API_KEY", "")
                : apiKey;

        if (key.isEmpty()) {
            throw new SendKitException("Missing API key", "missing_api_key", null);
        }

        this.apiKey = key;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.httpClient = httpClient;
        this.gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        this.emails = new Emails(this);
    }

    public Emails emails() {
        return emails;
    }

    <T> T post(String path, Object body, Class<T> responseType) throws SendKitException {
        try {
            String json = gson.toJson(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return gson.fromJson(response.body(), responseType);
            }

            ErrorResponse error;
            try {
                error = gson.fromJson(response.body(), ErrorResponse.class);
            } catch (Exception e) {
                error = new ErrorResponse("application_error", "Unknown error", null);
            }

            throw new SendKitException(
                    error.message != null ? error.message : "Unknown error",
                    error.name != null ? error.name : "application_error",
                    error.statusCode
            );
        } catch (SendKitException e) {
            throw e;
        } catch (Exception e) {
            throw new SendKitException("HTTP request failed: " + e.getMessage(), "http_error", null);
        }
    }

    static class ErrorResponse {
        String name;
        String message;
        Integer statusCode;

        ErrorResponse(String name, String message, Integer statusCode) {
            this.name = name;
            this.message = message;
            this.statusCode = statusCode;
        }
    }
}
