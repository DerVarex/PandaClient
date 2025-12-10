console.log('renderer.js loaded');

// Ensure instance manager starts hidden on load
window.addEventListener('DOMContentLoaded', () => {
    const im = document.getElementById('instance-manager');
    if (im) im.classList.add('hidden');
});

const imageInput = document.getElementById("image");

document.getElementById("create-instance-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    const formData = new FormData(e.target);
    const data = {
        name: formData.get("name") || "",
        version: formData.get("version") || "",
        modloader: (formData.get("modloader") || "").toUpperCase()
    };


    console.log(data);
    console.log("Name ist in js angekommen: " + data.name);

//    // Image optional
//    if (imageInput.files.length > 0) {
//        data.image = imageInput.files[0].path; // Electron erlaubt file.path
//    } else {
//        data.image = ""; // nie null
//    }

    try {
        const res = await fetch("http://localhost:8800/create-instance", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });

        const result = await res.json();
        console.log("Instance created:", result);
        if (result.success) closeCreateInstanceWindow();
        else alert("Error creating instance: " + result.error);
    } catch (err) {
        console.error("Fehler beim Erstellen der Instance:", err);
    }
});


window.showNotification = function(type, message) {
    console.log("Notification:", type, message);
    const container = document.getElementById("notifications");

    const colors = {
    INFO: "#2E8B57",      // dunkelgrün
    WARNING: "#FFA500",   // orange
    ERROR: "#FF4500"      // rot
};
    console.log(colors);

    const toast = document.createElement("div");
    toast.innerText = `[${type}] ${message}`;
    toast.style.background = colors[type] || "gray";
    toast.style.color = "white";
    toast.style.padding = "15px 20px";
    toast.style.borderRadius = "8px";
    toast.style.boxShadow = "0px 0px 10px rgba(0,0,0,0.5)";
    toast.style.fontSize = "16px";
    toast.style.fontWeight = "bold";
    toast.style.opacity = "0";
    toast.style.transform = "translateX(100%)"; // rechts raus starten
    toast.style.transition = "transform 0.5s, opacity 0.5s";

    container.appendChild(toast);

    // Slide-in Animation
    setTimeout(() => {
    toast.style.opacity = "1";
    toast.style.transform = "translateX(0)";
}, 10);

    let timeout;

if(type === "ERROR") {
    timeout = 10000;
} else {
    timeout = 4000;
}
    // Slide-out und entfernen nach 4 Sekunden
    setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(100%)";
    setTimeout(() => container.removeChild(toast), 500);
}, timeout);
}

async function checkLogin() {
    try {
        const resp = await fetch("http://localhost:8800/isLoggedIn");
        const data = await resp.json();
        console.log("isLoggedIn Antwort:", data);

        if (data.loggedIn) {
            console.log("Already logged in.");
            return;
        }
        // Try saved session first if available
        if (data.hasSaved) {
            try {
                const respSaved = await fetch("http://localhost:8800/loginWithToken");
                const resSaved = await respSaved.json();
                if (resSaved.success) {
                    closeLoginWindow();
                    return;
                }
            } catch (e) {
                console.warn("loginWithToken failed, falling back to device code:", e);
            }
        }
        openLoginWindow();
        console.log("Not logged in.");



        const form = document.querySelector('.login-form');
        if (!form._handlerAttached) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                startDeviceCodeLogin();
            });
            form._handlerAttached = true;
        }
    } catch (err) {
        console.error("Fehler beim Abrufen von isLoggedIn:", err);
    }
}

let loginStatusPoller = null;
async function startDeviceCodeLogin() {
    try {
        const resp = await fetch(`http://localhost:8800/login`);
        const state = await resp.json();
        console.log("/login state:", state);
        updateLoginUI(state);
        // start polling until SUCCESS or ERROR
        if (loginStatusPoller) clearInterval(loginStatusPoller);
        loginStatusPoller = setInterval(pollLoginStatus, 2000);
    } catch (e) {
        console.error("Start login failed:", e);
        showNotification('ERROR', 'Could not start login');
    }
}

