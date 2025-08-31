require('electron-reload')(__dirname, {
    electron: require(`${__dirname}/node_modules/electron`)
});



const { app, BrowserWindow } = require("electron");
const path = require("path");

function createWindow() {
    const win = new BrowserWindow({
        width: 800,
        height: 600,
        frame: false,
        webPreferences: {
            //preload: path.join(__dirname, "preload.js"), // optional
            nodeIntegration : true,
            contextIsolation : true
        }
    });
    // Settings damit es akzeptabel aussieht :)
    win.removeMenu();
    win.setIcon("images/logo.ico")

    // Damit der müll nicht in PandaClient ordner ist1
    app.setPath('userData', path.join(app.getPath('userData'), 'PandaClientData'));

    win.loadFile("index.html");
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
