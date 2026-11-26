const log = document.getElementById("log");
const input = document.getElementById("q");
const keyInput = document.getElementById("key"); // BYOK: read per request, never stored

function add(cls, text) {
  const d = document.createElement("div");
  d.className = "msg " + cls;
  d.textContent = text;
  log.appendChild(d);
  return d;
}

async function send() {
  const msg = input.value.trim();
  if (!msg) return;
  const key = keyInput.value.trim();
  if (!key) {
    keyInput.focus();
    add("bot", "Please paste your OpenRouter API key first.");
    return;
  }
  add("user", msg);
  input.value = "";
  const thinking = add("bot", "…");
  try {
    const r = await fetch("/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-OpenRouter-Key": key },
      body: JSON.stringify({ message: msg }),
    });
    const data = await r.json();
    thinking.textContent = data.answer;
    if (data.steps && data.steps.length) {
      add(
        "steps",
        data.steps
          .map((s) => `🔧 ${s.tool}(${JSON.stringify(s.args)}) -> ${s.result}`)
          .join("\n")
      );
    }
  } catch (e) {
    thinking.textContent = "Error: " + e;
  }
}

document.getElementById("ask").addEventListener("click", send);
input.addEventListener("keydown", (e) => {
  if (e.key === "Enter") send();
});