package com.traceviewer.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import com.sun.net.httpserver.HttpServer;
import com.traceviewer.common.TraceLogger;

public class FrontDoorService {

    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/place-order", exchange -> {
            // Generate a new trace ID for this one request
            String traceId = UUID.randomUUID().toString().substring(0, 8);

            TraceLogger.logStarted("front-door", traceId);
long start = System.currentTimeMillis();

try {
    callService("http://localhost:8081/check-user?traceId=" + traceId);
    callService("http://localhost:8082/check-price?traceId=" + traceId);
    callService("http://localhost:8084/payment?traceId=" + traceId);
    callService("http://localhost:8083/send-confirmation?traceId=" + traceId);
} catch (Exception e) {
    System.out.println("Something went wrong calling a service: " + e.getMessage());
}

long duration = System.currentTimeMillis() - start;
TraceLogger.logFinished("front-door", traceId, duration);

            String response = "{\"message\":\"order placed\",\"traceId\":\"" + traceId + "\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

            System.out.println(">>> Order placed! Trace ID = " + traceId);
        });

        server.start();
        System.out.println("front-door service running on http://localhost:8080");
        System.out.println("Try: http://localhost:8080/place-order");
    }

    // Makes an HTTP GET request to another service, and waits for it to respond
    private static void callService(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("  called " + url + " -> got response: " + response.body());
    }
}