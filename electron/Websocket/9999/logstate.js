(function(){
    let socket;
    let retryDelay = 1000; // start 1s, bis max 10s
    const MAX_DELAY = 10000;
    let reconnectTimer = null;

    function connect() {
        try {
            socket = new WebSocket("ws://127.0.0.1:9999");
        } catch (e) {
            console.error("Logstate: failed to create WebSocket:", e);
            scheduleReconnect();
            return;
        }

        socket.onopen = () => {
            console.log("%c[Logstate]%c Connected to Java backend", "color: lime;", "color: inherit;");
            retryDelay = 1000;
            if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; }
        };

        socket.onmessage = (event) => {
            let data;
            try {
                data = JSON.parse(event.data);
            } catch (e) {
                console.error("Logstate: invalid JSON:", e, event.data);
                return;
            }

            if (!data?.action || !data?.function) return;

            switch (data.function) {
                case "logstate": {
                    const args = data.args || [];
                    const level = args[0] || "info";
                    const message = args[2] || "";

                    let color = "#eee"; // default
                    switch (level.toLowerCase()) {
                        case "warn":
                        case "warning": color = "#facc15"; break;
                        case "error": color = "#f87171"; break;
                        case "info": color = "#60a5fa"; break;
                    }

                    window.updateLogState(color, message);
                    break;
                }


                case "clearlogstate": {
                    window.clearLogState();
                    break;
                }

                default:
                    console.warn("Logstate: Unknown function", data.function);
            }
        };

        socket.onerror = (err) => console.error("WebSocket Error (logstate):", err);
        socket.onclose = () => {
            console.warn("Disconnected from Java LogstateServer");
            scheduleReconnect();
        };
    }

    function scheduleReconnect() {
        if (reconnectTimer) return;
        reconnectTimer = setTimeout(() => {
            retryDelay = Math.min(MAX_DELAY, Math.floor(retryDelay * 1.6));
            reconnectTimer = null;
            connect();
        }, retryDelay);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', connect, { once: true });
    } else {
        connect();
    }

})();

// ---------------------------------
// Update-Funktion für eine Zeile Logstate
// ---------------------------------
window.updateLogState = function (color, text) {
    const el = document.getElementById("logstate-text");
    const box = document.getElementById("logstate");
    if (!el || !box) return;

    box.style.opacity = "1";
    el.style.color = color || "#eee";
    el.textContent = text || "";

    // Automatisch nach 10s leicht ausblenden
    clearTimeout(window._logstateTimeout);
    window._logstateTimeout = setTimeout(() => {
        box.style.opacity = "0.6";
    }, 10000);
};

window.clearLogState = function () {
    const el = document.getElementById("logstate-text");
    if (el) el.textContent = "";
};

// Optional: Reaktion auf Backend-Events
window.addEventListener("panda-logstate", e => {
    const { color, text } = e.detail || {};
    window.updateLogState(color, text);
});

window.addEventListener("panda-clearlogstate", () => window.clearLogState());
