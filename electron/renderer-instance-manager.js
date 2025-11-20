console.log('renderer-instance-manager.js loaded');

// We reuse the normalization from the main renderer for consistency.
function normalizeInstance(raw) {
    if (!raw || typeof raw !== 'object') return null;
    const loaderRaw = raw.loader || raw.modloader || '';
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
    img.src = inst.imagePath || 'images/default.png';
    img.alt = inst.name;
    iconWrap.appendChild(img);

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

//    const btnSelect = document.createElement('button');
//    btnSelect.className = 'im-btn';
//    btnSelect.textContent = 'Use for Launch';
//    btnSelect.onclick = () => {
//        // In standalone view we just show info; integration with main window can be added later via IPC or shared storage.
//        alert(`Selected ${inst.name} as launch target (not yet wired to launcher).`);
//    };

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
    // Assign a click handler — do NOT call openEditMenu immediately during render
    btnEdit.onclick = () => openEditMenu(inst);

//    footer.appendChild(btnSelect);
    footer.appendChild(btnLaunch);
    footer.appendChild(btnEdit);

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
        window.location.href = 'edit-instance.html';
        return;
    }

    // Navigate to the edit page with the profile name as query param.
    const params = new URLSearchParams();
    params.set('profileName', instance.name);
    window.location.href = `edit-instance.html?${params.toString()}`;
}