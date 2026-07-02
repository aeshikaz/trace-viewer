package com.traceviewer.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Random;

import com.sun.net.httpserver.HttpServer;
import com.traceviewer.common.TraceLogger;

public class PaymentService {

    private static final Random random = new Random();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8084), 0);

        server.createContext("/payment", exchange -> {
            String traceId = getQueryParam(exchange.getRequestURI(), "traceId");

            TraceLogger.logStarted("payment", traceId);
            long start = System.currentTimeMillis();

            try {
                Thread.sleep(2000 + random.nextInt(2000)); // 2-4 real seconds
            } catch (InterruptedException ignored) {}

            long duration = System.currentTimeMillis() - start;
            TraceLogger.logFinished("payment", traceId, duration);

            String response = "payment processed";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        server.start();
        System.out.println("payment service running on http://localhost:8084");
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