package com.traceviewer.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Random;

import com.sun.net.httpserver.HttpServer;
import com.traceviewer.common.TraceLogger;

public class CheckUserService {

    private static final Random random = new Random();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        server.createContext("/check-user", exchange -> {
            String traceId = getQueryParam(exchange.getRequestURI(), "traceId");

            TraceLogger.logStarted("check-user", traceId);
            long start = System.currentTimeMillis();

            // Simulate doing real work (e.g. a database lookup) with a small random pause
            try {
                Thread.sleep(100 + random.nextInt(200)); // sleeps somewhere between 100-300ms
            } catch (InterruptedException ignored) {}

            long duration = System.currentTimeMillis() - start; // ACTUAL measured time
            TraceLogger.logFinished("check-user", traceId, duration);

            String response = "user checked";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        server.start();
        System.out.println("check-user service running on http://localhost:8081");
    }

    private static String getQueryParam(URI uri, String key) {
        String query = uri.getQuery();
        if (query == null) return "unknown";
        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(key)) {
                return keyValue[1];
            }
        }
        return "unknown";
    }
}