async function pollLoginStatus() {
    try {
        const resp = await fetch(`http://localhost:8800/login/status`);
        const state = await resp.json();
        console.log("/login/status:", state);
        updateLoginUI(state);
        if (state.status === 'SUCCESS') {
            clearInterval(loginStatusPoller);
            loginStatusPoller = null;
            showNotification('INFO', `Logged in as ${state.username || ''}`);
            closeLoginWindow();
        } else if (state.status === 'ERROR') {
            clearInterval(loginStatusPoller);
            loginStatusPoller = null;
            showNotification('ERROR', state.message || 'Login failed');
        }
    } catch (e) {
        console.error("Polling login status failed:", e);
    }
}

function updateLoginUI(state) {
    const statusText = document.getElementById('login-status-text');
    const codeBox = document.getElementById('device-code-box');
    const codeEl = document.getElementById('device-code');
    const linkEl = document.getElementById('verify-link');
    if (!statusText) return;

    const s = state.status || (state.success ? 'SUCCESS' : 'IDLE');
    statusText.textContent = state.message || (s === 'PENDING' ? 'Waiting for authorization…' : 'Starting login…');
    if (state.userCode) {
        codeBox.style.display = '';
        codeEl.textContent = state.userCode;
        if (state.directVerificationUri) {
            linkEl.href = state.directVerificationUri;
        } else if (state.verificationUri) {
            linkEl.href = state.verificationUri;
        }
    }
}

async function fetchInstances() {
    const res = await fetch('http://localhost:8800/instances');
    const instances = await res.json();
    console.log('[instances][raw]', instances);
    // renderInstances(instances); // Commented out to avoid error
    return instances;
}
// Add stub for renderInstances to avoid ReferenceError if called elsewhere
function renderInstances(instances) {
    // Stub: do nothing or log
    console.log('renderInstances called (stub)', instances);
}

// Normalisierung: Backend -> vereinheitlichte Struktur
function normalizeInstance(raw) {
    if (!raw || typeof raw !== 'object') return null;
    const loaderRaw = raw.loader || raw.modloader || '';
    // Loader hübscher machen (z.B. FABRIC -> Fabric)
    const loaderPretty = loaderRaw ? loaderRaw.charAt(0) + loaderRaw.slice(1).toLowerCase() : '';
    return {
        original: raw,
        name: raw.profileName || raw.name || raw.title || 'Instance',
        version: raw.versionId || raw.version || raw.minecraftVersion || '',
        loader: loaderPretty || 'Unknown',
        imagePath: raw.profileImagePath || raw.profileImage || raw.image || 'images/default.png'
    };
}
function normalizeInstances(arr) {
    if (!Array.isArray(arr)) return [];
    return arr.map(normalizeInstance).filter(Boolean);
}

function formatInstanceLabel(inst) {
    // inst kann bereits normalisiert sein; falls nicht, normalisieren
    const n = inst.name ? inst : normalizeInstance(inst);
    return `${n.name} (${n.loader} ${n.version})`.trim();
}

// --- Instance Selection Logic ---
let selectedInstance = null;
let instancesCache = [];

window.swapVersion = async function(event) {
    event.stopPropagation();
    document.getElementById('select-instance-window').classList.remove('hidden');
    await fetchInstancesFromBackend();
};

async function fetchInstancesFromBackend() {
    try {
        const res = await fetch('http://localhost:8800/instances');
        const raw = await res.json();
        console.log('[instances][raw fetchInstancesFromBackend]', raw);
        const arr = Array.isArray(raw) ? raw : (raw.instances || []);
        instancesCache = normalizeInstances(arr);
        console.log('[instances][normalized]', instancesCache);
        renderInstanceList(instancesCache);
    } catch (err) {
        renderInstanceList([]);
        showNotification('ERROR', 'Could not fetch instances from backend');
        console.error('Error fetching instances:', err);
    }
}

function renderInstanceList(instances) {
    const list = document.getElementById('select-instance-list');
    if (!list) return;
    list.innerHTML = '';
    if (!instances.length) {
        const div = document.createElement('div');
        div.textContent = 'You don’t have instances';
        div.style.color = '#aaa';
        list.appendChild(div);
        return;
    }
    instances.forEach(nInst => {
        const inst = nInst.name ? nInst : normalizeInstance(nInst);
        const div = document.createElement('div');
        div.className = 'instance-option';
        div.textContent = formatInstanceLabel(inst);
        div.onclick = () => {
            selectedInstance = inst; // Speichere normalisierte Instanz
            updateLaunchButton();
            closeSelectInstanceWindow();
        };
        list.appendChild(div);
    });
}

