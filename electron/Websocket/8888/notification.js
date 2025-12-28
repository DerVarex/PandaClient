// Notification WebSocket client
(function() {
    const NOTIFICATION_WS_PORT = 8888;
    let socket;
    let retryDelay = 1000;
    const MAX_DELAY = 10000;
    let reconnectTimer = null;

    function connect() {
        try {
            socket = new WebSocket(`ws://127.0.0.1:${NOTIFICATION_WS_PORT}`);
        } catch (e) {
            console.error("[Notification] Failed to create WebSocket:", e);
            scheduleReconnect();
            return;
        }

        socket.onopen = () => {
            console.log("%c[Notification]%c Connected to Java NotificationServer", "color: lime;", "color: inherit;");
            retryDelay = 1000;
            if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; }
        };

        socket.onmessage = (event) => {
            let data;
            try {
                data = JSON.parse(event.data);
            } catch (e) {
                console.error("[Notification] Error while parsing:", e, event.data);
                return;
            }

            if (data.action === "call" && data.function === "showNotification") {
                if (typeof showNotification === "function") {
                    showNotification(...data.args);
                } else {
                    console.error("[Notification] Function showNotification not defined!");
                }
            }
        };

        socket.onerror = (err) => console.error("[Notification] WebSocket Error:", err);
        socket.onclose = () => {
            console.warn("[Notification] Disconnected from Java NotificationServer");
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

    // Initialize connection when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', connect, { once: true });
    } else {
        connect();
    }
})();

