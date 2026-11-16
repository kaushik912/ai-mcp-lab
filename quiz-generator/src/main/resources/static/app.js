// Entry point: wires the views together and owns the tab switching.
// Loaded as an ES module (<script type="module">); no libraries, no CDN.
import { $ } from './js/dom.js';
import * as store from './js/favorites-store.js';
import { setMessage, setStorageNotice } from './js/notices.js';
import { initQuizView } from './js/quiz-view.js';
import { initSavedView, renderSaved } from './js/saved-view.js';
import { initFavoritesIo } from './js/favorites-io.js';
import { initTopics } from './js/topics.js';

const viewNew = $('view-new');
const viewSaved = $('view-saved');
const tabNew = $('tab-new');
const tabSaved = $('tab-saved');

function showTab(name) {
  const saved = name === 'saved';
  viewNew.hidden = saved;
  viewSaved.hidden = !saved;
  tabNew.setAttribute('aria-selected', String(!saved));
  tabSaved.setAttribute('aria-selected', String(saved));
  setMessage('');
  if (saved) renderSaved();
}

tabNew.addEventListener('click', () => showTab('new'));
tabSaved.addEventListener('click', () => showTab('saved'));

initQuizView();
initSavedView();
initFavoritesIo();
initTopics(); // loads topics.json (async); falls back to free text if it fails

// Header bits that depend on store state.
store.subscribe(() => {
  tabSaved.textContent = 'Saved (' + store.count() + ')';
  setStorageNotice(store.isPersistent());
});

store.init(); // last: loads from localStorage and notifies all subscribers