window.closeInstancesWindow = function() {
    const el = document.getElementById('instances-window');
    if (el) el.classList.add('hidden');
};
window.openCreateInstanceWindow = function() {
    document.getElementById('create-instance-window').classList.remove('hidden');
};
window.closeCreateInstanceWindow = function() {
    document.getElementById('create-instance-window').classList.add('hidden');
};
window.openLoginWindow = function() {
    document.getElementById('login-window').classList.remove('hidden');
};
window.closeLoginWindow = function() {
    document.getElementById('login-window').classList.add('hidden');
};
window.openSelectInstanceWindow = function() {
    document.getElementById('select-instance-window').classList.remove('hidden');
};
window.closeSelectInstanceWindow = function() {
    document.getElementById('select-instance-window').classList.add('hidden');
};

// --- END GLOBAL MODAL OPEN/CLOSE FUNCTIONS ---

document.addEventListener('mousedown', function(event) {
    const closableModals = [
        document.getElementById('select-instance-window'),
        document.getElementById('instances-window'),
        document.getElementById('create-instance-window')
    ];
    closableModals.forEach(modal => {
        if (modal && !modal.classList.contains('hidden') && !modal.contains(event.target)) {
            modal.classList.add('hidden');
        }
    });
});

// Fix makeDraggable to not start drag when clicking the close button
function makeDraggable(windowId, headerClass) {
    const win = document.getElementById(windowId);
    const header = win.querySelector(headerClass);
    let offsetX, offsetY, isDown = false;
    if (!header) return;
    header.addEventListener('mousedown', e => {
        // Prevent drag if close button is clicked
        if (e.target.classList.contains('close-btn')) return;
        isDown = true;
        offsetX = e.clientX - win.offsetLeft;
        offsetY = e.clientY - win.offsetTop;
    });
    document.addEventListener('mouseup', () => isDown = false);
    document.addEventListener('mousemove', e => {
        if (!isDown) return;
        let x = e.clientX - offsetX;
        let y = e.clientY - offsetY;
        const maxX = window.innerWidth - win.offsetWidth;
        const maxY = window.innerHeight - win.offsetHeight;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;
        win.style.left = x + 'px';
        win.style.top = y + 'px';
    });
}

function updateLaunchButton() {
    try {
        const titleEl = document.querySelector('.launch-btn .title');
        const subEl = document.querySelector('.launch-btn .subtitle');
        if (!titleEl || !subEl) return;
        if (selectedInstance) {
            const name = selectedInstance.name || selectedInstance.profileName || 'Instance';
            const ver = selectedInstance.version || selectedInstance.minecraftVersion || '';
            const loader = selectedInstance.loader || selectedInstance.modloader || '';

            console.log("Selected instance:", selectedInstance);
            console.log("Name:", name, "Version:", ver, "Loader:", loader);
            // Trim to avoid extra spaces if ver or loader are empty

            titleEl.textContent = `Launch ${ver || ''}`.trim();
            subEl.textContent = `${name} ${loader} ${ver}`.trim();
        }
    } catch (e) {
        console.warn('updateLaunchButton failed:', e);
    }
}

async function startGame() {
    if (!selectedInstance) {
        showNotification('WARNING', 'Please select an instance first (click the swap icon).');
        return;
    }
    try {
        const inst = selectedInstance.name ? selectedInstance : normalizeInstance(selectedInstance);
        const params = new URLSearchParams();
        if (inst && inst.name) params.set('profileName', inst.name); // Backend erwartet profileName
        const url = `http://localhost:8800/launch${params.toString() ? ('?' + params.toString()) : ''}`;
        console.log('[launch][request]', url);
        const resp = await fetch(url);
        const data = await resp.json().catch(() => ({}));
        console.log('[launch][response]', data);
        showNotification(data.success ? 'INFO' : 'ERROR', data.success ? `Launching ${inst.name}` : (data.error || 'Launch failed'));
    } catch (e) {
        console.error('Launch failed:', e);
        showNotification('ERROR', 'Launch failed');
    }
}
window.openPandaClientFolder = async function() {
    try {
        await fetch("http://localhost:8800/openPandaClientFolder");
    } catch (e) {
        console.error("openPandaClientFolder failed:", e);
        showNotification('ERROR', 'Could not open Panda Client folder');
    }
};

