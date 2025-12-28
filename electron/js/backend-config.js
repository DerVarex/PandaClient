// Backend configuration - discovers and provides the backend port
(function () {
    const DEFAULT_PORT = 8800;
    const MAX_PORT_ATTEMPTS = 10;
    let cachedPort = null;

    /**
     * Get the backend port. Tries multiple strategies:
     * 1. Check if already cached
     * 2. Get from Electron's main process via IPC
     * 3. Read from preload cached value
     * 4. Auto-discover by probing ports starting from 8800
     */
    async function getBackendPort() {
        if (cachedPort !== null) {
            return cachedPort;
        }

        // Strategy 1: Get from Electron API (main process has the correct port)
        if (window.electronAPI && typeof window.electronAPI.getBackendPort === 'function') {
            try {
                const port = await window.electronAPI.getBackendPort();
                if (port && port > 0) {
                    console.log('[backend-config] Port from main process:', port);
                    cachedPort = port;
                    return port;
                }
            } catch (e) {
                console.warn('[backend-config] Could not get port from main process:', e.message);
            }
        }

        // Strategy 2: Get cached port from preload
        if (window.electronAPI && typeof window.electronAPI.getCachedBackendPort === 'function') {
            const port = window.electronAPI.getCachedBackendPort();
            if (port && port > 0) {
                console.log('[backend-config] Port from preload cache:', port);
                cachedPort = port;
                return port;
            }
        }

        // Strategy 3: Auto-discover by probing ports
        cachedPort = await discoverPort();
        return cachedPort;
    }

    /**
     * Probes ports starting from DEFAULT_PORT to find the backend
     */
    async function discoverPort() {
        for (let offset = 0; offset < MAX_PORT_ATTEMPTS; offset++) {
            const port = DEFAULT_PORT + offset;
            if (await isPortResponding(port)) {
                console.log('[backend-config] Discovered backend on port:', port);
                return port;
            }
        }
        console.warn('[backend-config] Could not discover backend port, using default:', DEFAULT_PORT);
        return DEFAULT_PORT;
    }

    /**
     * Check if backend is responding on given port
     */
    async function isPortResponding(port) {
        try {
            const controller = new AbortController();
            const timeout = setTimeout(() => controller.abort(), 500);
            const response = await fetch(`http://localhost:${port}/isLoggedIn`, {
                method: 'GET',
                signal: controller.signal
            });
            clearTimeout(timeout);
            return response.ok;
        } catch (e) {
            return false;
        }
    }

    /**
     * Get the base URL for the backend API
     */
    async function getBackendUrl() {
        const port = await getBackendPort();
        return `http://localhost:${port}`;
    }

    /**
     * Create a fetch wrapper that uses the correct backend port
     */
    async function backendFetch(endpoint, options = {}) {
        const baseUrl = await getBackendUrl();
        const url = endpoint.startsWith('/') ? `${baseUrl}${endpoint}` : `${baseUrl}/${endpoint}`;
        return fetch(url, options);
    }

    // Synchronous getter for cached port (returns null if not yet discovered)
    function getCachedPort() {
        return cachedPort;
    }

    // Initialize port discovery on load
    let portPromise = null;
    function initPortDiscovery() {
        if (!portPromise) {
            portPromise = getBackendPort();
        }
        return portPromise;
    }

    // Export globally
    window.backendConfig = {
        getBackendPort,
        getBackendUrl,
        backendFetch,
        getCachedPort,
        initPortDiscovery,
        DEFAULT_PORT
    };

    // Start discovery immediately
    initPortDiscovery();
})();

