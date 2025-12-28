const { contextBridge, ipcRenderer } = require('electron');
const fs = require('fs');
const path = require('path');

// Backend port discovery for renderer
let cachedBackendPort = null;

function readBackendPortFromFile() {
    try {
        const portFilePath = path.join(__dirname, '.backend-port');
        if (fs.existsSync(portFilePath)) {
            const portStr = fs.readFileSync(portFilePath, 'utf8').trim();
            const port = parseInt(portStr, 10);
            if (!isNaN(port) && port > 0) {
                return port;
            }
        }
    } catch (e) {
        console.warn('[preload] Could not read port file:', e.message);
    }
    return 8800; // Default port
}

// Initialize cached port
cachedBackendPort = readBackendPortFromFile();

contextBridge.exposeInMainWorld('electronAPI', {
    quit: () => ipcRenderer.send('app-quit'),
    openOverviewWindow: () => ipcRenderer.invoke('open-overview-window'),
    openInstanceManager: () => ipcRenderer.send('open-instance-manager-window'),

    // Backend port discovery
    getBackendPort: () => ipcRenderer.invoke('get-backend-port'),
    getCachedBackendPort: () => cachedBackendPort,
    getBackendUrl: () => `http://localhost:${cachedBackendPort}`,

    // Utility to refresh the cached port
    refreshBackendPort: () => {
        cachedBackendPort = readBackendPortFromFile();
        return cachedBackendPort;
    }
});
