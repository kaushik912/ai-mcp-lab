// Topic picker: umbrella labels from topics.json ({label, hint}) + "Other (type your own)".
// The label is shown and saved with favorites; the hint is sent as `scope` to enrich the prompt.
import { $, el } from './dom.js';

const OTHER = '__other__';
const selectEl = $('topic-select');
const customEl = $('topic');
let topics = [];

function valid(t) {
  return t && typeof t.label === 'string' && t.label.trim() !== '' && t.label.length <= 100
    && typeof t.hint === 'string' && t.hint.length <= 400;
}

function syncCustom() {
  const other = selectEl.value === OTHER;
  customEl.hidden = !other;
  customEl.required = other;
}

export async function initTopics() {
  try {
    const res = await fetch('topics.json');
    const data = await res.json();
    if (res.ok && Array.isArray(data)) topics = data.filter(valid);
  } catch (e) {
    topics = []; // fall back to free text only
  }
  selectEl.textContent = '';
  topics.forEach((t, i) => {
    const o = el('option', null, t.label);
    o.value = String(i);
    selectEl.appendChild(o);
  });
  const other = el('option', null, 'Other (type your own)');
  other.value = OTHER;
  selectEl.appendChild(other);
  if (topics.length === 0) selectEl.value = OTHER;
  selectEl.addEventListener('change', syncCustom);
  syncCustom();
}

// {topic, scope}: curated pick sends label + hint; free text sends the typed topic only.
export function getSelection() {
  if (topics.length === 0 || selectEl.value === OTHER) {
    return { topic: customEl.value.trim(), scope: null };
  }
  const t = topics[Number(selectEl.value)];
  return { topic: t.label, scope: t.hint };
}
