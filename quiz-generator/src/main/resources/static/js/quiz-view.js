// "New quiz" view: generate form -> API call -> render questions -> grade in the browser.
// Also owns the star buttons on question cards.
import { $, el, optionRow, markAnswer, scenarioBlock } from './dom.js';
import * as store from './favorites-store.js';
import { setMessage } from './notices.js';
import { getSelection } from './topics.js';

const form = $('form');
const statusEl = $('status');
const quizEl = $('quiz');
const questionsEl = $('questions');
const scoreEl = $('score');
const generateBtn = $('generate');
const submitBtn = $('submit');

let quiz = null;
let quizDifficulty = 'MEDIUM';

function setStatus(msg, isError) {
  statusEl.textContent = msg;
  statusEl.className = isError ? 'error' : '';
}

// ---------- generate ----------
async function generate() {
  generateBtn.disabled = true;
  quizEl.hidden = true;
  quizDifficulty = $('difficulty').value;
  const { topic, scope } = getSelection();
  setStatus('Generating... this can take 10-30 seconds.');
  try {
    const res = await fetch('/api/quiz', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-OpenRouter-Key': $('key').value,
      },
      body: JSON.stringify({ topic, scope: scope || undefined, difficulty: quizDifficulty }),
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.error || 'Request failed (' + res.status + ')');
    quiz = body;
    render();
    setStatus('');
  } catch (err) {
    setStatus(err.message, true);
  } finally {
    generateBtn.disabled = false;
  }
}

// ---------- stars ----------
function setStar(btn, on) {
  btn.textContent = on ? '★' : '☆';
  btn.setAttribute('aria-pressed', String(on));
  btn.setAttribute('aria-label', on ? 'Remove from favorites' : 'Save to favorites');
}

function starButton(q) {
  const btn = el('button', 'star');
  btn.type = 'button';
  btn.dataset.id = store.idOf(q);
  btn.addEventListener('click', () => toggleFavorite(q));
  setStar(btn, store.isFavorite(btn.dataset.id));
  return btn;
}

function refreshStars() {
  questionsEl.querySelectorAll('button.star').forEach((btn) => {
    setStar(btn, store.isFavorite(btn.dataset.id));
  });
}

function toggleFavorite(q) {
  const id = store.idOf(q);
  if (store.isFavorite(id)) {
    store.remove(id);
    setMessage('Removed from favorites.');
    return;
  }
  const result = store.saveQuestion(q, quiz.topic, quizDifficulty);
  if (result === 'full') setMessage('Favorites full (' + store.FAV_MAX + '). Remove some in the Saved tab first.', true);
  else if (result === 'invalid') setMessage('This question could not be saved (unexpected format).', true);
  else setMessage('Saved to favorites.');
}

// ---------- render + grade ----------
function render() {
  questionsEl.textContent = '';
  scoreEl.textContent = '';
  submitBtn.disabled = false;
  quiz.questions.forEach((q, i) => {
    const box = el('div', 'q');
    const head = el('div', 'qhead');
    head.appendChild(el('div', 'meta', 'Q' + (i + 1) + ' · ' + q.kind.toLowerCase()));
    head.appendChild(starButton(q));
    box.appendChild(head);
    if (q.scenario) box.appendChild(scenarioBlock(q.scenario));
    box.appendChild(el('div', 'text', q.question));
    q.options.forEach((opt, j) => box.appendChild(optionRow('q' + i, j, opt)));
    questionsEl.appendChild(box);
  });
  quizEl.hidden = false;
}

function submit() {
  let correct = 0;
  const boxes = questionsEl.querySelectorAll('.q');
  quiz.questions.forEach((q, i) => {
    const picked = questionsEl.querySelector('input[name="q' + i + '"]:checked');
    const pickedIdx = picked ? Number(picked.value) : -1;
    if (pickedIdx === q.correctIndex) correct++;
    markAnswer(boxes[i], q.correctIndex, pickedIdx);
    const verdict = pickedIdx === q.correctIndex ? 'Correct. ' : (pickedIdx < 0 ? 'Skipped. ' : 'Incorrect. ');
    boxes[i].appendChild(el('div', 'explain', verdict + q.explanation));
  });
  scoreEl.textContent = 'Score: ' + correct + ' / ' + quiz.questions.length;
  submitBtn.disabled = true;
  scoreEl.scrollIntoView({ behavior: 'smooth' });
}

export function initQuizView() {
  form.addEventListener('submit', (ev) => {
    ev.preventDefault();
    generate();
  });
  submitBtn.addEventListener('click', submit);
  store.subscribe(refreshStars); // keep stars in sync with the store (e.g. removed in Saved tab)
}
