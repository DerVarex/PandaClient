console.log('renderer-instance-manager.js loaded');

// Cache für Bildpfade
const badImagePaths = new Set();
const goodImagePaths = new Set();
const DEFAULT_IMAGE = 'images/default.png';

function isWindowsPath(p){return /[A-Za-z]:\\/.test(p);} // einfache Erkennung

function resolveImagePath(p) {
    if (!p) return DEFAULT_IMAGE;
    // Browser-taugliche URLs direkt zurück
    if (/^(https?:|file:)/i.test(p)) return p;
    if (p.startsWith('images/')) return p; // bereits relativ zum sites Verzeichnis
    // Absolute Linux-Pfade
    if (p.startsWith('/')) return 'file://' + p;
    // Windows-Pfade
    if (isWindowsPath(p)) return 'file:///' + p.replace(/\\/g,'/');
    return p; // sonst unverändert (evtl. relativer Pfad der funktioniert)
}

function normalizeInstance(raw) {
    if (!raw || typeof raw !== 'object') return null;
    const loaderRaw = raw.loader || raw.modloader || '';
    const loaderPretty = loaderRaw ? loaderRaw.charAt(0) + loaderRaw.slice(1).toLowerCase() : '';
    const rawImage = raw.profileImagePath || raw.profileImage || raw.image || '';
    return {
        original: raw,
        name: raw.profileName || raw.name || raw.title || 'Instance',
        version: raw.versionId || raw.version || raw.minecraftVersion || '',
        loader: loaderPretty || 'Unknown',
        imagePath: resolveImagePath(rawImage) || DEFAULT_IMAGE
    };
}

function normalizeInstances(arr) {
    if (!Array.isArray(arr)) return [];
    return arr.map(normalizeInstance).filter(Boolean);
}

let imInstancesCache = [];
let imActiveIndex = -1;

function renderImList(instances) {
    const listEl = document.getElementById('im-instance-list');
    if (!listEl) return;
    listEl.innerHTML = '';

    if (!instances.length) {
        const empty = document.createElement('div');
        empty.textContent = 'No instances found.';
        empty.style.color = '#6b7280';
        empty.style.fontSize = '13px';
        empty.style.padding = '8px 4px';
        listEl.appendChild(empty);
        return;
    }

    instances.forEach((inst, idx) => {
        const item = document.createElement('div');
        item.className = 'im-instance-item' + (idx === imActiveIndex ? ' active' : '');

        const nameSpan = document.createElement('div');
        nameSpan.className = 'im-instance-name';
        nameSpan.textContent = inst.name;

        const subSpan = document.createElement('div');
        subSpan.className = 'im-instance-sub';
        subSpan.textContent = `${inst.loader} ${inst.version}`.trim();

        item.appendChild(nameSpan);
        item.appendChild(subSpan);

        item.onclick = () => {
            imActiveIndex = idx;
            renderImList(imInstancesCache);
            renderImDetail(inst);
        };

        listEl.appendChild(item);
    });
}

function loadImageStable(imgEl, candidate) {
    if (!imgEl) return;
    if (!candidate || badImagePaths.has(candidate)) {
        imgEl.src = DEFAULT_IMAGE;
        return;
    }
    // Wenn bereits als gut bekannt -> direkt setzen ohne Preload
    if (goodImagePaths.has(candidate)) {
        if (imgEl.src !== candidate) imgEl.src = candidate;
        return;
    }
    // Falls bereits der gewünschte Kandidat angezeigt wird -> nichts tun
    if (imgEl.src === candidate) return;
    // Preload
    const pre = new Image();
    pre.onload = () => {
        goodImagePaths.add(candidate);
        if (!badImagePaths.has(candidate)) imgEl.src = candidate;
    };
    pre.onerror = () => {
        badImagePaths.add(candidate);
        imgEl.src = DEFAULT_IMAGE;
        console.warn('[IM] image failed, caching as bad:', candidate);
    };
    pre.src = candidate;
}

