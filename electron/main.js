const { app, BrowserWindow, ipcMain } = require("electron");
const path = require("path");
const fs = require("fs");
const http = require("http");

let mainWindow; //main window reference
let backendPort = 8800; // Default port

// Read backend port from file
function readBackendPort() {
    try {
        const portFilePath = path.join(__dirname, '.backend-port');
        if (fs.existsSync(portFilePath)) {
            const portStr = fs.readFileSync(portFilePath, 'utf8').trim();
            const port = parseInt(portStr, 10);
            if (!isNaN(port) && port > 0) {
                backendPort = port;
                console.log('[main] Read backend port from file:', backendPort);
                return port;
            }
        }
    } catch (e) {
        console.warn('[main] Could not read port file:', e.message);
    }
    return backendPort;
}

// Try to discover backend port by probing
async function discoverBackendPort() {
    const maxAttempts = 10;
    for (let offset = 0; offset < maxAttempts; offset++) {
        const port = 8800 + offset;
        try {
            const isResponding = await new Promise((resolve) => {
                const req = http.get(`http://127.0.0.1:${port}/isLoggedIn`, (res) => {
                    resolve(res.statusCode === 200);
                });
                req.on('error', () => resolve(false));
                req.setTimeout(500, () => { req.destroy(); resolve(false); });
            });
            if (isResponding) {
                backendPort = port;
                console.log('[main] Discovered backend on port:', port);
                return port;
            }
        } catch (e) {
            // Continue to next port
        }
    }
    console.log('[main] Using default port:', backendPort);
    return backendPort;
}

// Initialize backend port
async function initBackendPort() {
    // First try to read from file
    readBackendPort();
    // Then try to discover (validates the port is actually running)
    await discoverBackendPort();
}

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

    if (fs.existsSync(iconPath)) {
        windowOptions.icon = iconPath;
    }

    mainWindow = new BrowserWindow(windowOptions);
    mainWindow.removeMenu();

    app.setPath('userData', path.join(app.getPath('userData'), 'PandaClientData'));

    mainWindow.loadFile("index.html").catch(err => {
        console.error("Failed to load index.html:", err);
    });

    // DevTools
    mainWindow.webContents.openDevTools();
}

// new window for Instance Manager
function openInstanceManagerWindow() {
    const instanceWindow = new BrowserWindow({
        width: 800,
        height: 600,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: true
        }
    });

    instanceWindow.removeMenu();
    instanceWindow.loadFile("sites/instance-manager.html").catch(err => {
        console.error("Failed to load instance-manager.html:", err);
    });
}

// Backend-Shutdown
function shutdownBackend(timeoutMs = 1500) {
    return new Promise((resolve) => {
        let settled = false;
        const req = http.get(`http://127.0.0.1:${backendPort}/shutdown`, (res) => {
            res.on("data", () => {});
            res.on("end", () => { if (!settled) { settled = true; resolve(); } });
        });
        req.on("error", () => { if (!settled) { settled = true; resolve(); } });
        req.setTimeout(timeoutMs, () => { try { req.destroy(); } finally { if (!settled) { settled = true; resolve(); } } });
    });
}

// Java Swing Dashboard
function callJavaOpenServerDashboard() {
    return new Promise((resolve) => {
        const req = http.get(`http://127.0.0.1:${backendPort}/openServerDashboard`, (res) => {
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

// IPC
ipcMain.on('app-quit', () => {
    shutdownBackend(1500).finally(() => app.quit());
});

// Get backend port for renderer
ipcMain.handle('get-backend-port', () => {
    return backendPort;
});

// Dashboard-window
ipcMain.handle('open-overview-window', async () => {
    const result = await callJavaOpenServerDashboard();
    return result && typeof result === 'object' ? result : { success: false };
});

// Instance Manager
ipcMain.on('open-instance-manager', () => {
    console.log("Opening Instance Manager window");
    openInstanceManagerWindow();
});
ipcMain.on('open-instance-manager-window', () => {
    console.log("Opening Instance Manager");
    const win = new BrowserWindow({
        width: 800,
        height: 600,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false
        }
    });
    win.removeMenu();
    win.loadFile('sites/instance-manager.html');
});


// App Ready
app.whenReady().then(async () => {
    await initBackendPort();
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
