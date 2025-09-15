const socket = new WebSocket("ws://127.0.0.1:8888");

socket.onopen = () => {
    console.log("Verbunden mit Java NotificationServer");
};

socket.onmessage = (event) => {
    let data;
    try {
        data = JSON.parse(event.data);
    } catch (e) {
        console.error("Fehler beim Parsen:", e, event.data);
        return;
    }

    if (data.action === "call" && data.function === "showNotification") {
        if (typeof showNotification === "function") {
            showNotification(...data.args);
        } else {
            console.error("Funktion showNotification nicht definiert!");
        }
    }
};

socket.onerror = (err) => console.error("WebSocket Fehler:", err);

