package com.traceviewer.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Random;

import com.sun.net.httpserver.HttpServer;
import com.traceviewer.common.TraceLogger;

public class CheckPriceService {

    private static final Random random = new Random();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);

        server.createContext("/check-price", exchange -> {
            String traceId = getQueryParam(exchange.getRequestURI(), "traceId");

            TraceLogger.logStarted("check-price", traceId);
            long start = System.currentTimeMillis();

            try {
                Thread.sleep(100 + random.nextInt(200));
            } catch (InterruptedException ignored) {}

            long duration = System.currentTimeMillis() - start;
            TraceLogger.logFinished("check-price", traceId, duration);

            String response = "price calculated";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        server.start();
        System.out.println("check-price service running on http://localhost:8082");
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