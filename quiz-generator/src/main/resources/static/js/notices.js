// The two small banners shown above both views: a transient favorites message and a
// persistent "storage unavailable" notice.
import { $ } from './dom.js';

const msgEl = $('fav-msg');
const noticeEl = $('fav-notice');

export function setMessage(text, isError) {
  msgEl.textContent = text;
  msgEl.className = isError ? 'error' : 'hint';
}

export function setStorageNotice(persistOk) {
  noticeEl.textContent = persistOk ? '' :
    'Browser storage unavailable: favorites will not persist after you leave. Use Export to keep them.';
}
