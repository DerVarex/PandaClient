package com.dervarex.PandaClient.utils.exceptions;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Erweiterte Exception für fehlende Internet‑/Netzwerkverbindung.
 * Liefert strukturierte Diagnosedaten (DNS, TCP, HTTP Tests), Vorschläge für den Nutzer
 * und kann als JSON serialisiert werden.
 */
public class NoConnectionException extends Exception {

    private final long timestampEpochMillis = System.currentTimeMillis();
    private final String actionContext;
    private final boolean dnsResolved;
    private final boolean tcpAnyReachable;
    private final boolean httpAnyReachable;
    private final Map<String, ProbeResult> probeResults;
    private final List<String> suggestions;
    private final String activeOs;
    private final String networkInterfaceSummary;
    private final Throwable rootCause;
    private transient String cachedMessage;

    private NoConnectionException(Builder b) {
        super(makeBaseMessage(b), b.rootCause);
        this.actionContext = b.actionContext;
        this.dnsResolved = b.dnsResolved;
        this.tcpAnyReachable = b.tcpAnyReachable;
        this.httpAnyReachable = b.httpAnyReachable;
        this.probeResults = Collections.unmodifiableMap(new LinkedHashMap<>(b.probeResults));
        this.suggestions = Collections.unmodifiableList(new ArrayList<>(b.suggestions));
        this.activeOs = b.activeOs;
        this.networkInterfaceSummary = b.networkInterfaceSummary;
        this.rootCause = b.rootCause;
    }

    private static String makeBaseMessage(Builder b) {
        return "No internet connection (" + b.actionContext + ") - DNS=" + b.dnsResolved + ", TCP=" + b.tcpAnyReachable + ", HTTP=" + b.httpAnyReachable;
    }

    public String getActionContext() { return actionContext; }
    public boolean wasDnsResolved() { return dnsResolved; }
    public boolean wasAnyTcpReachable() { return tcpAnyReachable; }
    public boolean wasAnyHttpReachable() { return httpAnyReachable; }
    public long getTimestampEpochMillis() { return timestampEpochMillis; }
    public Instant getTimestampInstant() { return Instant.ofEpochMilli(timestampEpochMillis); }
    public Map<String, ProbeResult> getProbeResults() { return probeResults; }
    public List<String> getSuggestions() { return suggestions; }
    public String getActiveOs() { return activeOs; }
    public String getNetworkInterfaceSummary() { return networkInterfaceSummary; }
    public Throwable getRootCause() { return rootCause; }

    public String toUserFriendlyMessage() {
        if (cachedMessage != null) return cachedMessage;
        StringBuilder sb = new StringBuilder();
        sb.append("Internet / Netzwerk Problem für Aktion: ").append(actionContext).append('\n');
        sb.append("OS: ").append(activeOs).append(" | Zeit: ").append(getTimestampInstant()).append('\n');
        sb.append("DNS ok: ").append(dnsResolved).append(", TCP erreichbar: ").append(tcpAnyReachable)
          .append(", HTTP erreichbar: ").append(httpAnyReachable).append('\n');
        sb.append("Probes:\n");
        probeResults.forEach((k,v) -> sb.append("  - ").append(k).append(": ").append(v).append('\n'));
        if (!suggestions.isEmpty()) {
            sb.append("Vorschläge:\n");
            suggestions.forEach(s -> sb.append("  * ").append(s).append('\n'));
        }
        cachedMessage = sb.toString();
        return cachedMessage;
    }

    public String toJson() {
        String probesJson = probeResults.entrySet().stream()
                .map(e -> escape(e.getKey()) + ":" + e.getValue().toJson())
                .collect(Collectors.joining(","));
        String suggJson = suggestions.stream().map(NoConnectionException::escape).collect(Collectors.joining(","));
        return "{" +
                "\"action\":" + escape(actionContext) + "," +
                "\"timestamp\":" + timestampEpochMillis + "," +
                "\"os\":" + escape(activeOs) + "," +
                "\"dnsResolved\":" + dnsResolved + "," +
                "\"tcpAnyReachable\":" + tcpAnyReachable + "," +
                "\"httpAnyReachable\":" + httpAnyReachable + "," +
                "\"probes\":{" + probesJson + "}," +
                "\"suggestions\":[" + suggJson + "]," +
                "\"networkIface\":" + escape(networkInterfaceSummary) +
                "}";
    }

