# Stack reach UI

Small **Vite** page that **GET-probes** the Java, Python, and Rust dashboard URLs from the browser. Each stack has an **editable URL** with the usual local defaults (`127.0.0.1` ports **8080** / **5000** / **8082** — same as [../DOCKER.md](../DOCKER.md)).

## Behaviour

1. **Inputs** — You can type any base URL; values persist in **`localStorage`** when you click **Save URLs**.
2. **Defaults** — If an input is empty, the probe uses, in order: saved value → **`VITE_EXERCISES_*_EMBED_URL`** from the build (see `.env.example`) → for Java only, legacy **`VITE_EXERCISES_EMBED_URL`** → localhost default.
3. **Refresh** — Re-runs all three probes without saving (uses current field text, or defaults when empty).
4. **CORS** — Probes use `fetch(..., { mode: 'cors' })`. Each app must allow **GET** from this UI’s origin or the browser will report a network / CORS-style error.

## Run

```bash
cd reach-ui
npm install
npm run dev
```

Open the URL Vite prints (usually **http://127.0.0.1:5173/**).

Production build:

```bash
npm run build
npm run preview
```

## Configure defaults at build time

Copy `.env.example` to `.env`, set `VITE_*` values, then `npm run build`. User-edited URLs in the browser still override via **Save URLs** (stored locally).
