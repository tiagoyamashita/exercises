# Python exercises

## Virtual environment

From this directory (`python/`):

**Windows (PowerShell)**

```powershell
.\scripts\init_venv.ps1
```

**macOS / Linux**

```bash
chmod +x scripts/init_venv.sh
./scripts/init_venv.sh
```

This creates `exercises/` (the virtual environment directory), upgrades `pip`, and installs the project in editable mode plus dev tools from `requirements-dev.txt`.

## Test dashboard (Flask)

From `python/` with the venv activated:

```bash
exercises-web
```

Open `http://127.0.0.1:5000/`. The UI loads `reports/junit.xml`, lists pytest results, and **Run all tests** / **Re-run** invoke pytest on the server (for local development). Optional: `EXERCISES_PROJECT_ROOT` points at another checkout; default is the directory that contains `pyproject.toml`.
