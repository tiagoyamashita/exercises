const LS_JAVA = "exercises.reach.javaUrl";
const LS_PYTHON = "exercises.reach.pythonUrl";
const LS_RUST = "exercises.reach.rustUrl";
const LS_GRAFANA = "exercises.reach.grafanaUrl";
const LS_PROMETHEUS = "exercises.reach.prometheusUrl";
const LS_ELASTICSEARCH = "exercises.reach.elasticsearchUrl";
const LS_KIBANA = "exercises.reach.kibanaUrl";

type StackKey = "rust" | "java" | "python";

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

function buildDefaultGrafana(): string {
  return (import.meta.env.VITE_GRAFANA_EMBED_URL || "").trim() || "http://127.0.0.1:3000/";
}

function buildDefaultPrometheus(): string {
  return (import.meta.env.VITE_PROMETHEUS_EMBED_URL || "").trim() || "http://127.0.0.1:9090/";
}

function buildDefaultElasticsearch(): string {
  return (import.meta.env.VITE_ELASTICSEARCH_EMBED_URL || "").trim() || "http://127.0.0.1:9200/";
}

function buildDefaultKibana(): string {
  return (import.meta.env.VITE_KIBANA_EMBED_URL || "").trim() || "http://127.0.0.1:5601/";
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

const intro = el("p", { className: "intro" }, [
  "Probes each URL with a browser ",
  el("code", {}, ["GET"]),
  " (CORS). Values persist in ",
  el("code", {}, ["localStorage"]),
  " when you click ",
  el("code", {}, ["Save URLs"]),
  ". Empty fields use build-time ",
  el("code", {}, ["VITE_*"]),
  " vars (see ",
  el("code", {}, [".env.example"]),
  "), then localhost defaults from ",
  el("code", {}, ["../DOCKER.md"]),
  ". ",
  el("strong", {}, ["Probe all again"]),
  " re-runs every ",
  el("code", {}, ["GET"]),
  " probe below. ",
  "Embeds use the same app URLs; if a site sends ",
  el("code", {}, ["X-Frame-Options"]),
  " / CSP frame-ancestors, the iframe stays blank — open in a new tab instead.",
]);

function row(
  label: string,
  input: HTMLInputElement,
  status: HTMLElement,
  fallback: () => string,
): HTMLElement {
  const open = el("a", { href: "#", className: "open-tab" }, ["Open"]);
  open.addEventListener("click", (e) => {
    e.preventDefault();
    const u = effectiveUrl(input, fallback);
    if (u) window.open(u, "_blank", "noopener,noreferrer");
  });
  return el(
    "div",
    { className: "row" },
    [
      el("div", { className: "label-row" }, [el("label", { htmlFor: input.id }, [label]), open]),
      input,
      el("div", { className: "status-wrap" }, [status]),
    ],
  );
}

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

const grafanaInput = el("input", {
  type: "url",
  id: "url-grafana",
  className: "url",
  placeholder: buildDefaultGrafana(),
  value: loadOrDefault(LS_GRAFANA, buildDefaultGrafana()),
  autocomplete: "off",
  spellcheck: "false",
}) as HTMLInputElement;

const prometheusInput = el("input", {
  type: "url",
  id: "url-prometheus",
  className: "url",
  placeholder: buildDefaultPrometheus(),
  value: loadOrDefault(LS_PROMETHEUS, buildDefaultPrometheus()),
  autocomplete: "off",
  spellcheck: "false",
}) as HTMLInputElement;

const elasticsearchInput = el("input", {
  type: "url",
  id: "url-elasticsearch",
  className: "url",
  placeholder: buildDefaultElasticsearch(),
  value: loadOrDefault(LS_ELASTICSEARCH, buildDefaultElasticsearch()),
  autocomplete: "off",
  spellcheck: "false",
}) as HTMLInputElement;

const kibanaInput = el("input", {
  type: "url",
  id: "url-kibana",
  className: "url",
  placeholder: buildDefaultKibana(),
  value: loadOrDefault(LS_KIBANA, buildDefaultKibana()),
  autocomplete: "off",
  spellcheck: "false",
}) as HTMLInputElement;

const javaStatus = el("pre", { className: "status", id: "st-java" }, ["—"]);
const pyStatus = el("pre", { className: "status", id: "st-python" }, ["—"]);
const rustStatus = el("pre", { className: "status", id: "st-rust" }, ["—"]);
const grafanaStatus = el("pre", { className: "status", id: "st-grafana" }, ["—"]);
const prometheusStatus = el("pre", { className: "status", id: "st-prometheus" }, ["—"]);
const elasticsearchStatus = el("pre", { className: "status", id: "st-elasticsearch" }, ["—"]);
const kibanaStatus = el("pre", { className: "status", id: "st-kibana" }, ["—"]);

const graphNodes: Record<StackKey, HTMLButtonElement> = {
  rust: el("button", { type: "button", className: "graph-node", "data-stack": "rust" }, [
    "Rust",
  ]) as HTMLButtonElement,
  java: el("button", { type: "button", className: "graph-node", "data-stack": "java" }, [
    "Java",
  ]) as HTMLButtonElement,
  python: el("button", { type: "button", className: "graph-node", "data-stack": "python" }, [
    "Python",
  ]) as HTMLButtonElement,
};

const connectivityGraph = el("div", { className: "connectivity-graph", id: "connectivity-graph" }, [
  el("div", { className: "graph-heading" }, ["Connectivity graph"]),
  el("p", { className: "graph-hint" }, [
    "Nodes reflect the latest ",
    el("code", {}, ["GET"]),
    " probe for each app. Click a node to open that home page in the preview pane.",
  ]),
  el("div", { className: "graph-nodes" }, [
    graphNodes.rust,
    el("span", { className: "graph-edge", "aria-hidden": "true" }, ["—"]),
    graphNodes.java,
    el("span", { className: "graph-edge", "aria-hidden": "true" }, ["—"]),
    graphNodes.python,
  ]),
]);

const postgresNote = el("div", { className: "row row-static" }, [
  el("p", { className: "static-note" }, [
    el("strong", {}, ["PostgreSQL — "]),
    "TCP ",
    el("code", {}, ["127.0.0.1:5432"]),
    " (default Compose publish). Browsers cannot run a TCP reach check from this page; use ",
    el("code", {}, ["Test-NetConnection 127.0.0.1 -Port 5432"]),
    ", ",
    el("code", {}, ["psql"]),
    ", or your SQL client.",
  ]),
]);

const saveBtn = el("button", { type: "button", id: "save" }, ["Save URLs"]);
const refreshBtn = el("button", { type: "button", id: "refresh" }, ["Probe all again"]);

const connectivityRefresh = el("button", { type: "button", id: "refresh-connectivity", className: "secondary" }, [
  "Probe connectivity again",
]);

const fallbacks: Record<StackKey, () => string> = {
  rust: buildDefaultRust,
  java: buildDefaultJava,
  python: buildDefaultPython,
};

const iframeByStack: Record<StackKey, HTMLIFrameElement> = {
  rust: el("iframe", {
    className: "embed-frame",
    title: "Rust home",
    sandbox: "allow-scripts allow-forms allow-same-origin allow-popups",
  }) as HTMLIFrameElement,
  java: el("iframe", {
    className: "embed-frame",
    title: "Java home",
    sandbox: "allow-scripts allow-forms allow-same-origin allow-popups",
  }) as HTMLIFrameElement,
  python: el("iframe", {
    className: "embed-frame",
    title: "Python home",
    sandbox: "allow-scripts allow-forms allow-same-origin allow-popups",
  }) as HTMLIFrameElement,
};

const tabButtons: Record<StackKey, HTMLButtonElement> = {
  rust: el("button", { type: "button", className: "embed-tab", role: "tab", "data-tab": "rust" }, [
    "Rust",
  ]) as HTMLButtonElement,
  java: el("button", { type: "button", className: "embed-tab", role: "tab", "data-tab": "java" }, [
    "Java",
  ]) as HTMLButtonElement,
  python: el("button", { type: "button", className: "embed-tab", role: "tab", "data-tab": "python" }, [
    "Python",
  ]) as HTMLButtonElement,
};

const embedTablist = el("div", { className: "embed-tabs", role: "tablist" }, [
  tabButtons.rust,
  tabButtons.java,
  tabButtons.python,
]);

const embedPanels = el("div", { className: "embed-panels" }, [
  el("div", { className: "embed-panel", "data-panel": "rust", hidden: "" }, [iframeByStack.rust]),
  el("div", { className: "embed-panel", "data-panel": "java", hidden: "" }, [iframeByStack.java]),
  el("div", { className: "embed-panel", "data-panel": "python", hidden: "" }, [iframeByStack.python]),
]);

const embedReload = el("button", { type: "button", className: "secondary embed-reload" }, [
  "Reload current preview",
]);

const embedPane = el("aside", { className: "reach-col reach-col-embed" }, [
  el("h2", { className: "embed-pane-title" }, ["Home preview"]),
  embedTablist,
  embedReload,
  embedPanels,
]);

const leftCol = el("div", { className: "reach-col reach-col-main" }, [
  el("h1", {}, ["Exercises stack reach"]),
  intro,
  connectivityGraph,
  el("h2", { className: "section-title" }, ["Apps"]),
  el("section", { className: "grid" }, [
    row("Java (Spring)", javaInput, javaStatus, buildDefaultJava),
    row("Python (Flask)", pyInput, pyStatus, buildDefaultPython),
    row("Rust (Axum)", rustInput, rustStatus, buildDefaultRust),
  ]),
  el("h2", { className: "section-title" }, ["Connectivity"]),
  el("div", { className: "section-actions" }, [connectivityRefresh]),
  el("section", { className: "grid" }, [
    row("Grafana (dashboards)", grafanaInput, grafanaStatus, buildDefaultGrafana),
    row("Prometheus (UI + TSDB)", prometheusInput, prometheusStatus, buildDefaultPrometheus),
    row("Elasticsearch (HTTP API)", elasticsearchInput, elasticsearchStatus, buildDefaultElasticsearch),
    row("Kibana", kibanaInput, kibanaStatus, buildDefaultKibana),
    postgresNote,
  ]),
  el("div", { className: "actions" }, [saveBtn, refreshBtn]),
]);

const layout = el("div", { className: "reach-layout" }, [leftCol, embedPane]);
app.append(layout);

const inputByStack: Record<StackKey, HTMLInputElement> = {
  rust: rustInput,
  java: javaInput,
  python: pyInput,
};

let activeEmbedTab: StackKey = "rust";
const lastIframeSrc: Partial<Record<StackKey, string>> = {};

function setGraphNodeState(stack: StackKey, ok: boolean | null) {
  const btn = graphNodes[stack];
  btn.classList.remove("graph-node--ok", "graph-node--fail", "graph-node--pending");
  if (ok === null) btn.classList.add("graph-node--pending");
  else if (ok) btn.classList.add("graph-node--ok");
  else btn.classList.add("graph-node--fail");
}

function updateGraphFromProbes(javaOk: boolean, pythonOk: boolean, rustOk: boolean) {
  setGraphNodeState("java", javaOk);
  setGraphNodeState("python", pythonOk);
  setGraphNodeState("rust", rustOk);
}

function effectiveUrl(input: HTMLInputElement, fallback: () => string): string {
  return input.value.trim() || fallback();
}

function selectEmbedTab(stack: StackKey, opts?: { forceReload?: boolean }) {
  activeEmbedTab = stack;
  for (const k of Object.keys(tabButtons) as StackKey[]) {
    const on = k === stack;
    tabButtons[k].classList.toggle("embed-tab--active", on);
    tabButtons[k].setAttribute("aria-selected", on ? "true" : "false");
    const panel = embedPanels.querySelector(`[data-panel="${k}"]`) as HTMLElement;
    panel.hidden = !on;
  }
  const url = effectiveUrl(inputByStack[stack], fallbacks[stack]);
  const iframe = iframeByStack[stack];
  const force = opts?.forceReload === true;
  if (force || lastIframeSrc[stack] !== url) {
    iframe.removeAttribute("src");
    iframe.src = url;
    lastIframeSrc[stack] = url;
  }
}

function persistFromInputs() {
  localStorage.setItem(LS_JAVA, javaInput.value.trim() || buildDefaultJava());
  localStorage.setItem(LS_PYTHON, pyInput.value.trim() || buildDefaultPython());
  localStorage.setItem(LS_RUST, rustInput.value.trim() || buildDefaultRust());
  localStorage.setItem(LS_GRAFANA, grafanaInput.value.trim() || buildDefaultGrafana());
  localStorage.setItem(LS_PROMETHEUS, prometheusInput.value.trim() || buildDefaultPrometheus());
  localStorage.setItem(LS_ELASTICSEARCH, elasticsearchInput.value.trim() || buildDefaultElasticsearch());
  localStorage.setItem(LS_KIBANA, kibanaInput.value.trim() || buildDefaultKibana());
}

async function runProbes() {
  setGraphNodeState("java", null);
  setGraphNodeState("python", null);
  setGraphNodeState("rust", null);
  javaStatus.textContent = "…";
  pyStatus.textContent = "…";
  rustStatus.textContent = "…";
  grafanaStatus.textContent = "…";
  prometheusStatus.textContent = "…";
  elasticsearchStatus.textContent = "…";
  kibanaStatus.textContent = "…";

  const [j, p, r, g, pr, es, kb] = await Promise.all([
    probeGet(effectiveUrl(javaInput, buildDefaultJava)),
    probeGet(effectiveUrl(pyInput, buildDefaultPython)),
    probeGet(effectiveUrl(rustInput, buildDefaultRust)),
    probeGet(effectiveUrl(grafanaInput, buildDefaultGrafana)),
    probeGet(effectiveUrl(prometheusInput, buildDefaultPrometheus)),
    probeGet(effectiveUrl(elasticsearchInput, buildDefaultElasticsearch)),
    probeGet(effectiveUrl(kibanaInput, buildDefaultKibana)),
  ]);

  javaStatus.textContent = j.ok ? `OK — ${j.detail}` : `Fail — ${j.detail}`;
  pyStatus.textContent = p.ok ? `OK — ${p.detail}` : `Fail — ${p.detail}`;
  rustStatus.textContent = r.ok ? `OK — ${r.detail}` : `Fail — ${r.detail}`;
  grafanaStatus.textContent = g.ok ? `OK — ${g.detail}` : `Fail — ${g.detail}`;
  prometheusStatus.textContent = pr.ok ? `OK — ${pr.detail}` : `Fail — ${pr.detail}`;
  elasticsearchStatus.textContent = es.ok ? `OK — ${es.detail}` : `Fail — ${es.detail}`;
  kibanaStatus.textContent = kb.ok ? `OK — ${kb.detail}` : `Fail — ${kb.detail}`;
  updateGraphFromProbes(j.ok, p.ok, r.ok);
}

async function runConnectivityProbes() {
  grafanaStatus.textContent = "…";
  prometheusStatus.textContent = "…";
  elasticsearchStatus.textContent = "…";
  kibanaStatus.textContent = "…";

  const [g, pr, es, kb] = await Promise.all([
    probeGet(effectiveUrl(grafanaInput, buildDefaultGrafana)),
    probeGet(effectiveUrl(prometheusInput, buildDefaultPrometheus)),
    probeGet(effectiveUrl(elasticsearchInput, buildDefaultElasticsearch)),
    probeGet(effectiveUrl(kibanaInput, buildDefaultKibana)),
  ]);

  grafanaStatus.textContent = g.ok ? `OK — ${g.detail}` : `Fail — ${g.detail}`;
  prometheusStatus.textContent = pr.ok ? `OK — ${pr.detail}` : `Fail — ${pr.detail}`;
  elasticsearchStatus.textContent = es.ok ? `OK — ${es.detail}` : `Fail — ${es.detail}`;
  kibanaStatus.textContent = kb.ok ? `OK — ${kb.detail}` : `Fail — ${kb.detail}`;
}

saveBtn.addEventListener("click", () => {
  persistFromInputs();
  javaInput.value = localStorage.getItem(LS_JAVA) || javaInput.value;
  pyInput.value = localStorage.getItem(LS_PYTHON) || pyInput.value;
  rustInput.value = localStorage.getItem(LS_RUST) || rustInput.value;
  grafanaInput.value = localStorage.getItem(LS_GRAFANA) || grafanaInput.value;
  prometheusInput.value = localStorage.getItem(LS_PROMETHEUS) || prometheusInput.value;
  elasticsearchInput.value = localStorage.getItem(LS_ELASTICSEARCH) || elasticsearchInput.value;
  kibanaInput.value = localStorage.getItem(LS_KIBANA) || kibanaInput.value;
  void runProbes().then(() => selectEmbedTab(activeEmbedTab, { forceReload: true }));
});

refreshBtn.addEventListener("click", () => {
  void runProbes();
});

connectivityRefresh.addEventListener("click", () => {
  void runConnectivityProbes();
});

embedTablist.addEventListener("click", (e) => {
  const t = (e.target as HTMLElement).closest("[data-tab]");
  if (!t) return;
  const stack = t.getAttribute("data-tab") as StackKey;
  if (!stack || !tabButtons[stack]) return;
  selectEmbedTab(stack);
});

for (const stack of Object.keys(graphNodes) as StackKey[]) {
  graphNodes[stack].addEventListener("click", () => selectEmbedTab(stack));
}

embedReload.addEventListener("click", () => {
  selectEmbedTab(activeEmbedTab, { forceReload: true });
});

["input", "change"].forEach((ev) => {
  rustInput.addEventListener(ev, () => {
    if (activeEmbedTab === "rust") lastIframeSrc.rust = "";
  });
  javaInput.addEventListener(ev, () => {
    if (activeEmbedTab === "java") lastIframeSrc.java = "";
  });
  pyInput.addEventListener(ev, () => {
    if (activeEmbedTab === "python") lastIframeSrc.python = "";
  });
});

const style = document.createElement("style");
style.textContent = `
  body { font-family: system-ui, sans-serif; margin: 0; padding: 0; line-height: 1.45; background: #fafafa; color: #1a1a1a; }
  .reach-layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(300px, min(42vw, 520px));
    gap: 1rem 1.25rem;
    max-width: 1400px;
    margin: 0 auto;
    padding: 1rem 1rem 1.5rem;
    align-items: start;
    box-sizing: border-box;
  }
  @media (max-width: 900px) {
    .reach-layout { grid-template-columns: 1fr; }
    .reach-col-embed { position: static; height: min(70vh, 560px); }
  }
  .reach-col-main { min-width: 0; }
  .reach-col-embed {
    position: sticky;
    top: 0.75rem;
    height: calc(100vh - 1.5rem);
    max-height: calc(100vh - 1.5rem);
    display: flex;
    flex-direction: column;
    background: #fff;
    border: 1px solid #ddd;
    border-radius: 10px;
    padding: 0.65rem 0.75rem;
    box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  }
  .embed-pane-title { font-size: 1rem; margin: 0 0 0.5rem; font-weight: 650; }
  .embed-tabs { display: flex; gap: 0.35rem; flex-wrap: wrap; margin-bottom: 0.45rem; }
  .embed-tab {
    font: inherit;
    padding: 0.35rem 0.65rem;
    cursor: pointer;
    border: 1px solid #ccc;
    border-radius: 6px;
    background: #f4f4f5;
    color: #333;
  }
  .embed-tab--active {
    border-color: #16a34a;
    background: #ecfdf3;
    font-weight: 650;
    color: #14532d;
  }
  .embed-reload { margin-bottom: 0.45rem; font-size: 0.85rem; padding: 0.3rem 0.55rem; }
  .embed-panels { flex: 1 1 auto; min-height: 0; position: relative; }
  .embed-panel {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
  }
  .embed-panel[hidden] { display: none !important; }
  .embed-frame {
    flex: 1 1 auto;
    width: 100%;
    min-height: 200px;
    border: 1px solid #e4e4e7;
    border-radius: 8px;
    background: #fff;
  }
  h1 { font-size: 1.25rem; margin: 0 0 0.35rem; }
  h2.section-title { font-size: 1.05rem; margin: 1.25rem 0 0.5rem; font-weight: 650; }
  .intro { margin: 0 0 0.75rem; max-width: 52rem; }
  code { font-size: 0.9em; }
  .connectivity-graph {
    margin: 0 0 1rem;
    padding: 0.75rem 1rem;
    border: 1px solid #ddd;
    border-radius: 10px;
    background: #fff;
    max-width: 52rem;
  }
  .graph-heading { font-weight: 650; font-size: 0.98rem; margin-bottom: 0.35rem; }
  .graph-hint { margin: 0 0 0.65rem; font-size: 0.82rem; color: #555; }
  .graph-nodes {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
    gap: 0.35rem 0.5rem;
  }
  .graph-node {
    font: inherit;
    font-weight: 600;
    padding: 0.5rem 0.85rem;
    border-radius: 999px;
    border: 2px solid #a1a1aa;
    cursor: pointer;
    background: #f4f4f5;
    color: #18181b;
    min-width: 5rem;
  }
  .graph-node--pending { border-color: #a1a1aa; background: #f4f4f5; }
  .graph-node--ok { border-color: #22c55e; background: #dcfce7; color: #14532d; }
  .graph-node--fail { border-color: #ef4444; background: #fee2e2; color: #7f1d1d; }
  .graph-edge { color: #a1a1aa; font-size: 0.75rem; user-select: none; }
  .grid { display: flex; flex-direction: column; gap: 1rem; margin: 1rem 0; }
  .label-row { display: flex; flex-wrap: wrap; align-items: baseline; gap: 0.5rem 0.75rem; margin-bottom: 0.25rem; }
  .label-row label { font-weight: 600; }
  .open-tab { font-size: 0.88rem; font-weight: 500; }
  .url { width: 100%; box-sizing: border-box; padding: 0.4rem 0.5rem; font: inherit; }
  .status-wrap { margin-top: 0.35rem; }
  .status { margin: 0; white-space: pre-wrap; font-size: 0.85rem; color: #333; }
  .row-static { margin-top: 0.25rem; }
  .static-note { margin: 0; font-size: 0.88rem; color: #444; line-height: 1.45; }
  .section-actions { margin: 0 0 0.5rem; }
  .actions { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-top: 0.75rem; }
  button { font: inherit; padding: 0.4rem 0.75rem; cursor: pointer; }
  button.secondary { font-size: 0.9rem; }
`;
document.head.append(style);

void runProbes().then(() => selectEmbedTab("rust"));
