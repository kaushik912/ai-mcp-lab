// Plain JS, no libraries. All model output is rendered with textContent (never innerHTML).
(function () {
  var form = document.getElementById('form');
  var statusEl = document.getElementById('status');
  var quizEl = document.getElementById('quiz');
  var questionsEl = document.getElementById('questions');
  var scoreEl = document.getElementById('score');
  var generateBtn = document.getElementById('generate');
  var submitBtn = document.getElementById('submit');
  var quiz = null;

  function setStatus(msg, isError) {
    statusEl.textContent = msg;
    statusEl.className = isError ? 'error' : '';
  }

  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text !== undefined) e.textContent = text;
    return e;
  }

  form.addEventListener('submit', function (ev) {
    ev.preventDefault();
    generateBtn.disabled = true;
    quizEl.hidden = true;
    setStatus('Generating... this can take 10-30 seconds.');
    fetch('/api/quiz', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-OpenRouter-Key': document.getElementById('key').value
      },
      body: JSON.stringify({
        topic: document.getElementById('topic').value,
        difficulty: document.getElementById('difficulty').value
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

  function render() {
    questionsEl.textContent = '';
    scoreEl.textContent = '';
    submitBtn.disabled = false;
    quiz.questions.forEach(function (q, i) {
      var box = el('div', 'q');
      box.appendChild(el('div', 'meta', 'Q' + (i + 1) + ' · ' + q.kind.toLowerCase()));
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
})();
