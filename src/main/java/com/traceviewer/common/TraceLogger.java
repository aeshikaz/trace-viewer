package com.traceviewer.common;

import java.io.FileWriter;
import java.io.IOException;

public class TraceLogger {

    public static void logStarted(String serviceName, String traceId) {
        long timestamp = System.currentTimeMillis();
        String line = "{\"trace_id\":\"" + traceId + "\",\"service\":\"" + serviceName
                + "\",\"event\":\"started\",\"timestamp\":" + timestamp + "}";
        write(serviceName, line);
    }

    public static void logFinished(String serviceName, String traceId, long durationMs) {
        long timestamp = System.currentTimeMillis();
        String line = "{\"trace_id\":\"" + traceId + "\",\"service\":\"" + serviceName
                + "\",\"event\":\"finished\",\"timestamp\":" + timestamp
                + ",\"duration_ms\":" + durationMs + "}";
        write(serviceName, line);
    }

    private static void write(String serviceName, String line) {
        System.out.println("[" + serviceName + "] " + line);

        String path = "logs/" + serviceName + ".log";
        try (FileWriter fw = new FileWriter(path, true)) { // true = append, don't overwrite
            fw.write(line + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Could not write log file for " + serviceName + ": " + e.getMessage());
        }
    }
}