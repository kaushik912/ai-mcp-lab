// Favorites data layer: validation, id hashing, localStorage persistence. No DOM here.
// Views react through subscribe(); every change (save/remove/import/init) notifies once.

export const FAV_MAX = 200;
const FAV_KEY = 'quiz.favorites.v1';
const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

let favorites = [];     // in-memory source of truth
let persistOk = true;   // false when localStorage is blocked/unavailable
const listeners = [];

// cyrb53 string hash -> short base36 id
function hash(str) {
  let h1 = 0xdeadbeef, h2 = 0x41c6ce57;
  for (let i = 0; i < str.length; i++) {
    const ch = str.charCodeAt(i);
    h1 = Math.imul(h1 ^ ch, 2654435761);
    h2 = Math.imul(h2 ^ ch, 1597334677);
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  return (4294967296 * (2097151 & h2) + (h1 >>> 0)).toString(36);
}

// Same question text + same option set => same id, regardless of option order.
export function idOf(q) {
  return hash(q.question + '\u0001' + q.options.slice().sort().join('\u0001'));
}

function isStr(v, max) {
  return typeof v === 'string' && v.trim().length > 0 && v.length <= max;
}

// Returns a clean favorite item or null. Used for stored data and (untrusted) imports.
function clean(raw) {
  if (!raw || typeof raw !== 'object') return null;
  if (raw.kind !== 'CONCEPT' && raw.kind !== 'SCENARIO') return null;
  if (!DIFFICULTIES.includes(raw.difficulty)) return null;
  if (!isStr(raw.topic, 100) || !isStr(raw.question, 350) || !isStr(raw.explanation, 900)) return null;
  if (!Array.isArray(raw.options) || raw.options.length !== 4) return null;
  if (!raw.options.every((o) => isStr(o, 250))) return null;
  if (new Set(raw.options.map((o) => o.trim().toLowerCase())).size !== 4) return null;
  if (!Number.isInteger(raw.correctIndex) || raw.correctIndex < 0 || raw.correctIndex > 3) return null;
  let scenario = null;
  if (raw.kind === 'SCENARIO') {
    if (!isStr(raw.scenario, 900)) return null;
    scenario = raw.scenario;
  }
  const item = {
    topic: raw.topic,
    difficulty: raw.difficulty,
    kind: raw.kind,
    scenario,
    question: raw.question,
    options: raw.options.slice(),
    correctIndex: raw.correctIndex,
    explanation: raw.explanation,
    savedAt: typeof raw.savedAt === 'number' && isFinite(raw.savedAt) ? raw.savedAt : Date.now(),
  };
  item.id = idOf(item); // never trust an imported id
  return item;
}

function load() {
  try {
    const text = window.localStorage.getItem(FAV_KEY);
    if (!text) return [];
    const data = JSON.parse(text);
    if (!Array.isArray(data)) return []; // corrupt -> treat as empty; not overwritten until next save
    const out = [];
    const seen = new Set();
    for (const raw of data) {
      const item = clean(raw);
      if (item && !seen.has(item.id) && out.length < FAV_MAX) {
        seen.add(item.id);
        out.push(item);
      }
    }
    return out;
  } catch (e) {
    persistOk = false;
    return [];
  }
}

function notify() {
  listeners.forEach((fn) => fn());
}

function persist() {
  try {
    window.localStorage.setItem(FAV_KEY, JSON.stringify(favorites));
    persistOk = true;
  } catch (e) {
    persistOk = false;
  }
  notify();
}

// ---------- public API ----------

export function subscribe(fn) { listeners.push(fn); }

export function init() {
  favorites = load();
  notify();
}

export function all() { return favorites.slice(); }
export function count() { return favorites.length; }
export function isPersistent() { return persistOk; }
export function isFavorite(id) { return favorites.some((f) => f.id === id); }

// Returns 'saved' | 'full' | 'invalid'.
export function saveQuestion(q, topic, difficulty) {
  if (favorites.length >= FAV_MAX) return 'full';
  const item = clean({
    topic, difficulty, kind: q.kind, scenario: q.scenario, question: q.question,
    options: q.options, correctIndex: q.correctIndex, explanation: q.explanation, savedAt: Date.now(),
  });
  if (!item) return 'invalid';
  if (!isFavorite(item.id)) {
    favorites.unshift(item);
    persist();
  }
  return 'saved';
}

export function remove(id) {
  const at = favorites.findIndex((f) => f.id === id);
  if (at < 0) return false;
  favorites.splice(at, 1);
  persist();
  return true;
}

// Merge untrusted raw items. Returns {added, skipped}. Skips invalid, duplicate, or over the cap.
export function importItems(rawItems) {
  let added = 0, skipped = 0;
  for (const raw of rawItems) {
    const item = clean(raw);
    if (!item || isFavorite(item.id) || favorites.length >= FAV_MAX) { skipped++; continue; }
    favorites.push(item);
    added++;
  }
  favorites.sort((a, b) => b.savedAt - a.savedAt);
  persist();
  return { added, skipped };
}