    private static String escape(String s) {
        if (s == null) return "null";
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    @Override
    public String toString() { return toUserFriendlyMessage(); }

    public static final class ProbeResult {
        public enum Type { DNS, TCP, HTTP }
        private final Type type;
        private final boolean success;
        private final long latencyMillis;
        private final String target;
        private final String detail;
        public ProbeResult(Type type, boolean success, long latencyMillis, String target, String detail) {
            this.type = type; this.success = success; this.latencyMillis = latencyMillis; this.target = target; this.detail = detail;
        }
        public String toJson() {
            return "{" +
                    "\"type\":" + escape(type.name()) + "," +
                    "\"success\":" + success + "," +
                    "\"latency\":" + latencyMillis + "," +
                    "\"target\":" + escape(target) + "," +
                    "\"detail\":" + escape(detail) +
                    "}";
        }
        @Override public String toString() { return type+"("+target+") success="+success+" latency="+latencyMillis+"ms detail="+detail; }
    }

    public static class Builder {
        private String actionContext = "unknown";
        private boolean dnsResolved;
        private boolean tcpAnyReachable;
        private boolean httpAnyReachable;
        private final Map<String, ProbeResult> probeResults = new LinkedHashMap<>();
        private final List<String> suggestions = new ArrayList<>();
        private String activeOs = System.getProperty("os.name");
        private String networkInterfaceSummary = "";
        private Throwable rootCause;
        public Builder action(String ctx) { this.actionContext = ctx; return this; }
        public Builder dnsResolved(boolean v) { this.dnsResolved = v; return this; }
        public Builder tcpAny(boolean v) { this.tcpAnyReachable = v; return this; }
        public Builder httpAny(boolean v) { this.httpAnyReachable = v; return this; }
        public Builder addProbe(String key, ProbeResult pr) { this.probeResults.put(key, pr); return this; }
        public Builder addSuggestion(String s) { if (s!=null && !s.isBlank()) suggestions.add(s); return this; }
        public Builder suggestions(Collection<String> s) { s.forEach(this::addSuggestion); return this; }
        public Builder os(String os) { this.activeOs = os; return this; }
        public Builder netIfaces(String summary) { this.networkInterfaceSummary = summary; return this; }
        public Builder cause(Throwable t) { this.rootCause = t; return this; }
        public NoConnectionException build() { if (suggestions.isEmpty()) autoPopulateSuggestions(); return new NoConnectionException(this); }
        private void autoPopulateSuggestions() {
            if (!dnsResolved) suggestions.add("Prüfe DNS / Router neu starten");
            if (!tcpAnyReachable) suggestions.add("Firewall / VPN / Proxy prüfen");
            if (!httpAnyReachable && tcpAnyReachable) suggestions.add("Möglicher Captive Portal (Hotel WLAN?) Browser öffnen");
            suggestions.add("Internetverbindung (LAN/WLAN) neu verbinden");
            suggestions.add("Falls weiterhin Probleme: Offline Modus / später erneut versuchen");
        }
    }

    public static void throwIfOffline(String action, java.util.function.Supplier<Boolean> quickCheck) throws NoConnectionException {
        boolean ok;
        try { ok = quickCheck.get(); } catch (Throwable t) { ok = false; }
        if (!ok) {
            throw new NoConnectionException.Builder()
                    .action(action)
                    .dnsResolved(false)
                    .tcpAny(false)
                    .httpAny(false)
                    .addProbe("quick", new ProbeResult(ProbeResult.Type.TCP, false, -1, "quickCheck", "returned false"))
                    .build();
        }
    }
}
