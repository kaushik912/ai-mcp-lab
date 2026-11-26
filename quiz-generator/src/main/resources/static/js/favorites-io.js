// Export / import of favorites as a JSON file. Import input is untrusted: size-capped here,
// every item validated (and id recomputed) by the store.
import { $ } from './dom.js';
import * as store from './favorites-store.js';
import { setMessage } from './notices.js';

const IMPORT_MAX_BYTES = 1024 * 1024;

function exportFavorites() {
  const items = store.all();
  if (items.length === 0) { setMessage('Nothing to export yet.', true); return; }
  const blob = new Blob([JSON.stringify({ version: 1, items }, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'favorites.json';
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
  setMessage('Exported ' + items.length + ' favorites.');
}

async function importFile(file) {
  if (file.size > IMPORT_MAX_BYTES) { setMessage('File too large (max 1 MB).', true); return; }
  let data;
  try {
    data = JSON.parse(await file.text());
  } catch (e) {
    setMessage('Not a valid JSON file.', true);
    return;
  }
  const items = Array.isArray(data) ? data : (data && Array.isArray(data.items) ? data.items : null);
  if (!items) { setMessage('Unrecognised file format.', true); return; }
  const { added, skipped } = store.importItems(items);
  setMessage('Imported ' + added + ', skipped ' + skipped
    + ' (invalid, duplicate or over the ' + store.FAV_MAX + ' limit).');
}

export function initFavoritesIo() {
  const input = $('import-file');
  $('export').addEventListener('click', exportFavorites);
  $('import').addEventListener('click', () => input.click());
  input.addEventListener('change', () => {
    const file = input.files && input.files[0];
    input.value = '';
    if (file) importFile(file).catch(() => setMessage('Could not read the file.', true));
  });
}
