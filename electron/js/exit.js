// Renderer-side exit helper: request backend shutdown, then quit Electron
(function () {
  async function exitApp() {
    try {
      // Get the backend port dynamically
      let backendUrl = 'http://localhost:8800';
      if (window.backendConfig) {
        const port = await window.backendConfig.getBackendPort();
        backendUrl = `http://localhost:${port}`;
      }

      // Ask backend to shutdown (System.exit(0))
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 1500);
      await fetch(`${backendUrl}/shutdown`, { method: 'GET', signal: controller.signal });
      clearTimeout(timer);
    } catch (e) {
      console.warn('[exit] Backend /shutdown failed or timed out:', e);
    } finally {
      // Quit Electron app cross-platform via IPC exposed in preload
      if (window.electronAPI && typeof window.electronAPI.quit === 'function') {
        window.electronAPI.quit();
      } else {
        try {
          // Fallback if preload not available
          const { ipcRenderer } = require('electron');
          ipcRenderer.send('app-quit');
        } catch (_) {
          // As a last resort, close the window (won't exit on macOS)
          window.close();
        }
      }
    }
  }

  // Expose globally so inline handlers or other scripts can call it
  window.exitApp = exitApp;
})();
