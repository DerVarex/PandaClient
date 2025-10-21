(function(){
  let socket;
  let retryDelay = 1000; // start with 1s, up to 10s
  const MAX_DELAY = 10000;

  function connect() {
    try {
      socket = new WebSocket("ws://127.0.0.1:9999");
    } catch (e) {
      console.error("Logstate: failed to create WebSocket:", e);
      scheduleReconnect();
      return;
    }

    socket.onopen = () => {
      console.log("Connected with Java LogstateServer");
      retryDelay = 1000; // reset backoff
    };

    socket.onmessage = (event) => {
      let data;
      try {
        data = JSON.parse(event.data);
      } catch (e) {
        console.error("Logstate: error parsing message:", e, event.data);
        return;
      }

      if (data && data.action === "call" && data.function === "logstate") {
        try {
          const args = Array.isArray(data.args) ? data.args : [];
          // expected: [level, where, message, timestamp]
          if (typeof window.appendLogLine === 'function') {
            window.appendLogLine(args[0], args[1], args[2], args[3]);
          } else {
            console.warn("appendLogLine not available yet; received:", args);
          }
        } catch (err) {
          console.error("Logstate: handler failed:", err);
        }
      }
    };

    socket.onerror = (err) => console.error("WebSocket Error (logstate):", err);

    socket.onclose = () => {
      console.warn("Disconnected from Java LogstateServer");
      scheduleReconnect();
    };
  }

  function scheduleReconnect() {
    setTimeout(() => {
      retryDelay = Math.min(MAX_DELAY, Math.floor(retryDelay * 1.6));
      connect();
    }, retryDelay);
  }

  connect();
})();
