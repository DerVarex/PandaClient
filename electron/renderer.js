/*document.getElementById('Test').addEventListener('click', async () => {
    try {
        const response = await fetch('http://localhost:8080/Test');
        const mods = await response.json();

        const list = document.getElementById('testList');
        list.innerHTML = '';
        mods.forEach(mod => {
            const li = document.createElement('li');
            li.textContent = mod;
            list.appendChild(li);
        });
    } catch (err) {
        console.error("Fehler beim Abrufen vom Test:", err);
    }
}); */ //War nur nen test, ob es geht

const imageInput = document.getElementById("image");
let selectedInstance = null;

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
    renderInstances(instances);
    return instances;
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

async function swapVersion(event) {
    console.log("Swap clicked");
    if (event) event.stopPropagation(); // verhindert, dass startGame() mitfeuert

    const dropdown = document.getElementById("swap-dropdown");
    // toggle visible
    dropdown.classList.toggle("hidden");
    if (dropdown.classList.contains("hidden")) return;

    dropdown.innerHTML = ''; // clear

    try {
        const res = await fetch("http://localhost:8800/instances");
        const instances = await res.json();
        console.log("Instances from backend:", instances);

        if (!Array.isArray(instances) || instances.length === 0) {
            const empty = document.createElement("div");
            empty.className = 'instance-item';
            empty.textContent = "Keine Instanzen gefunden";
            dropdown.appendChild(empty);
            return;
        }

        instances.forEach(inst => {
            const item = document.createElement("div");
            item.className = 'instance-item';
            item.textContent = formatInstanceLabel(inst);

            item.onclick = (e) => {
                e.stopPropagation(); // wichtig, damit der click nicht hochgeht
                selectedInstance = inst;
                updateLaunchButton(inst);
                dropdown.classList.add("hidden");
                showNotification("INFO", `Ausgewählt: ${inst.profileName || inst.name || 'Instance'}`);
            };

            dropdown.appendChild(item);
        });
    } catch (err) {
        console.error("Fehler beim Laden der Instanzen:", err);
        const errEl = document.createElement("div");
        errEl.className = 'instance-item';
        errEl.textContent = "Fehler beim Laden";
        dropdown.appendChild(errEl);
    }
}



function showInstanceSelector(instances) {
    const container = document.getElementById('instance-container');
    container.innerHTML = ''; // Reset

    const list = document.createElement('ul');
    list.classList.add('instance-selector');

    instances.forEach(inst => {
        const item = document.createElement('li');
        item.textContent = `${inst.name} (${inst.modloader} ${inst.version})`;

        item.addEventListener('click', () => {
            selectedInstance = inst; // speichern, welche Instanz gewählt wurde
            updateLaunchButton(inst);
            container.innerHTML = ''; // Auswahl schließen
        });

        list.appendChild(item);
    });

    container.appendChild(list);
}

function updateLaunchButton(inst) {
    const launchTitle = document.querySelector('.launch-btn .title');
    launchTitle.textContent = `${inst.name} ${inst.version}`;
}
// Startet die gewählte Instance
async function startGame() {
    if (!selectedInstance) {
        alert('Bitte zuerst eine Instance auswählen!');
        return;
    }
    console.log('Launching', selectedInstance);
    // Starten
    const start = await fetch("http://localhost:8800/launch");
}

function renderInstances(instances) {
    const container = document.getElementById('instance-container');
    container.innerHTML = ''; // Reset

    instances.forEach(inst => {
        const card = document.createElement('article');
        card.classList.add('card');
        card.innerHTML = `
      <div class="visual">
        <img src="${inst.image || 'images/default.png'}" alt="${inst.name}" />
      </div>
      <div class="info">
        <div class="title-row">
          <h2 class="instance-title">${inst.name}</h2>
          <div class="tag">${inst.modloader} • ${inst.version}</div>
        </div>
        <div class="meta">Klicke auf den Titel, um Details anzuzeigen.</div>
      </div>
    `;

        // Klick-Handler (wie in deinem Beispiel)
        const title = card.querySelector('.instance-title');
        const meta = card.querySelector('.meta');
        let toggled = false;

        title.addEventListener('click', () => {
            toggled = !toggled;
            title.classList.toggle('selected', toggled);
            meta.textContent = toggled
                ? `Details: Diese Instanz läuft auf ${inst.modloader} ${inst.version}.`
                : 'Klicke auf den Titel, um Details anzuzeigen.';
        });

        container.appendChild(card);
    });
}

// Öffnen und Schließen
function openInstancesWindow() {
    document.getElementById("instances-window").classList.remove("hidden");
    fetchInstances(); // Instanzen laden und anzeigen
}

function closeInstancesWindow() {
    document.getElementById("instances-window").classList.add("hidden");
}
function openLoginWindow() {
    document.getElementById("login-window").classList.remove("hidden");
}
function closeLoginWindow() {
    document.getElementById("login-window").classList.add("hidden");
}
function openCreateInstanceWindow() {
    document.getElementById("create-instance-window").classList.remove("hidden");
}
function closeCreateInstanceWindow() {
    document.getElementById("create-instance-window").classList.add("hidden");
}

function makeDraggable(windowId) {
    const win = document.getElementById(windowId);
    const header = win.querySelector("div:first-child"); // Header
    let offsetX, offsetY, isDown = false;

    header.addEventListener("mousedown", e => {
        isDown = true;
        offsetX = e.clientX - win.offsetLeft;
        offsetY = e.clientY - win.offsetTop;
    });

    document.addEventListener("mouseup", () => isDown = false);

    document.addEventListener("mousemove", e => {
        if (!isDown) return;

        let x = e.clientX - offsetX;
        let y = e.clientY - offsetY;

        // Fenster innerhalb des Browserfensters halten
        const maxX = window.innerWidth - win.offsetWidth;
        const maxY = window.innerHeight - win.offsetHeight;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;

        win.style.left = x + "px";
        win.style.top = y + "px";
    });
}

// Anwenden auf beide Fenster
makeDraggable("instances-window");
makeDraggable("login-window");
makeDraggable("create-instance-window");

checkLogin();
fetchInstances();
