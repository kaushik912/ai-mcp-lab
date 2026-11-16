// "Saved" view: topic filter + practice cards (answer hidden until Reveal) + remove.
import { $, el, optionRow, markAnswer, scenarioBlock } from './dom.js';
import * as store from './favorites-store.js';
import { setMessage } from './notices.js';

const viewEl = $('view-saved');
const listEl = $('saved-list');
const filterEl = $('topic-filter');

function fillFilter(items) {
  const current = filterEl.value;
  const topics = [...new Set(items.map((f) => f.topic))].sort();
  filterEl.textContent = '';
  const all = el('option', null, 'All topics');
  all.value = '';
  filterEl.appendChild(all);
  topics.forEach((t) => {
    const o = el('option', null, t);
    o.value = t;
    filterEl.appendChild(o);
  });
  filterEl.value = topics.includes(current) ? current : '';
}

export function renderSaved() {
  const items = store.all();
  fillFilter(items);
  listEl.textContent = '';
  const shown = items.filter((f) => !filterEl.value || f.topic === filterEl.value);
  if (shown.length === 0) {
    listEl.appendChild(el('p', 'hint', items.length === 0
      ? 'No favorites yet. Tap ☆ on a question to save it.'
      : 'No favorites for this topic.'));
    return;
  }
  shown.forEach((f) => listEl.appendChild(savedCard(f)));
}

function savedCard(f) {
  const box = el('div', 'q');
  const head = el('div', 'qhead');
  head.appendChild(el('div', 'meta', f.topic + ' · ' + f.difficulty.toLowerCase() + ' · ' + f.kind.toLowerCase()));
  const remove = el('button', 'small danger', 'Remove');
  remove.type = 'button';
  remove.addEventListener('click', () => {
    store.remove(f.id); // store notifies -> view re-renders
    setMessage('Removed from favorites.');
  });
  head.appendChild(remove);
  box.appendChild(head);
  if (f.scenario) box.appendChild(scenarioBlock(f.scenario));
  box.appendChild(el('div', 'text', f.question));

  const name = 'f' + f.id;
  f.options.forEach((opt, j) => box.appendChild(optionRow(name, j, opt)));

  const reveal = el('button', 'small', 'Reveal answer');
  reveal.type = 'button';
  reveal.addEventListener('click', () => {
    const picked = box.querySelector('input[name="' + name + '"]:checked');
    const pickedIdx = picked ? Number(picked.value) : -1;
    markAnswer(box, f.correctIndex, pickedIdx);
    const verdict = pickedIdx === f.correctIndex ? 'Correct. ' : (pickedIdx < 0 ? '' : 'Incorrect. ');
    box.appendChild(el('div', 'explain', verdict + f.explanation));
    reveal.remove();
  });
  box.appendChild(reveal);
  return box;
}

export function initSavedView() {
  filterEl.addEventListener('change', renderSaved);
  // Re-render on store changes (remove, import) only while this view is showing.
  store.subscribe(() => { if (!viewEl.hidden) renderSaved(); });
}
