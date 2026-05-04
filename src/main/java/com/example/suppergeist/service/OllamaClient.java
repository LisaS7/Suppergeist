package com.example.suppergeist.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OllamaClient {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final Gson GSON = new Gson();
    private static final Logger log = Logger.getLogger(OllamaClient.class.getName());

    private final HttpClient client;
    private final URI uri;
    private final String model;

    public OllamaClient(String model) {
        this(model, URI.create(OLLAMA_URL), HttpClient.newHttpClient());
    }

    OllamaClient(String model, URI uri, HttpClient client) {
        this.model = model;
        this.uri = uri;
        this.client = client;
    }

    public String generate(String prompt) throws IOException {
        String body = GSON.toJson(new GenerateRequest(model, prompt, false));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            log.info(() -> "Sending meal-plan generation request to Ollama model " + model + " at " + uri);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info(() -> "Ollama generation request completed with status " + response.statusCode());
            if (response.statusCode() >= 400) {
                log.warning(() -> "Ollama returned unsuccessful status " + response.statusCode()
                        + " with response body length " + response.body().length());
            }
            return parseGenerateResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.log(Level.WARNING, "Ollama generation request interrupted", e);
            throw new IOException("Request interrupted", e);
        } catch (IOException e) {
            log.log(Level.WARNING, "Ollama generation request failed", e);
            throw e;
        }
    }

    private record GenerateRequest(String model, String prompt, boolean stream) {}

    private String parseGenerateResponse(String body) throws IOException {
        try {
            GenerateResponse response = GSON.fromJson(body, GenerateResponse.class);
            if (response == null || response.response == null) {
                throw new IOException("Ollama response did not include generated content");
            }
            return response.response;
        } catch (JsonSyntaxException e) {
            throw new IOException("Invalid Ollama response JSON", e);
        }
    }

    private record GenerateResponse(String response) {}
}
