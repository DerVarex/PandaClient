const { app, BrowserWindow } = require("electron");
const path = require("path");
const fs = require("fs");

function createWindow() {
    const iconPath = path.join(__dirname, "images", "logo.ico");
    const windowOptions = {
        width: 800,
        height: 600,
        frame: false,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: true
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

    win.webContents.openDevTools();
}

app.whenReady().then(() => {
    createWindow();

    app.on("activate", () => {
        if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
});

app.on("window-all-closed", () => {
    if (process.platform !== "darwin") app.quit();
});
