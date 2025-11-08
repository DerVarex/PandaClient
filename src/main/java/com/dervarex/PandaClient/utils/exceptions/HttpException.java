package com.dervarex.PandaClient.utils.exceptions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Rich HTTP exception carrying status, method, URL and a compact response snapshot.
 * Use this when an HTTP call fails or returns a non-success code.
 */
public class HttpException extends Exception {

    public enum Method { GET, POST, PUT, DELETE, HEAD, PATCH, OPTIONS, TRACE, CONNECT }

    private final long timestampEpochMillis = System.currentTimeMillis();
    private final int statusCode;
    private final String statusMessage;
    private final Method method;
    private final String url;
    private final Map<String, String> responseHeaders;
    private final byte[] responseBody; // raw for diagnostic snippet
    private final String requestId;
    private final boolean transientFailure; // e.g., 429/503

    public HttpException(Builder b) {
        super(buildMessage(b), b.cause);
        this.statusCode = b.statusCode;
        this.statusMessage = nvl(b.statusMessage);
        this.method = b.method;
        this.url = nvl(b.url);
        this.responseHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(b.responseHeaders));
        this.responseBody = b.responseBody == null ? new byte[0] : b.responseBody.clone();
        this.requestId = nvl(b.requestId);
        this.transientFailure = b.transientFailure;
    }

    private static String buildMessage(Builder b) {
        StringJoiner j = new StringJoiner(" ");
        j.add("HTTP").add(String.valueOf(b.statusCode));
        if (b.statusMessage != null && !b.statusMessage.isBlank()) j.add("(").add(b.statusMessage).add(")");
        if (b.method != null) j.add(b.method.name());
        if (b.url != null) j.add(b.url);
        return j.toString();
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    public int getStatusCode() { return statusCode; }
    public String getStatusMessage() { return statusMessage; }
    public Method getMethod() { return method; }
    public String getUrl() { return url; }
    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public byte[] getResponseBody() { return responseBody.clone(); }
    public long getTimestampEpochMillis() { return timestampEpochMillis; }
    public Instant getTimestampInstant() { return Instant.ofEpochMilli(timestampEpochMillis); }
    public String getRequestId() { return requestId; }
    public boolean isTransientFailure() { return transientFailure; }

    public String responseSnippet(int maxChars) {
        if (responseBody.length == 0) return "";
        String s = new String(responseBody, StandardCharsets.UTF_8);
        if (s.length() <= maxChars) return s;
        return s.substring(0, Math.max(0, maxChars)) + "…";
    }

    public String toUserFriendlyMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP Fehler ").append(statusCode);
        if (!statusMessage.isBlank()) sb.append(" (").append(statusMessage).append(")");
        sb.append(" bei ").append(method).append(" ").append(url).append('\n');
        if (!requestId.isBlank()) sb.append("Request-Id: ").append(requestId).append('\n');
        String retry = transientFailure ? " (vorübergehend, später erneut versuchen)" : "";
        sb.append("Zeit: ").append(getTimestampInstant()).append(retry).append('\n');
        String ct = responseHeaders.getOrDefault("content-type", responseHeaders.getOrDefault("Content-Type", ""));
        if (!ct.isBlank()) sb.append("Content-Type: ").append(ct).append('\n');
        String body = responseSnippet(600);
        if (!body.isBlank()) sb.append("Antwort: ").append(body);
        return sb.toString();
    }

    public String toJson() {
        StringBuilder h = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String,String> e : responseHeaders.entrySet()) {
            if (!first) h.append(',');
            first = false;
            h.append(escape(e.getKey())).append(':').append(escape(e.getValue()));
        }
        h.append('}');
        return "{" +
                "\"status\":" + statusCode + "," +
                "\"message\":" + escape(statusMessage) + "," +
                "\"method\":" + escape(method == null ? "" : method.name()) + "," +
                "\"url\":" + escape(url) + "," +
                "\"timestamp\":" + timestampEpochMillis + "," +
                "\"transient\":" + transientFailure + "," +
                "\"headers\":" + h + "," +
                "\"requestId\":" + escape(requestId) + "," +
                "\"bodySnippet\":" + escape(responseSnippet(600)) +
                "}";
    }

    private static String escape(String s) {
        if (s == null) return "null";
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    public static class Builder {
        private int statusCode = -1;
        private String statusMessage = "";
        private Method method = Method.GET;
        private String url = "";
        private Map<String, String> responseHeaders = new LinkedHashMap<>();
        private byte[] responseBody = new byte[0];
        private String requestId = "";
        private boolean transientFailure = false;
        private Throwable cause;

        public Builder status(int code) { this.statusCode = code; return this; }
        public Builder statusMessage(String msg) { this.statusMessage = msg; return this; }
        public Builder method(Method m) { this.method = m; return this; }
        public Builder url(String u) { this.url = u; return this; }
        public Builder header(String k, String v) { this.responseHeaders.put(Objects.toString(k, ""), Objects.toString(v, "")); return this; }
        public Builder headers(Map<String,String> m) { if (m!=null) m.forEach(this::header); return this; }
        public Builder body(byte[] b) { this.responseBody = b == null ? new byte[0] : b.clone(); return this; }
        public Builder body(String s) { this.responseBody = s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8); return this; }
        public Builder requestId(String id) { this.requestId = id == null ? "" : id; return this; }
        public Builder transientFailure(boolean tf) { this.transientFailure = tf; return this; }
        public Builder cause(Throwable t) { this.cause = t; return this; }
        public HttpException build() { return new HttpException(this); }
    }
}