// Add: open the server overview in a new native window using the preload-exposed API
window.openServerOverview = async function() {
    try {
        const res = await fetch('http://localhost:8800/openServerDashboard', {
            method: 'GET',
        });
        if (!res.ok) {
            throw new Error('HTTP ' + res.status);
        }
        const data = await res.json().catch(() => ({}));
        console.log('openServerDashboard result', data);
    } catch (err) {
        console.error('Failed to open server dashboard', err);
    }
};

// --- Server management helpers ---
/*
async function fetchServers() {
    try {
        const res = await fetch('http://localhost:8800/servers');
        const data = await res.json();
        console.log('[servers][response]', data);
        if (!data.success) {
            showNotification('ERROR', data.error || 'Could not fetch servers');
            return [];
        }
        return Array.isArray(data.servers) ? data.servers : [];
    } catch (e) {
        console.error('Error fetching servers:', e);
        showNotification('ERROR', 'Could not fetch servers');
        return [];
    }
}

async function startServerByName(name) {
    try {
        const res = await fetch('http://localhost:8800/server/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        const data = await res.json();
        console.log('[server/start]', name, data);
        if (!data.success) {
            showNotification('ERROR', data.error || `Could not start server ${name}`);
        } else {
            showNotification('INFO', `Starting server ${name}`);
        }
        return data;
    } catch (e) {
        console.error('startServerByName failed:', e);
        showNotification('ERROR', `Could not start server ${name}`);
        return { success: false, error: String(e) };
    }
}

async function stopServerByName(name) {
    try {
        const res = await fetch('http://localhost:8800/server/stop', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        const data = await res.json();
        console.log('[server/stop]', name, data);
        if (!data.success) {
            showNotification('ERROR', data.error || `Could not stop server ${name}`);
        } else {
            showNotification('INFO', `Stopping server ${name}`);
        }
        return data;
    } catch (e) {
        console.error('stopServerByName failed:', e);
        showNotification('ERROR', `Could not stop server ${name}`);
        return { success: false, error: String(e) };
    }
}

async function killServerByName(name) {
    try {
        const res = await fetch('http://localhost:8800/server/kill', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        const data = await res.json();
        console.log('[server/kill]', name, data);
        if (!data.success) {
            showNotification('ERROR', data.error || `Could not kill server ${name}`);
        } else {
            showNotification('INFO', `Killed server ${name}`);
        }
        return data;
    } catch (e) {
        console.error('killServerByName failed:', e);
        showNotification('ERROR', `Could not kill server ${name}`);
        return { success: false, error: String(e) };
    }
}

async function sendServerCommand(name, command) {
    try {
        const res = await fetch('http://localhost:8800/server/command', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, command })
        });
        const data = await res.json();
        console.log('[server/command]', name, command, data);
        if (!data.success) {
            showNotification('ERROR', data.error || `Command failed on ${name}`);
        }
        return data;
    } catch (e) {
        console.error('sendServerCommand failed:', e);
        showNotification('ERROR', `Command failed on ${name}`);
        return { success: false, error: String(e) };
    }
}

// Expose for quick testing / later UI wiring
window.serverApi = {
    fetchServers,
    startServerByName,
    stopServerByName,
    killServerByName,
    sendServerCommand,
}; */

// --- Instance Manager (list + detail) ---

let managerInstancesCache = [];
let managerActiveIndex = -1;

function renderInstanceListPanel(instances) {
    const panel = document.getElementById('instances-list-panel');
    if (!panel) return;
    panel.innerHTML = '';

    if (!instances.length) {
        const empty = document.createElement('div');
        empty.style.color = '#9ca3af';
        empty.style.fontSize = '12px';
        empty.style.padding = '6px 4px';
        empty.textContent = 'No instances yet.';
        panel.appendChild(empty);
        return;
    }

    instances.forEach((inst, idx) => {
        const item = document.createElement('div');
        item.className = 'instance-list-item' + (idx === managerActiveIndex ? ' active' : '');

        const nameSpan = document.createElement('span');
        nameSpan.className = 'instance-list-name';
        nameSpan.textContent = inst.name;

        const subSpan = document.createElement('span');
        subSpan.className = 'instance-list-sub';
        subSpan.textContent = `${inst.loader} ${inst.version}`.trim();

        item.appendChild(nameSpan);
        item.appendChild(subSpan);

        item.onclick = () => {
            managerActiveIndex = idx;
            renderInstanceListPanel(managerInstancesCache);
            renderInstanceDetail(inst);
        };

        panel.appendChild(item);
    });
}

