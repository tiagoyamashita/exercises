# Docker / Podman

Build contexts live next to each stack:

| Directory | Image | Default port |
|-----------|--------|----------------|
| `java/` | Spring Boot | **8080** |
| `python/` | Flask dashboard | **5000** |
| `rust/` | Axum dashboard | **8082** |

From the **repository root** (use **Podman** if `docker` is not installed):

```bash
podman compose up --build
```

With Docker Engine:

```bash
docker compose up --build
```

Works with **Podman** using `podman compose` (Compose v2) or legacy `podman-compose`.

### Compose layout

- **`postgres`** — database on host port **5432** (named volume `exercises_pg_data`).
- **`java`** — uses Spring **`postgres`** profile; connects to the `postgres` service (`DB_HOST=postgres`). The **Dockerfile** keeps `pom.xml`, `src`, `mvnw`, and `target/` (including **Surefire reports** from the image build) so the **test dashboard** works inside Compose, not only when running `./mvnw` on the host.
- **`python`** / **`rust`** — listen on `0.0.0.0` inside the container (required for published ports).
- **`grafana`** — dashboards on **3000**; provisioning under **`grafana/`**. Started by default with the apps. **`GF_SECURITY_ALLOW_EMBEDDING=true`** so it can load inside an **`<iframe>`** (dev-oriented; tighten for production).
- **`elasticsearch`** / **`logstash`** / **`kibana`** — ELK (lab defaults; security off). **Not** started by default; enable with **`--profile elk`** (see below). Config under **`elk/`**.

Use **`elk/docker-compose.yml`** or **`grafana/docker-compose.yml`** only if you want those stacks **without** the app services — do **not** run them at the same time as the root file when the same ports are published (duplicate **3000** / ELK ports).

**Enable ELK with root Compose:**

```bash
podman compose --profile elk up --build
```

URLs (use **`127.0.0.1`** in the browser on Windows if **`localhost`** hangs or refuses — see troubleshooting below):

- Java: `http://127.0.0.1:8080/`
- Python: `http://127.0.0.1:5000/`
- Rust: `http://127.0.0.1:8082/`
- Grafana: `http://127.0.0.1:3000/` (default login `admin` / `admin`; `GF_SERVER_ROOT_URL` matches this — use the same host you type in the address bar)
- With **`--profile elk`**: Elasticsearch `http://127.0.0.1:9200/`, Kibana `http://127.0.0.1:5601/`, Logstash Beats **5044**

### Browser cannot reach containers (Podman on Windows)

Containers can be **Up** in `podman compose ps` while the browser still fails. Common causes:

1. **`localhost` uses IPv6 first** — On some Windows setups, `http://localhost:PORT` goes to **`::1`** while published ports are only on **IPv4**. Try **`http://127.0.0.1:PORT/`** for Java (**8080**), Python (**5000**), Rust (**8082**), Grafana (**3000**).
2. **Podman machine not running** — From PowerShell: `podman machine list` then `podman machine start` if the VM is stopped.
3. **Grafana still starting** — First boot can take a minute; wait and reload. Check logs: `podman compose logs grafana --tail 50`.
4. **Port clash** — Another program (or a second Compose project) using **3000** / **8080** / etc. shows as running but wrong app. Run `podman compose ps` and confirm **PORTS** include `0.0.0.0:3000->3000/tcp` (or similar).
5. **Firewall** — Allow **Podman** / **WSL** / **gvproxy** through Windows Defender Firewall if prompted.

Quick checks from PowerShell (after `podman compose up`):

```powershell
podman compose ps
Test-NetConnection 127.0.0.1 -Port 3000
Test-NetConnection 127.0.0.1 -Port 8080
```

Optional **browser reach checker** (editable URLs + `GET` probes, `VITE_*` build defaults): [../reach-ui/README.md](../reach-ui/README.md).

### Build one service

```bash
docker compose build java
docker compose up postgres java
```

### Java H2 only (no Postgres)

Run the image without Compose wiring — override profile and DB settings:

```bash
docker build -t exercises-java ./java
docker run --rm -p 8080:8080 exercises-java
```

(Default Spring profile uses in-memory H2 when `SPRING_PROFILES_ACTIVE` is unset.)

### Standalone Postgres (Podman scripts)

See **`postgre/README.md`** for `podman` scripts that match this Compose database defaults.

### Kubernetes image tags

Push images to your registry using the same repository names as in **`kubernetes-orchestration/helm/exercises-stack/values.yaml`** (defaults: `exercises-java`, `exercises-python`, `exercises-rust`), then set **`global.imageRegistry`** in Helm. See **`kubernetes-orchestration/README.md`**.
