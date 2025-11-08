package com.dervarex.PandaClient.utils.exceptions;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * A controlled abort/stop signal with structured details.
 * Throw this when an operation must be terminated intentionally (user cancel,
 * precondition failed, safety stop) rather than due to an unexpected bug.
 */
public class StopException extends Exception {

    public enum Severity { INFO, WARNING, ERROR, FATAL }

    private final long timestampEpochMillis = System.currentTimeMillis();
    private final String reason;        // short human readable reason
    private final String code;          // machine readable code (e.g. "USER_CANCEL", "PRECONDITION_FAILED")
    private final Severity severity;
    private final boolean recoverable;  // true if caller may retry later
    private final Map<String, String> metadata; // extra context (operation, ids, etc.)

    private StopException(Builder b) {
        super(buildMessage(b), b.cause);
        this.reason = nvl(b.reason);
        this.code = nvl(b.code);
        this.severity = b.severity;
        this.recoverable = b.recoverable;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(b.metadata));
    }

    private static String buildMessage(Builder b) {
        StringJoiner j = new StringJoiner(" ");
        if (b.severity != null) j.add(b.severity.name());
        if (b.code != null && !b.code.isBlank()) j.add("[").add(b.code).add("]");
        if (b.reason != null) j.add(b.reason);
        return j.toString();
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    public long getTimestampEpochMillis() { return timestampEpochMillis; }
    public Instant getTimestampInstant() { return Instant.ofEpochMilli(timestampEpochMillis); }
    public String getReason() { return reason; }
    public String getCode() { return code; }
    public Severity getSeverity() { return severity; }
    public boolean isRecoverable() { return recoverable; }
    public Map<String, String> getMetadata() { return metadata; }

    public String toUserFriendlyMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vorgang gestoppt: ").append(reason).append('\n');
        if (!code.isBlank()) sb.append("Code: ").append(code).append('\n');
        sb.append("Schweregrad: ").append(severity).append(", Wiederholbar: ").append(recoverable).append('\n');
        sb.append("Zeit: ").append(getTimestampInstant()).append('\n');
        if (!metadata.isEmpty()) {
            sb.append("Details:\n");
            metadata.forEach((k,v) -> sb.append("  - ").append(k).append(": ").append(v).append('\n'));
        }
        return sb.toString();
    }

    public String toJson() {
        StringBuilder meta = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String,String> e : metadata.entrySet()) {
            if (!first) meta.append(',');
            first = false;
            meta.append(escape(e.getKey())).append(':').append(escape(e.getValue()));
        }
        meta.append('}');
        return "{" +
                "\"timestamp\":" + timestampEpochMillis + "," +
                "\"reason\":" + escape(reason) + "," +
                "\"code\":" + escape(code) + "," +
                "\"severity\":" + escape(severity == null ? "" : severity.name()) + "," +
                "\"recoverable\":" + recoverable + "," +
                "\"metadata\":" + meta +
                "}";
    }

    private static String escape(String s) {
        if (s == null) return "null";
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    // Convenience factories
    public static StopException userCancel(String reason) {
        return new Builder().severity(Severity.INFO).code("USER_CANCEL").reason(reason).recoverable(true).build();
    }
    public static StopException preconditionFailed(String reason) {
        return new Builder().severity(Severity.WARNING).code("PRECONDITION_FAILED").reason(reason).recoverable(true).build();
    }
    public static StopException fatal(String reason) {
        return new Builder().severity(Severity.FATAL).code("FATAL_STOP").reason(reason).recoverable(false).build();
    }

    public static class Builder {
        private String reason = "";
        private String code = "";
        private Severity severity = Severity.ERROR;
        private boolean recoverable = false;
        private final Map<String, String> metadata = new LinkedHashMap<>();
        private Throwable cause;

        public Builder reason(String r) { this.reason = r; return this; }
        public Builder code(String c) { this.code = c; return this; }
        public Builder severity(Severity s) { this.severity = s; return this; }
        public Builder recoverable(boolean r) { this.recoverable = r; return this; }
        public Builder meta(String k, String v) { this.metadata.put(k, v); return this; }
        public Builder metadata(Map<String,String> m) { if (m!=null) m.forEach(this::meta); return this; }
        public Builder cause(Throwable t) { this.cause = t; return this; }
        public StopException build() { return new StopException(this); }
    }
}

