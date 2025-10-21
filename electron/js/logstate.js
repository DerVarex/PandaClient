// Frontend log view for PandaClient
(function() {
  const MAX_LINES = 500;

  function getContainer() {
    const box = document.getElementById('logstate');
    if (box) box.style.display = 'block';
    let list = document.getElementById('logstate-list');
    if (!list) {
      // fallback: create if missing
      const l = document.createElement('div');
      l.id = 'logstate-list';
      l.className = 'logstate-text';
      if (box) box.appendChild(l);
      list = l;
    }
    return list;
  }

  function formatTime(ts) {
    try {
      const d = ts ? new Date(ts) : new Date();
      return d.toTimeString().split(' ')[0];
    } catch (_) {
      const d = new Date();
      return d.toTimeString().split(' ')[0];
    }
  }

  function appendLogLine(level, where, message, timestamp) {
    const list = getContainer();
    if (!list) return;

    const line = document.createElement('div');
    line.className = 'logline';

    const lvl = (level || 'INFO').toUpperCase();
    line.classList.add(
      lvl === 'ERROR' ? 'level-error' : (lvl === 'WARN' || lvl === 'WARNING') ? 'level-warn' : 'level-info'
    );

    const time = formatTime(timestamp);
    const whereStr = where ? ` ${where}` : '';
    line.textContent = `[${time}] [${lvl}]${whereStr}: ${message ?? ''}`;

    list.appendChild(line);

    // trim to max lines
    while (list.childElementCount > MAX_LINES) {
      list.removeChild(list.firstChild);
    }

    // auto scroll
    list.scrollTop = list.scrollHeight;
  }

  function clearLog() {
    const list = document.getElementById('logstate-list');
    if (list) list.innerHTML = '';
  }

  function hideLog() {
    const box = document.getElementById('logstate');
    if (box) box.style.display = 'none';
  }

  function showLog() {
    const box = document.getElementById('logstate');
    if (box) box.style.display = 'block';
  }

  // expose globally for websocket script
  window.appendLogLine = appendLogLine;
  window.clearLog = clearLog;
  window.hideLog = hideLog;
  window.showLog = showLog;
})();
