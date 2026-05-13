const LS_JAVA = "exercises.reach.javaUrl";
const LS_PYTHON = "exercises.reach.pythonUrl";
const LS_RUST = "exercises.reach.rustUrl";

function buildDefaultJava(): string {
  const env = import.meta.env;
  return (
    (env.VITE_EXERCISES_JAVA_EMBED_URL || "").trim() ||
    (env.VITE_EXERCISES_EMBED_URL || "").trim() ||
    "http://127.0.0.1:8080/"
  );
}

function buildDefaultPython(): string {
  return (import.meta.env.VITE_EXERCISES_PYTHON_EMBED_URL || "").trim() || "http://127.0.0.1:5000/";
}

function buildDefaultRust(): string {
  return (import.meta.env.VITE_EXERCISES_RUST_EMBED_URL || "").trim() || "http://127.0.0.1:8082/";
}

function loadOrDefault(key: string, fallback: string): string {
  try {
    const v = localStorage.getItem(key);
    if (v != null && v.trim() !== "") return v.trim();
  } catch {
    /* ignore */
  }
  return fallback;
}

async function probeGet(url: string): Promise<{ ok: boolean; detail: string }> {
  const u = url.trim();
  if (!u) return { ok: false, detail: "Empty URL" };
  try {
    const res = await fetch(u, { method: "GET", mode: "cors", cache: "no-store" });
    if (res.ok) return { ok: true, detail: `HTTP ${res.status}` };
    return { ok: false, detail: `HTTP ${res.status} ${res.statusText}`.trim() };
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    return {
      ok: false,
      detail: `${msg} — often CORS (app must allow GET from this page’s origin) or the server is down.`,
    };
  }
}

function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  props: Record<string, string> = {},
  children: (string | Node)[] = [],
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (k === "className") node.className = v;
    else if (k === "htmlFor" && "htmlFor" in node) (node as HTMLLabelElement).htmlFor = v;
    else node.setAttribute(k, v);
  }
  for (const c of children) node.append(typeof c === "string" ? document.createTextNode(c) : c);
  return node;
}

const app = document.getElementById("app");
if (!app) throw new Error("#app missing");

const intro = el("p", {}, [
  "Probes each stack with a browser ",
  el("code", {}, ["GET"]),
  " (CORS). URLs come from this form, then ",
  el("code", {}, ["localStorage"]),
  "; if empty, from build-time ",
  el("code", {}, ["VITE_EXERCISES_JAVA_EMBED_URL"]),
  ", ",
  el("code", {}, ["VITE_EXERCISES_PYTHON_EMBED_URL"]),
  ", ",
  el("code", {}, ["VITE_EXERCISES_RUST_EMBED_URL"]),
  " (Java may use legacy ",
  el("code", {}, ["VITE_EXERCISES_EMBED_URL"]),
  "), then usual localhost defaults from ",
  el("code", {}, ["../DOCKER.md"]),
  ".",
]);

const javaInput = el("input", {
  type: "url",
  id: "url-java",
  className: "url",
  placeholder: buildDefaultJava(),
  value: loadOrDefault(LS_JAVA, buildDefaultJava()),
  autocomplete: "off",
  spellcheck: "false",
}) as HTMLInputElement;

const pyInput = el("input", {
  type: "url",
  id: "url-python",
  className: "url",
  placeholder: buildDefaultPython(),
  value: loadOrDefault(LS_PYTHON, buildDefaultPython()),
  autocomplete: "off",
  spellcheck: "false",
}) as HTMLInputElement;

const rustInput = el("input", {
  type: "url",
  id: "url-rust",
  className: "url",
  placeholder: buildDefaultRust(),
  value: loadOrDefault(LS_RUST, buildDefaultRust()),
  autocomplete: "off",
  spellcheck: "false",
}) as HTMLInputElement;

const javaStatus = el("pre", { className: "status", id: "st-java" }, ["—"]);
const pyStatus = el("pre", { className: "status", id: "st-python" }, ["—"]);
const rustStatus = el("pre", { className: "status", id: "st-rust" }, ["—"]);

function row(label: string, input: HTMLInputElement, status: HTMLElement) {
  return el(
    "div",
    { className: "row" },
    [
      el("label", { htmlFor: input.id }, [label]),
      input,
      el("div", { className: "status-wrap" }, [status]),
    ],
  );
}

const saveBtn = el("button", { type: "button", id: "save" }, ["Save URLs"]);
const refreshBtn = el("button", { type: "button", id: "refresh" }, ["Refresh probes"]);

app.append(
  el("h1", {}, ["Exercises stack reach"]),
  intro,
  el("section", { className: "grid" }, [
    row("Java (Spring)", javaInput, javaStatus),
    row("Python (Flask)", pyInput, pyStatus),
    row("Rust (Axum)", rustInput, rustStatus),
  ]),
  el("div", { className: "actions" }, [saveBtn, refreshBtn]),
);

function persistFromInputs() {
  localStorage.setItem(LS_JAVA, javaInput.value.trim() || buildDefaultJava());
  localStorage.setItem(LS_PYTHON, pyInput.value.trim() || buildDefaultPython());
  localStorage.setItem(LS_RUST, rustInput.value.trim() || buildDefaultRust());
}

function effectiveUrl(input: HTMLInputElement, fallback: () => string): string {
  return input.value.trim() || fallback();
}

async function runProbes() {
  javaStatus.textContent = "…";
  pyStatus.textContent = "…";
  rustStatus.textContent = "…";

  const [j, p, r] = await Promise.all([
    probeGet(effectiveUrl(javaInput, buildDefaultJava)),
    probeGet(effectiveUrl(pyInput, buildDefaultPython)),
    probeGet(effectiveUrl(rustInput, buildDefaultRust)),
  ]);

  javaStatus.textContent = j.ok ? `OK — ${j.detail}` : `Fail — ${j.detail}`;
  pyStatus.textContent = p.ok ? `OK — ${p.detail}` : `Fail — ${p.detail}`;
  rustStatus.textContent = r.ok ? `OK — ${r.detail}` : `Fail — ${r.detail}`;
}

saveBtn.addEventListener("click", () => {
  persistFromInputs();
  javaInput.value = localStorage.getItem(LS_JAVA) || javaInput.value;
  pyInput.value = localStorage.getItem(LS_PYTHON) || pyInput.value;
  rustInput.value = localStorage.getItem(LS_RUST) || rustInput.value;
  void runProbes();
});

refreshBtn.addEventListener("click", () => {
  void runProbes();
});

const style = document.createElement("style");
style.textContent = `
  body { font-family: system-ui, sans-serif; max-width: 52rem; margin: 1.5rem auto; padding: 0 1rem; line-height: 1.45; }
  h1 { font-size: 1.25rem; }
  code { font-size: 0.9em; }
  .grid { display: flex; flex-direction: column; gap: 1rem; margin: 1rem 0; }
  .row label { display: block; font-weight: 600; margin-bottom: 0.25rem; }
  .url { width: 100%; box-sizing: border-box; padding: 0.4rem 0.5rem; font: inherit; }
  .status-wrap { margin-top: 0.35rem; }
  .status { margin: 0; white-space: pre-wrap; font-size: 0.85rem; color: #333; }
  .actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }
  button { font: inherit; padding: 0.4rem 0.75rem; cursor: pointer; }
`;
document.head.append(style);

void runProbes();
