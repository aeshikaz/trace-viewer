package com.traceviewer.collector;

public class TraceSpan {
    public String service;
    public long startTimestamp;
    public long durationMs;

    public TraceSpan(String service, long startTimestamp, long durationMs) {
        this.service = service;
        this.startTimestamp = startTimestamp;
        this.durationMs = durationMs;
    }
}