function renderInstanceDetail(inst) {
    const empty = document.getElementById('instance-detail-empty');
    const detail = document.getElementById('instance-detail');
    if (!empty || !detail) return;

    if (!inst) {
        empty.classList.remove('hidden');
        detail.classList.add('hidden');
        detail.innerHTML = '';
        return;
    }

    empty.classList.add('hidden');
    detail.classList.remove('hidden');
    detail.innerHTML = '';

    const header = document.createElement('div');
    header.className = 'instance-detail-header';

    const title = document.createElement('div');
    title.className = 'instance-detail-title';
    title.textContent = inst.name;

    const chips = document.createElement('div');
    chips.className = 'instance-detail-chips';

    const chipLoader = document.createElement('div');
    chipLoader.className = 'instance-chip';
    chipLoader.textContent = inst.loader;

    const chipVersion = document.createElement('div');
    chipVersion.className = 'instance-chip';
    chipVersion.textContent = inst.version || 'Unknown version';

    chips.appendChild(chipLoader);
    chips.appendChild(chipVersion);

    header.appendChild(title);
    header.appendChild(chips);

    const main = document.createElement('div');
    main.className = 'instance-detail-main';

    const iconWrap = document.createElement('div');
    iconWrap.className = 'instance-detail-icon';
    const img = document.createElement('img');
    img.src = inst.imagePath || 'images/default.png';
    img.alt = inst.name;
    iconWrap.appendChild(img);

    const fields = document.createElement('div');
    fields.className = 'instance-detail-fields';

    const fProfile = document.createElement('div');
    fProfile.innerHTML = `<strong>Profile name:</strong> ${inst.name}`;

    const fLoader = document.createElement('div');
    fLoader.innerHTML = `<strong>Loader:</strong> ${inst.loader}`;

    const fVersion = document.createElement('div');
    fVersion.innerHTML = `<strong>Version:</strong> ${inst.version || 'Unknown'}`;

    fields.appendChild(fProfile);
    fields.appendChild(fLoader);
    fields.appendChild(fVersion);

    main.appendChild(iconWrap);
    main.appendChild(fields);

    const footer = document.createElement('div');
    footer.className = 'instance-detail-footer';

    const btnSelect = document.createElement('button');
    btnSelect.className = 'instance-btn';
    btnSelect.textContent = 'Use for Launch';
    btnSelect.onclick = () => {
        selectedInstance = inst;
        updateLaunchButton();
        showNotification('INFO', `Selected ${inst.name} as active instance`);
    };

    const btnLaunch = document.createElement('button');
    btnLaunch.className = 'instance-btn instance-btn-primary';
    btnLaunch.textContent = 'Launch now';
    btnLaunch.onclick = async () => {
        selectedInstance = inst;
        updateLaunchButton();
        await startGame();
    };

    // Placeholder for future edit functionality
    const btnEdit = document.createElement('button');
    btnEdit.className = 'instance-btn';
    btnEdit.textContent = 'Edit (soon)';
    btnEdit.disabled = true;

    footer.appendChild(btnSelect);
    footer.appendChild(btnLaunch);
    footer.appendChild(btnEdit);

    detail.appendChild(header);
    detail.appendChild(main);
    detail.appendChild(footer);
}

window.openInstanceManager = function() {
    // Navigate to dedicated instance manager page
    window.location.href = 'sites/instance-manager.html';
};

window.closeInstanceManager = function() {
    // No-op in main launcher now; kept for backward compatibility
};

checkLogin();
console.log("Login checked");

makeDraggable('select-instance-window', '.select-instance-header');
//makeDraggable('instance-manager', '.instance-manager-header');
makeDraggable('login-window', '.login-header');
makeDraggable('create-instance-window', '.create-instance-header');

closeInstanceManager();

fetchInstances();
