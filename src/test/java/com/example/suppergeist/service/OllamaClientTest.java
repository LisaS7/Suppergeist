package com.example.suppergeist.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaClientTest {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private String requestMethod;
    private String requestContentType;
    private String requestBody;
    private IOException serverError;
    private int responseStatus;
    private String responseReason;
    private String responseBody;

    @BeforeEach
    void setUp() throws IOException {
        responseStatus = 200;
        responseReason = "OK";
        responseBody = """
                {"model":"llama3.2","response":"{\\"meals\\":[]}","done":true}
                """;
        serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        serverThread = new Thread(this::handleSingleRequest);
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws IOException, InterruptedException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        if (serverThread != null) {
            serverThread.join(1000);
        }
    }

    @Test
    void generate_postsPromptToOllamaEndpointAndReturnsGeneratedResponseField() throws IOException {
        URI uri = URI.create("http://127.0.0.1:" + serverSocket.getLocalPort() + "/api/generate");
        OllamaClient client = new OllamaClient("llama3.2", uri, HttpClient.newHttpClient());

        String response = client.generate("say \"hello\"\nthen return JSON");

        assertNull(serverError);
        assertEquals("{\"meals\":[]}", response);
        assertEquals("POST", requestMethod);
        assertEquals("application/json", requestContentType);

        JsonObject body = JsonParser.parseString(requestBody).getAsJsonObject();
        assertEquals("llama3.2", body.get("model").getAsString());
        assertEquals("say \"hello\"\nthen return JSON", body.get("prompt").getAsString());
        assertEquals(false, body.get("stream").getAsBoolean());
    }

    @Test
    void generate_throwsTypedExceptionForHttpErrorWithoutParsingBody() {
        responseStatus = 500;
        responseReason = "Internal Server Error";
        responseBody = "ollama model failed";
        URI uri = URI.create("http://127.0.0.1:" + serverSocket.getLocalPort() + "/api/generate");
        OllamaClient client = new OllamaClient("llama3.2", uri, HttpClient.newHttpClient());

        OllamaClient.OllamaHttpException exception = assertThrows(
                OllamaClient.OllamaHttpException.class,
                () -> client.generate("return JSON")
        );

        assertNull(serverError);
        assertEquals(500, exception.statusCode());
        assertEquals("ollama model failed", exception.bodyPreview());
        assertEquals("Ollama request failed with HTTP status 500 and response body: ollama model failed", exception.getMessage());
    }

    @Test
    void generate_truncatesHttpErrorBodyPreview() {
        responseStatus = 404;
        responseReason = "Not Found";
        responseBody = "x".repeat(600);
        URI uri = URI.create("http://127.0.0.1:" + serverSocket.getLocalPort() + "/api/generate");
        OllamaClient client = new OllamaClient("llama3.2", uri, HttpClient.newHttpClient());

        OllamaClient.OllamaHttpException exception = assertThrows(
                OllamaClient.OllamaHttpException.class,
                () -> client.generate("return JSON")
        );

        assertEquals(404, exception.statusCode());
        assertEquals(503, exception.bodyPreview().length());
        assertTrue(exception.bodyPreview().endsWith("..."));
        assertFalse(exception.bodyPreview().contains("x".repeat(501)));
    }

    private void handleSingleRequest() {
        try (Socket socket = serverSocket.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream output = socket.getOutputStream()) {
            String requestLine = reader.readLine();
            requestMethod = requestLine.split(" ")[0];

            int contentLength = 0;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.regionMatches(true, 0, "Content-Type:", 0, "Content-Type:".length())) {
                    requestContentType = line.substring("Content-Type:".length()).trim();
                } else if (line.regionMatches(true, 0, "Content-Length:", 0, "Content-Length:".length())) {
                    contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                }
            }

            char[] body = new char[contentLength];
            int offset = 0;
            while (offset < contentLength) {
                int read = reader.read(body, offset, contentLength - offset);
                if (read == -1) {
                    break;
                }
                offset += read;
            }
            requestBody = new String(body, 0, offset);

            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            output.write(("HTTP/1.1 " + responseStatus + " " + responseReason
                    + "\r\nContent-Type: application/json\r\nContent-Length: "
                    + response.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(response);
            output.flush();
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                serverError = e;
            }
        }
    }
}
