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

                const email = document.querySelector('input[name="Email"]').value;
                const password = document.querySelector('input[name="password"]').value;

                try {
                    // Port auf 8800
                    const respLogin = await fetch(`http://localhost:8800/login?email=${email}&password=${password}`);
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
}

function renderInstances(instances) {
    const container = document.getElementById('instance-container');
    container.innerHTML = ''; // Reset

    instances.forEach(inst => {
        const card = document.createElement('article');
        card.classList.add('card');
        card.innerHTML = `
      <div class="visual">
        <img src="${inst.image || 'ExampleImage.jpg'}" alt="${inst.name}" />
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