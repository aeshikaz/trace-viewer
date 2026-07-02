package com.traceviewer.dashboard;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.sun.net.httpserver.HttpServer;
import com.traceviewer.collector.Collector;
import com.traceviewer.collector.TraceSpan;

public class DashboardServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9090), 0);

        // API endpoint: GET /api/trace/abc123 -> returns the spans as JSON
        server.createContext("/api/trace/", exchange -> {
            String path = exchange.getRequestURI().getPath(); // e.g. /api/trace/abc123
            String traceId = path.substring("/api/trace/".length());

            List<TraceSpan> spans = Collector.getSpansForTrace(traceId);
            String json = toJson(spans);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });

        // Serves the actual webpage for everything else
        server.createContext("/", exchange -> {
            Path htmlPath = Path.of("src/main/resources/static/index.html");
            byte[] bytes = Files.readAllBytes(htmlPath);
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });

        server.start();
        System.out.println("Dashboard running at http://localhost:9090");
    }

    // Converts our list of TraceSpan objects into a JSON array by hand
    private static String toJson(List<TraceSpan> spans) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < spans.size(); i++) {
            TraceSpan s = spans.get(i);
            sb.append("{\"service\":\"").append(s.service)
              .append("\",\"startTimestamp\":").append(s.startTimestamp)
              .append(",\"durationMs\":").append(s.durationMs).append("}");
            if (i < spans.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}