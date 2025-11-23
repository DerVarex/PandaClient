console.log('renderer-edit-instance.js loaded');

// Simple helper to read query params
function getQueryParam(key) {
    const params = new URLSearchParams(window.location.search);
    return params.get(key);
}

function normalizeInstance(raw) {
    if (!raw || typeof raw !== 'object') return null;
    const loaderRaw = raw.loader || raw.modloader || '';
    const loaderPretty = loaderRaw ? loaderRaw.charAt(0) + loaderRaw.slice(1).toLowerCase() : '';
    return {
        original: raw,
        id: raw.id || raw.profileName || raw.name || '',
        name: raw.profileName || raw.name || raw.title || 'Instance',
        version: raw.versionId || raw.version || raw.minecraftVersion || '',
        loader: loaderPretty || 'Unknown'
    };
}

async function fetchInstanceById(id) {
    try {
        const res = await fetch('http://localhost:8800/instances');
        const raw = await res.json();
        const arr = Array.isArray(raw) ? raw : (raw.instances || []);
        const all = arr.map(normalizeInstance).filter(Boolean);
        return all.find(i => i.id === id || i.name === id) || null;
    } catch (e) {
        console.error('Failed to fetch instances for edit:', e);
        return null;
    }
}

function fillForm(inst) {
    const titleEl = document.getElementById('instedit-name');
    const subEl = document.getElementById('instedit-subtitle');
    const badgeEl = document.getElementById('instedit-badge');
    const nameInput = document.getElementById('instedit-name-input');

    if (!titleEl || !subEl || !badgeEl || !nameInput) {
        console.error('[inst-edit] Missing expected DOM elements');
        return;
    }

    if (inst) {
        titleEl.textContent = inst.name;
        subEl.textContent = `${inst.loader} \u00b7 ${inst.version || ''}`.trim();
        badgeEl.textContent = 'PROFILE';
        nameInput.value = inst.name;
    } else {
        titleEl.textContent = 'Unknown Instance';
        subEl.textContent = '';
        badgeEl.textContent = 'UNKNOWN';
        nameInput.value = '';
    }
}

async function submitEditForm(e) {
    e.preventDefault();
    const msg = document.getElementById('instedit-message');
    msg.textContent = '';

    const id = getQueryParam('id') || getQueryParam('profileName') || '';
    const name = document.getElementById('instedit-name-input').value.trim();

    if (!name) {
        msg.textContent = 'Name cannot be empty.';
        return;
    }

    const payload = { id, name};
    console.log('[inst-edit][submit]', payload);

    try {
        const res = await fetch('http://localhost:8800/edit-instance', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            // Send only the expected fields (id and name) as plain JSON
            body: JSON.stringify({ id: String(id || ''), name: String(name) })
        });
        const data = await res.json().catch(() => ({}));
        if (data.success) {
            msg.textContent = 'Saved. Redirecting…';
            setTimeout(() => {
                window.location.href = 'instance-manager.html';
            }, 700);
        } else {
            msg.textContent = data.error || 'Saving failed.';
        }
    } catch (e) {
        console.error('Edit instance failed:', e);
        msg.textContent = 'Saving failed (network / backend error).';
    }
}

window.addEventListener('DOMContentLoaded', async () => {
    const id = getQueryParam('id') || getQueryParam('profileName');
    if (id) {
        const inst = await fetchInstanceById(id);
        fillForm(inst);
    } else {
        fillForm(null);
    }

    const form = document.getElementById('instedit-form');
    if (form) {
        form.addEventListener('submit', submitEditForm);
    }
});
