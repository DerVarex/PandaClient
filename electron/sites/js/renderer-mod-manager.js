console.log('renderer-mod-manager.js loaded');

function normalizeInstance(raw) {
  if (!raw || typeof raw !== 'object') return null;
  const loaderRaw = raw.loader || raw.modloader || '';
  const loaderPretty = loaderRaw ? loaderRaw.charAt(0) + loaderRaw.slice(1).toLowerCase() : '';
  return {
    original: raw,
    name: raw.profileName || raw.name || raw.title || 'Instance',
    version: raw.versionId || raw.version || raw.minecraftVersion || '',
    loader: loaderPretty || 'Unknown'
  };
}
function normalizeInstances(arr) {
  if (!Array.isArray(arr)) return [];
  return arr.map(normalizeInstance).filter(Boolean);
}

let mmInstancesCache = [];
let mmActiveIndex = -1;

async function loadInstancesForMM() {
  try {
    const res = await fetch('http://localhost:8800/instances');
    const raw = await res.json();
    const arr = Array.isArray(raw) ? raw : (raw.instances || []);
    mmInstancesCache = normalizeInstances(arr);
    renderMmList(mmInstancesCache);
    renderMmDetail(null);
  } catch (e) {
    console.error('[MM] Failed to load instances:', e);
    mmInstancesCache = [];
    renderMmList([]);
    renderMmDetail(null);
  }
}

function renderMmList(instances) {
  const listEl = document.getElementById('mm-instance-list');
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
    item.className = 'mm-instance-item' + (idx === mmActiveIndex ? ' active' : '');

    const nameSpan = document.createElement('div');
    nameSpan.className = 'mm-instance-name';
    nameSpan.textContent = inst.name;

    const subSpan = document.createElement('div');
    subSpan.className = 'mm-instance-sub';
    subSpan.textContent = `${inst.loader} ${inst.version}`.trim();

    item.appendChild(nameSpan);
    item.appendChild(subSpan);

    item.onclick = () => {
      mmActiveIndex = idx;
      renderMmList(mmInstancesCache);
      openModsForInstance(inst);
    };

    listEl.appendChild(item);
  });
}

async function openModsForInstance(inst) {
  if (!inst || !inst.name) {
    renderMmDetail(null);
    return;
  }
  try {
    const resp = await fetch('http://localhost:8800/mods', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ profileName: String(inst.name) })
    });
    const data = await resp.json().catch(() => ({}));
    const mods = Array.isArray(data.mods) ? data.mods : [];
    renderMmDetail({ inst, mods });
  } catch (e) {
    console.error('[MM] Fetch mods failed:', e);
    renderMmDetail({ inst, mods: [] });
  }
}

function renderMmDetail(payload) {
  const empty = document.getElementById('mm-empty');
  const detail = document.getElementById('mm-detail');
  if (!empty || !detail) return;

  if (!payload) {
    empty.classList.remove('hidden');
    detail.classList.add('hidden');
    detail.querySelector('#mm-mod-list') && (detail.querySelector('#mm-mod-list').innerHTML = '');
    return;
  }

  const { inst, mods } = payload;
  empty.classList.add('hidden');
  detail.classList.remove('hidden');

  const titleEl = document.getElementById('mm-detail-title');
  const chips = document.getElementById('mm-chips');
  const list = document.getElementById('mm-mod-list');

  if (titleEl) titleEl.textContent = inst.name;
  if (chips) {
    chips.innerHTML = '';
    const chipL = document.createElement('div'); chipL.className = 'mm-chip'; chipL.textContent = inst.loader; chips.appendChild(chipL);
    const chipV = document.createElement('div'); chipV.className = 'mm-chip'; chipV.textContent = inst.version || 'Unknown'; chips.appendChild(chipV);
  }
  if (list) {
    list.innerHTML = '';
    if (!mods.length) {
      const d = document.createElement('div');
      d.textContent = 'No mods found in mods folder.';
      d.style.color = '#6b7280';
      d.style.fontSize = '13px';
      d.style.padding = '8px 4px';
      list.appendChild(d);
    } else {
      mods.forEach(m => {
        const row = document.createElement('div'); row.className = 'mm-mod-item';
        const name = document.createElement('div'); name.className = 'mm-mod-name'; name.textContent = m;
        const actions = document.createElement('div'); actions.className = 'mm-mod-actions';
        const btn = document.createElement('button'); btn.textContent = '🗑️';
        btn.title = 'Delete mod';
        btn.onclick = () => deleteMod(inst.name, m);
        actions.appendChild(btn);
        row.appendChild(name);
        row.appendChild(actions);
        list.appendChild(row);
      });
    }
  }
}

