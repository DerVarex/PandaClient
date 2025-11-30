const { app, BrowserWindow, ipcMain } = require("electron");
const path = require("path");
const fs = require("fs");
const http = require("http");

function createWindow() {
    let iconPath;
    if (process.platform === "win32") {
        iconPath = path.join(__dirname, "images", "logo.ico");
    } else {
        iconPath = path.join(__dirname, "images", "logo.png");
    }
    const windowOptions = {
        width: 800,
        height: 600,
        frame: false,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: true,
            preload: path.join(__dirname, 'preload.js')
        }
    };

    // Only set icon if it exists (prevents errors on missing file)
    if (fs.existsSync(iconPath)) {
        windowOptions.icon = iconPath;
    }

    const win = new BrowserWindow(windowOptions);

    win.removeMenu();

    app.setPath('userData', path.join(app.getPath('userData'), 'PandaClientData'));

    win.loadFile("index.html").catch(err => {
        console.error("Failed to load index.html:", err);
    });

    // Open DevTools for debugging
//    win.webContents.openDevTools();
}

// Try to shut down the Java backend gracefully before quitting Electron
function shutdownBackend(timeoutMs = 1500) {
    return new Promise((resolve) => {
        let settled = false;
        const req = http.get("http://127.0.0.1:8800/shutdown", (res) => {
            res.on("data", () => {});
            res.on("end", () => { if (!settled) { settled = true; resolve(); } });
        });
        req.on("error", () => { if (!settled) { settled = true; resolve(); } });
        req.setTimeout(timeoutMs, () => { try { req.destroy(); } finally { if (!settled) { settled = true; resolve(); } } });
    });
}

// Helper to call Java backend endpoint to open Swing server UI
function callJavaOpenServerDashboard() {
    return new Promise((resolve) => {
        const req = http.get("http://127.0.0.1:8800/openServerDashboard", (res) => {
            let body = "";
            res.setEncoding("utf8");
            res.on("data", chunk => body += chunk);
            res.on("end", () => {
                try {
                    const json = JSON.parse(body);
                    resolve(json);
                } catch (e) {
                    resolve({ success: false, error: "Invalid JSON from backend" });
                }
            });
        });
        req.on("error", (err) => {
            console.error("Failed to call /openServerDashboard:", err.message);
            resolve({ success: false, error: err.message });
        });
        req.setTimeout(1500, () => { try { req.destroy(); } finally { resolve({ success: false, error: "timeout" }); } });
    });
}

// IPC from renderer to quit app explicitly (works on macOS too)
ipcMain.on('app-quit', () => {
    shutdownBackend(1500).finally(() => app.quit());
});

// IPC to open Swing server overview window
ipcMain.handle('open-overview-window', async () => {
    const result = await callJavaOpenServerDashboard();
    return result && typeof result === 'object' ? result : { success: false };
});

app.whenReady().then(() => {
    createWindow();

    app.on("activate", () => {
        if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
});

app.on("window-all-closed", () => {
    if (process.platform !== "darwin") {
        shutdownBackend(1500).finally(() => app.quit());
    }
});
