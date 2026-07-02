package com.traceviewer.collector;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Collector {

    // Matches pieces like "trace_id":"abc123" or "timestamp":1234567890
    // inside our JSON log lines, so we can pull out each field by name.
    private static final Pattern FIELD = Pattern.compile("\"(\\w+)\":\"?([^\",}]+)\"?");

    public static List<TraceSpan> getSpansForTrace(String traceId) throws IOException {
        // Tracks the "started" timestamp for a service until we find its "finished" line
        Map<String, Long> pendingStarts = new HashMap<>();
        List<TraceSpan> spans = new ArrayList<>();

        Path logsDir = Paths.get("logs");
        if (!Files.exists(logsDir)) {
            return spans; // no logs yet, return an empty list
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir, "*.log")) {
            for (Path file : stream) {
                for (String line : Files.readAllLines(file)) {
                    Map<String, String> fields = parseLine(line);
                    if (fields.isEmpty()) continue;
                    if (!traceId.equals(fields.get("trace_id"))) continue; // skip other traces

                    String service = fields.get("service");
                    String event = fields.get("event");
                    long timestamp = Long.parseLong(fields.get("timestamp"));

                    if ("started".equals(event)) {
                        pendingStarts.put(service, timestamp);
                    } else if ("finished".equals(event)) {
                        Long startTime = pendingStarts.remove(service);
                        long duration = fields.containsKey("duration_ms")
                                ? Long.parseLong(fields.get("duration_ms"))
                                : 0;
                        long effectiveStart = (startTime != null) ? startTime : timestamp - duration;
                        spans.add(new TraceSpan(service, effectiveStart, duration));
                    }
                }
            }
        }

        // Sort so the earliest-started service comes first -- needed for a left-to-right timeline
        spans.sort((a, b) -> Long.compare(a.startTimestamp, b.startTimestamp));
        return spans;
    }

    // Pulls out every "key":"value" pair from one JSON log line, by hand.
    // (We're not using a JSON library here to keep the project dependency-free --
    // our log format is simple and fixed, so this is safe to do.)
    private static Map<String, String> parseLine(String line) {
        Map<String, String> fields = new HashMap<>();
        Matcher matcher = FIELD.matcher(line);
        while (matcher.find()) {
            fields.put(matcher.group(1), matcher.group(2));
        }
        return fields;
    }
}