function renderImDetail(inst) {
    const empty = document.getElementById('im-empty');
    const detail = document.getElementById('im-detail');
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
    header.className = 'im-detail-header';

    const title = document.createElement('div');
    title.className = 'im-detail-title';
    title.textContent = inst.name;

    const chips = document.createElement('div');
    chips.className = 'im-chips';
    const chipLoader = document.createElement('div');
    chipLoader.className = 'im-chip';
    chipLoader.textContent = inst.loader;
    const chipVersion = document.createElement('div');
    chipVersion.className = 'im-chip';
    chipVersion.textContent = inst.version || 'Unknown version';
    chips.appendChild(chipLoader);
    chips.appendChild(chipVersion);
    header.appendChild(title);
    header.appendChild(chips);

    const main = document.createElement('div');
    main.className = 'im-detail-main';

    const iconWrap = document.createElement('div');
    iconWrap.className = 'im-icon-wrap';
    const img = document.createElement('img');
    img.alt = inst.name;
    img.src = DEFAULT_IMAGE; // stabiler Start
    iconWrap.appendChild(img);

    // Stabiles Laden des Kandidaten
    loadImageStable(img, inst.imagePath);

    const fields = document.createElement('div');
    fields.className = 'im-fields';
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
    footer.className = 'im-footer';

    const btnLaunch = document.createElement('button');
    btnLaunch.className = 'im-btn primary';
    btnLaunch.textContent = 'Launch now';
    btnLaunch.onclick = async () => {
        try {
            const params = new URLSearchParams();
            if (inst && inst.name) params.set('profileName', inst.name);
            const url = `http://localhost:8800/launch${params.toString() ? ('?' + params.toString()) : ''}`;
            console.log('[launch][IM][request]', url);
            const resp = await fetch(url);
            const data = await resp.json().catch(() => ({}));
            console.log('[launch][IM][response]', data);
            alert(data.success ? `Launching ${inst.name}` : (data.error || 'Launch failed'));
        } catch (e) {
            console.error('Launch from Instance Manager failed:', e);
            alert('Launch failed');
        }
    };

    const btnEdit = document.createElement('button');
    btnEdit.className = 'im-btn';
    btnEdit.textContent = 'Edit';
    btnEdit.disabled = false;
    btnEdit.onclick = () => openEditMenu(inst);

    // Neuer Button: Mods anzeigen
    const btnMods = document.createElement('button');
    btnMods.className = 'im-btn';
    btnMods.textContent = 'Mods';
    btnMods.onclick = () => openModManager(inst);

    footer.appendChild(btnLaunch);
    footer.appendChild(btnEdit);
    footer.appendChild(btnMods);

    detail.appendChild(header);
    detail.appendChild(main);
    detail.appendChild(footer);
}

async function loadInstances() {
    try {
        const res = await fetch('http://localhost:8800/instances');
        const raw = await res.json();
        const arr = Array.isArray(raw) ? raw : (raw.instances || []);
        imInstancesCache = normalizeInstances(arr);
        // Do not auto-select the first instance when the manager opens.
        imActiveIndex = -1;
        renderImList(imInstancesCache);
        // Show empty/detail state with no selection
        renderImDetail(null);
    } catch (e) {
        console.error('Failed to load instances in instance-manager view:', e);
        imInstancesCache = [];
        imActiveIndex = -1;
        renderImList([]);
        renderImDetail(null);
    }
}

window.addEventListener('DOMContentLoaded', () => {
    loadInstances();
});

function openEditMenu(instance) {
    // If no instance provided, just open the edit page (create flow)
    if (!instance || !instance.name) {
        window.location.href = '/electron/sites/edit-instance.html';
        return;
    }

    // Navigate to the edit page with the profile name as query param.
    const params = new URLSearchParams();
    params.set('profileName', instance.name);
    window.location.href = `edit-instance.html?${params.toString()}`;
}

function openModManager(instance) {
    const params = new URLSearchParams();
    if (instance && instance.name) params.set('profileName', instance.name);
    window.location.href = `mod-manager.html?${params.toString()}`;
}