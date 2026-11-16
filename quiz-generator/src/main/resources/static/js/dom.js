// Tiny DOM helpers shared by all views. Text is always set via textContent (never innerHTML).

export function $(id) {
  return document.getElementById(id);
}

export function el(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text !== undefined) e.textContent = text;
  return e;
}

// One option row (radio + label). Shared by the quiz view and the saved view.
export function optionRow(name, index, text) {
  const label = el('label', 'opt');
  const radio = document.createElement('input');
  radio.type = 'radio';
  radio.name = name;
  radio.value = String(index);
  label.appendChild(radio);
  label.appendChild(el('span', null, text));
  return label;
}

// Disable radios and mark the correct option (and the picked one if wrong) inside `box`.
export function markAnswer(box, correctIndex, pickedIndex) {
  box.querySelectorAll('.opt').forEach((label, j) => {
    label.querySelector('input').disabled = true;
    if (j === correctIndex) label.classList.add('correct');
    else if (j === pickedIndex) label.classList.add('wrong');
  });
}
