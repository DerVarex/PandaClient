const socket = new WebSocket("ws://127.0.0.1:8888");

socket.onopen = () => {
    console.log("Connected with Java NotificationServer");
};

socket.onmessage = (event) => {
    let data;
    try {
        data = JSON.parse(event.data);
    } catch (e) {
        console.error("Error while Parsing:", e, event.data);
        return;
    }

    if (data.action === "call" && data.function === "showNotification") {
        if (typeof showNotification === "function") {
            showNotification(...data.args);
        } else {
            console.error("Function showNotification not defined!");
        }
    }
};

socket.onerror = (err) => console.error("WebSocket Error:", err);

