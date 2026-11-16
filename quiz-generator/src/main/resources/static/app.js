// Plain JS, no libraries. All model/imported text is rendered with textContent (never innerHTML).
(function () {
  // ---------- tiny helpers ----------
  function $(id) { return document.getElementById(id); }

  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text !== undefined) e.textContent = text;
    return e;
  }

  // ---------- favorites (localStorage + export/import) ----------
  var FAV_KEY = 'quiz.favorites.v1';
  var FAV_MAX = 200;
  var IMPORT_MAX_BYTES = 1024 * 1024;
  var DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

  var favorites = [];       // in-memory source of truth
  var persistOk = true;     // false when localStorage is blocked/unavailable

  // cyrb53 string hash -> short base36 id
  function hash(str) {
    var h1 = 0xdeadbeef, h2 = 0x41c6ce57;
    for (var i = 0; i < str.length; i++) {
      var ch = str.charCodeAt(i);
      h1 = Math.imul(h1 ^ ch, 2654435761);
      h2 = Math.imul(h2 ^ ch, 1597334677);
    }
    h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
    h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
    return (4294967296 * (2097151 & h2) + (h1 >>> 0)).toString(36);
  }

  // Same question text + same option set => same id, regardless of option order.
  function idOf(q) {
    return hash(q.question + '\u0001' + q.options.slice().sort().join('\u0001'));
  }

  function isStr(v, max) { return typeof v === 'string' && v.trim().length > 0 && v.length <= max; }

  // Returns a clean favorite item or null. Used for stored data and (untrusted) imports.
  function clean(raw) {
    if (!raw || typeof raw !== 'object') return null;
    if (raw.kind !== 'CONCEPT' && raw.kind !== 'SCENARIO') return null;
    if (DIFFICULTIES.indexOf(raw.difficulty) < 0) return null;
    if (!isStr(raw.topic, 100) || !isStr(raw.question, 350) || !isStr(raw.explanation, 900)) return null;
    if (!Array.isArray(raw.options) || raw.options.length !== 4) return null;
    for (var i = 0; i < 4; i++) { if (!isStr(raw.options[i], 250)) return null; }
    if (new Set(raw.options.map(function (o) { return o.trim().toLowerCase(); })).size !== 4) return null;
    if (!Number.isInteger(raw.correctIndex) || raw.correctIndex < 0 || raw.correctIndex > 3) return null;
    var scenario = null;
    if (raw.kind === 'SCENARIO') {
      if (!isStr(raw.scenario, 900)) return null;
      scenario = raw.scenario;
    }
    var item = {
      topic: raw.topic,
      difficulty: raw.difficulty,
      kind: raw.kind,
      scenario: scenario,
      question: raw.question,
      options: raw.options.slice(),
      correctIndex: raw.correctIndex,
      explanation: raw.explanation,
      savedAt: typeof raw.savedAt === 'number' && isFinite(raw.savedAt) ? raw.savedAt : Date.now()
    };
    item.id = idOf(item);   // never trust an imported id
    return item;
  }

  function loadFavorites() {
    try {
      var text = window.localStorage.getItem(FAV_KEY);
      if (!text) return [];
      var data = JSON.parse(text);
      if (!Array.isArray(data)) return [];   // corrupt -> treat as empty; not overwritten until next save
      var out = [], seen = {};
      data.forEach(function (raw) {
        var item = clean(raw);
        if (item && !seen[item.id] && out.length < FAV_MAX) { seen[item.id] = true; out.push(item); }
      });
      return out;
    } catch (e) {
      persistOk = false;
      return [];
    }
  }

  function persistFavorites() {
    try {
      window.localStorage.setItem(FAV_KEY, JSON.stringify(favorites));
      persistOk = true;
    } catch (e) {
      persistOk = false;
    }
    updateFavNotice();
  }

  function findFavorite(id) {
    for (var i = 0; i < favorites.length; i++) { if (favorites[i].id === id) return i; }
    return -1;
  }

  // ---------- elements / state ----------
  var form = $('form');
  var statusEl = $('status');
  var quizEl = $('quiz');
  var questionsEl = $('questions');
  var scoreEl = $('score');
  var generateBtn = $('generate');
  var submitBtn = $('submit');
  var viewNew = $('view-new');
  var viewSaved = $('view-saved');
  var tabNew = $('tab-new');
  var tabSaved = $('tab-saved');
  var favNotice = $('fav-notice');
  var favMsg = $('fav-msg');
  var savedList = $('saved-list');
  var topicFilter = $('topic-filter');
  var quiz = null;
  var quizDifficulty = 'MEDIUM';

  function setStatus(msg, isError) {
    statusEl.textContent = msg;
    statusEl.className = isError ? 'error' : '';
  }

  function setFavMsg(msg, isError) {
    favMsg.textContent = msg;
    favMsg.className = isError ? 'error' : 'hint';
  }

  function updateFavNotice() {
    favNotice.textContent = persistOk ? '' :
      'Browser storage unavailable: favorites will not persist after you leave. Use Export to keep them.';
    tabSaved.textContent = 'Saved (' + favorites.length + ')';
  }

  // ---------- tabs ----------
  function showTab(name) {
    var saved = name === 'saved';
    viewNew.hidden = saved;
    viewSaved.hidden = !saved;
    tabNew.setAttribute('aria-selected', String(!saved));
    tabSaved.setAttribute('aria-selected', String(saved));
    setFavMsg('');
    if (saved) renderSaved(); else refreshStars();
  }
  tabNew.addEventListener('click', function () { showTab('new'); });
  tabSaved.addEventListener('click', function () { showTab('saved'); });

  // ---------- generate ----------
  form.addEventListener('submit', function (ev) {
    ev.preventDefault();
    generateBtn.disabled = true;
    quizEl.hidden = true;
    quizDifficulty = $('difficulty').value;
    setStatus('Generating... this can take 10-30 seconds.');
    fetch('/api/quiz', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-OpenRouter-Key': $('key').value
      },
      body: JSON.stringify({
        topic: $('topic').value,
        difficulty: quizDifficulty
      })
    }).then(function (res) {
      return res.json().catch(function () { return {}; }).then(function (body) {
        if (!res.ok) throw new Error(body.error || ('Request failed (' + res.status + ')'));
        return body;
      });
    }).then(function (data) {
      quiz = data;
      render();
      setStatus('');
    }).catch(function (err) {
      setStatus(err.message, true);
    }).then(function () {
      generateBtn.disabled = false;
    });
  });

  // ---------- quiz view ----------
  function starButton(q) {
    var btn = el('button', 'star');
    btn.type = 'button';
    btn.dataset.id = idOf(q);
    btn.addEventListener('click', function () { toggleFavorite(q, btn); });
    setStar(btn, findFavorite(btn.dataset.id) >= 0);
    return btn;
  }

  function setStar(btn, on) {
    btn.textContent = on ? '★' : '☆';
    btn.setAttribute('aria-pressed', String(on));
    btn.setAttribute('aria-label', on ? 'Remove from favorites' : 'Save to favorites');
  }

  function refreshStars() {
    questionsEl.querySelectorAll('button.star').forEach(function (btn) {
      setStar(btn, findFavorite(btn.dataset.id) >= 0);
    });
  }

  function toggleFavorite(q, btn) {
    var id = idOf(q);
    var at = findFavorite(id);
    if (at >= 0) {
      favorites.splice(at, 1);
      setFavMsg('Removed from favorites.');
    } else {
      if (favorites.length >= FAV_MAX) {
        setFavMsg('Favorites full (' + FAV_MAX + '). Remove some in the Saved tab first.', true);
        return;
      }
      var item = clean({
        topic: quiz.topic, difficulty: quizDifficulty, kind: q.kind, scenario: q.scenario,
        question: q.question, options: q.options, correctIndex: q.correctIndex,
        explanation: q.explanation, savedAt: Date.now()
      });
      if (!item) { setFavMsg('This question could not be saved (unexpected format).', true); return; }
      favorites.unshift(item);
      setFavMsg('Saved to favorites.');
    }
    persistFavorites();
    setStar(btn, findFavorite(id) >= 0);
  }

  function render() {
    questionsEl.textContent = '';
    scoreEl.textContent = '';
    submitBtn.disabled = false;
    quiz.questions.forEach(function (q, i) {
      var box = el('div', 'q');
      var head = el('div', 'qhead');
      head.appendChild(el('div', 'meta', 'Q' + (i + 1) + ' · ' + q.kind.toLowerCase()));
      head.appendChild(starButton(q));
      box.appendChild(head);
      if (q.scenario) box.appendChild(el('div', 'scenario', q.scenario));
      box.appendChild(el('div', 'text', q.question));
      q.options.forEach(function (opt, j) {
        var label = el('label', 'opt');
        var radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'q' + i;
        radio.value = String(j);
        label.appendChild(radio);
        label.appendChild(el('span', null, opt));
        box.appendChild(label);
      });
      questionsEl.appendChild(box);
    });
    quizEl.hidden = false;
  }

  submitBtn.addEventListener('click', function () {
    var correct = 0;
    var boxes = questionsEl.querySelectorAll('.q');
    quiz.questions.forEach(function (q, i) {
      var picked = questionsEl.querySelector('input[name="q' + i + '"]:checked');
      var pickedIdx = picked ? Number(picked.value) : -1;
      if (pickedIdx === q.correctIndex) correct++;
      var labels = boxes[i].querySelectorAll('.opt');
      labels.forEach(function (label, j) {
        label.querySelector('input').disabled = true;
        if (j === q.correctIndex) label.classList.add('correct');
        else if (j === pickedIdx) label.classList.add('wrong');
      });
      boxes[i].appendChild(el('div', 'explain', (pickedIdx === q.correctIndex ? 'Correct. ' : (pickedIdx < 0 ? 'Skipped. ' : 'Incorrect. ')) + q.explanation));
    });
    scoreEl.textContent = 'Score: ' + correct + ' / ' + quiz.questions.length;
    submitBtn.disabled = true;
    scoreEl.scrollIntoView({ behavior: 'smooth' });
  });

  // ---------- saved view (practice mode: answer hidden until Reveal) ----------
  function renderSaved() {
    var current = topicFilter.value;
    var topics = [];
    favorites.forEach(function (f) { if (topics.indexOf(f.topic) < 0) topics.push(f.topic); });
    topics.sort();
    topicFilter.textContent = '';
    var all = el('option', null, 'All topics');
    all.value = '';
    topicFilter.appendChild(all);
    topics.forEach(function (t) {
      var o = el('option', null, t);
      o.value = t;
      topicFilter.appendChild(o);
    });
    topicFilter.value = topics.indexOf(current) >= 0 ? current : '';

    savedList.textContent = '';
    var shown = favorites.filter(function (f) { return !topicFilter.value || f.topic === topicFilter.value; });
    if (shown.length === 0) {
      savedList.appendChild(el('p', 'hint', favorites.length === 0
        ? 'No favorites yet. Tap ☆ on a question to save it.'
        : 'No favorites for this topic.'));
      return;
    }
    shown.forEach(function (f) { savedList.appendChild(savedCard(f)); });
  }

  function savedCard(f) {
    var box = el('div', 'q');
    var head = el('div', 'qhead');
    head.appendChild(el('div', 'meta', f.topic + ' · ' + f.difficulty.toLowerCase() + ' · ' + f.kind.toLowerCase()));
    var remove = el('button', 'small danger', 'Remove');
    remove.type = 'button';
    remove.addEventListener('click', function () {
      var at = findFavorite(f.id);
      if (at >= 0) favorites.splice(at, 1);
      persistFavorites();
      setFavMsg('Removed from favorites.');
      renderSaved();
    });
    head.appendChild(remove);
    box.appendChild(head);
    if (f.scenario) box.appendChild(el('div', 'scenario', f.scenario));
    box.appendChild(el('div', 'text', f.question));

    var name = 'f' + f.id;
    f.options.forEach(function (opt, j) {
      var label = el('label', 'opt');
      var radio = document.createElement('input');
      radio.type = 'radio';
      radio.name = name;
      radio.value = String(j);
      label.appendChild(radio);
      label.appendChild(el('span', null, opt));
      box.appendChild(label);
    });

    var reveal = el('button', 'small', 'Reveal answer');
    reveal.type = 'button';
    reveal.addEventListener('click', function () {
      var picked = box.querySelector('input[name="' + name + '"]:checked');
      var pickedIdx = picked ? Number(picked.value) : -1;
      box.querySelectorAll('.opt').forEach(function (label, j) {
        label.querySelector('input').disabled = true;
        if (j === f.correctIndex) label.classList.add('correct');
        else if (j === pickedIdx) label.classList.add('wrong');
      });
      box.appendChild(el('div', 'explain', (pickedIdx === f.correctIndex ? 'Correct. ' : (pickedIdx < 0 ? '' : 'Incorrect. ')) + f.explanation));
      reveal.remove();
    });
    box.appendChild(reveal);
    return box;
  }

  topicFilter.addEventListener('change', renderSaved);

  // ---------- export / import ----------
  $('export').addEventListener('click', function () {
    if (favorites.length === 0) { setFavMsg('Nothing to export yet.', true); return; }
    var blob = new Blob([JSON.stringify({ version: 1, items: favorites }, null, 2)], { type: 'application/json' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = 'favorites.json';
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    setFavMsg('Exported ' + favorites.length + ' favorites.');
  });

  var importFile = $('import-file');
  $('import').addEventListener('click', function () { importFile.click(); });
  importFile.addEventListener('change', function () {
    var file = importFile.files && importFile.files[0];
    importFile.value = '';
    if (!file) return;
    if (file.size > IMPORT_MAX_BYTES) { setFavMsg('File too large (max 1 MB).', true); return; }
    file.text().then(function (text) {
      var data;
      try { data = JSON.parse(text); } catch (e) { setFavMsg('Not a valid JSON file.', true); return; }
      var items = Array.isArray(data) ? data : (data && Array.isArray(data.items) ? data.items : null);
      if (!items) { setFavMsg('Unrecognised file format.', true); return; }
      var added = 0, skipped = 0;
      items.forEach(function (raw) {
        var item = clean(raw);
        if (!item || findFavorite(item.id) >= 0 || favorites.length >= FAV_MAX) { skipped++; return; }
        favorites.push(item);
        added++;
      });
      favorites.sort(function (a, b) { return b.savedAt - a.savedAt; });
      persistFavorites();
      renderSaved();
      setFavMsg('Imported ' + added + ', skipped ' + skipped + ' (invalid, duplicate or over the ' + FAV_MAX + ' limit).');
    }).catch(function () { setFavMsg('Could not read the file.', true); });
  });

  // ---------- init ----------
  favorites = loadFavorites();
  updateFavNotice();
})();
