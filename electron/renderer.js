console.log('renderer.js loaded');

const imageInput = document.getElementById("image");

document.getElementById("create-instance-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    const formData = new FormData(e.target);
    const data = {
        name: formData.get("name") || "",
        version: formData.get("version") || "",
        modloader: formData.get("instance-type") || ""
    };

    console.log(data);
    console.log("Name ist in js angekommen: " + data.name);

    // Image optional
    if (imageInput.files.length > 0) {
        data.image = imageInput.files[0].path; // Electron erlaubt file.path
    } else {
        data.image = ""; // nie null
    }

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

    // Slide-out und entfernen nach 4 Sekunden
    setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(100%)";
    setTimeout(() => container.removeChild(toast), 500);
}, 4000);
}

async function checkLogin() {
    try {
        const resp = await fetch("http://localhost:8800/isLoggedIn");
        const data = await resp.json();
        console.log("isLoggedIn Antwort:", data);

        if (!data.loggedIn) {
            openLoginWindow();
            console.log("Not logged in.");

            document.querySelector('.login-form').addEventListener('submit', async (e) => {
                e.preventDefault();



                try {
                    // Port auf 8800
                    const respLogin = await fetch(`http://localhost:8800/login`);
                    const result = await respLogin.json();
                    console.log("Login Result:", result);

                    if (result.success) {
                        closeLoginWindow();
                    } else {
                        alert("Login failed!");
                    }
                } catch (err) {
                    console.error("Fehler beim Login:", err);
                }
            });
        } else {
            console.log("Logging in with token.");
            try{
                const response = await fetch("http://localhost:8800/loginWithToken");
                const answer = await response.json();
            } catch (err) {
                console.error("Fehler beim Abrufen von isLoggedIn(hasTokenSaved=true): ", err);
            }
        }
    } catch (err) {
        console.error("Fehler beim Abrufen von isLoggedIn:", err);
    }
}
async function fetchInstances() {
    const res = await fetch('http://localhost:8800/instances');
    const instances = await res.json();
    // renderInstances(instances); // Commented out to avoid error
    return instances;
}
// Add stub for renderInstances to avoid ReferenceError if called elsewhere
function renderInstances(instances) {
    // Stub: do nothing or log
    console.log('renderInstances called (stub)', instances);
}

function formatInstanceLabel(inst) {
    // robust gegen verschiedene Feldnamen
    const name = inst.profileName || inst.name || inst.title || "Instance";
    let loader = inst.loader || inst.modloader || "";
    // friendly mapping (falls backend "UNK" liefert)
    if (!loader || /^UNK|UNKNOWN$/i.test(loader)) loader = "Vanilla";
    const version = inst.versionId || inst.version || "";
    return `${name} (${loader} ${version})`.trim();
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
        const res = await fetch('http://localhost:8888/instances');
        const instances = await res.json();
        instancesCache = Array.isArray(instances) ? instances : (instances.instances || []);
        renderInstanceList(instancesCache);
    } catch (err) {
        renderInstanceList([]);
        showNotification('ERROR', 'Could not fetch instances from backend');
        console.error('Error fetching instances:', err);
    }
}

function renderInstanceList(instances) {
    const list = document.getElementById('select-instance-list');
    list.innerHTML = '';
    if (!instances.length) {
        const div = document.createElement('div');
        div.textContent = 'You don’t have instances';
        div.style.color = '#aaa';
        list.appendChild(div);
        return;
    }
    instances.forEach(inst => {
        const div = document.createElement('div');
        div.className = 'instance-option';
        div.textContent = `${inst.name || inst.profileName || 'Unknown'} (${inst.version || inst.minecraftVersion || ''})`;
        div.onclick = () => {
            selectedInstance = inst;
            updateLaunchButton();
            closeSelectInstanceWindow();
        };
        list.appendChild(div);
    });
}

// --- GLOBAL MODAL OPEN/CLOSE FUNCTIONS ---
window.openInstancesWindow = function() {
    document.getElementById('instances-window').classList.remove('hidden');
};
window.closeInstancesWindow = function() {
    document.getElementById('instances-window').classList.add('hidden');
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

// Remove all event delegation for close buttons (handled by HTML onclick)
// Keep outside click logic if desired

document.addEventListener('mousedown', function(event) {
    const modals = [
        document.getElementById('select-instance-window'),
        document.getElementById('instances-window'),
        document.getElementById('login-window'),
        document.getElementById('create-instance-window')
    ];
    modals.forEach(modal => {
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

makeDraggable('select-instance-window', '.select-instance-header');
makeDraggable('instances-window', '.instances-header');
makeDraggable('login-window', '.login-header');
makeDraggable('create-instance-window', '.create-instance-header');

checkLogin();
fetchInstances();