async function deleteMod(profileName, modName) {
  if (!profileName || !modName) return;
  const sure = confirm(`Delete mod "${modName}" from instance "${profileName}"?`);
  if (!sure) return;
  try {
    const resp = await fetch('http://localhost:8800/deleteMod', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ profileName: String(profileName), mod: String(modName) })
    });
    const data = await resp.json().catch(() => ({}));
    if (data.success) {
      const inst = mmInstancesCache.find(i => i.name === profileName);
      if (inst) openModsForInstance(inst);
      alert(`Deleted mod: ${modName}`);
    } else {
      alert(`Could not delete mod: ${data.error || 'Unknown error'}`);
    }
  } catch (e) {
    console.error('[MM] deleteMod failed:', e);
    alert('Delete failed');
  }
}

async function mmDoSearch() {
  const qEl = document.getElementById('mm-search-query');
  const cEl = document.getElementById('mm-search-category');
  const resEl = document.getElementById('mm-search-results');
  if (!qEl || !resEl) return;
  const query = qEl.value.trim();
  const category = cEl ? cEl.value.trim() : '';
  if (!query) {
    resEl.innerHTML = '<div class="mm-search-empty">Enter a search query.</div>';
    return;
  }
  resEl.innerHTML = '<div class="mm-search-loading">Searching...</div>';
  try {
    const payload = { query: query, limit: 10 };
    if (category) payload.category = category;
    // include game version filter if instance selected
    if (mmActiveIndex >= 0 && mmInstancesCache[mmActiveIndex] && mmInstancesCache[mmActiveIndex].version) {
      payload.gameVersion = mmInstancesCache[mmActiveIndex].version;
    }
    const resp = await fetch('http://localhost:8800/modrinth/search', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
    });
    const data = await resp.json().catch(() => ({}));
    if (!data.success) {
      resEl.innerHTML = '<div class="mm-search-error">Search failed: ' + (data.error || 'Unknown') + '</div>';
      return;
    }
    const results = Array.isArray(data.results) ? data.results : [];
    if (!results.length) {
      resEl.innerHTML = '<div class="mm-search-empty">No results.</div>';
      return;
    }
    resEl.innerHTML = '';
    results.forEach(r => {
      const row = document.createElement('div'); row.className = 'mm-result-item';
      const title = document.createElement('div'); title.className = 'mm-result-title'; title.textContent = r.title || r.slug || r.id;
      const desc = document.createElement('div'); desc.className = 'mm-result-desc'; desc.textContent = (r.description || '').substring(0, 140);
      const actions = document.createElement('div'); actions.className = 'mm-result-actions';
      const addBtn = document.createElement('button'); addBtn.textContent = 'Add'; addBtn.onclick = () => mmDownloadMod(r);
      actions.appendChild(addBtn);
      row.appendChild(title); row.appendChild(desc); row.appendChild(actions);
      resEl.appendChild(row);
    });
  } catch (e) {
    console.error('[MM] search error', e);
    resEl.innerHTML = '<div class="mm-search-error">Error: ' + e.message + '</div>';
  }
}

function mmMapLoader(loader) {
  const l = (loader||'').toLowerCase();
  if (['fabric','forge','quilt','neoforge'].includes(l)) return l;
  return ''; // vanilla or unknown -> no loader filter
}

async function mmDownloadMod(result) {
  if (!result) {
    alert('No result payload');
    return;
  }
  if (mmActiveIndex < 0 || !mmInstancesCache[mmActiveIndex]) {
    alert('Select an instance first');
    return;
  }
  const inst = mmInstancesCache[mmActiveIndex];
  const projectRef = result.slug || result.id;
  if (!projectRef) {
    alert('Result has no slug/id');
    return;
  }
  const sure = confirm(`Download compatible mod version for "${result.title || result.slug}" to instance "${inst.name}"?`);
  if (!sure) return;
  try {
    const body = {
      profileName: inst.name,
      project: projectRef
    };
    if (inst.version) body.gameVersion = inst.version;
    const loaderMapped = mmMapLoader(inst.loader);
    if (loaderMapped) body.loader = loaderMapped;
    const resp = await fetch('http://localhost:8800/modrinth/download', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    });
    const data = await resp.json().catch(()=>({}));
    if (data.success) {
      alert('Downloaded: ' + data.file);
      openModsForInstance(inst);
    } else {
      alert('Download failed: ' + (data.error || 'Unknown error'));
    }
  } catch (e) {
    console.error('[MM] download error', e);
    alert('Download error: ' + e.message);
  }
}

window.addEventListener('DOMContentLoaded', () => {
  loadInstancesForMM();
});
