const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
    quit: () => ipcRenderer.send('app-quit'),
    openOverviewWindow: () => ipcRenderer.invoke('open-overview-window'),
    openInstanceManager: () => ipcRenderer.send('open-instance-manager-window')
});
