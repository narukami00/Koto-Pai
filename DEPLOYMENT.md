# Koto Pai — Deployment & Containerization Guide

This guide covers building, running, and deploying Koto Pai using Docker and Docker Compose.

---

## 1. Architecture Overview

- **Application Image:** Multi-stage build based on `eclipse-temurin:21-jre-alpine` running as a non-root user (`kotopai:kotopai`).
- **Layered JAR:** Spring Boot 3/4 jar layers (`dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`) are extracted and cached separately for fast builds.
- **Database:** PostgreSQL 16 (`postgres:16-alpine`) with health checks and persistent volume storage.
- **Container Health:** Checked automatically via `/actuator/health`.

---

## 2. Quickstart with Docker Compose

Ensure Docker (or Docker Desktop) is running on your machine.

### Start All Services (Database + App)
```bash
docker compose up -d --build
```

### View Live Logs
```bash
docker compose logs -f app
```

### Access Application
- Web Dashboard: [http://localhost:8080](http://localhost:8080)
- Actuator Health Check: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Demo logins (Compose starts with the `demo` profile): `rafsan10`, `tanvir_h`, `nabila_r`, `samiha` (all `password123`), with three demo outings. See `MANUAL_TEST.md`.
- There is no demo admin under Compose. To see the Admin Console, set `KOTOPAI_ADMIN_USER_CODE` (see [Administrator account](#administrator-account)), or run locally with the `h2` profile, which still creates `admin` / `password123`.
- If port 8080 is taken (Docker Desktop can hold it), use another: `HOST_APP_PORT=8082 docker compose up --build`.

### Stop Services
```bash
docker compose down
```

### Reset Database Volume (Fresh Start)
```bash
docker compose down -v
```

---

## 2b. Monitoring with Prometheus and Grafana

The app publishes metrics (requests, latency, JVM, database pool) at `/actuator/prometheus`. The endpoint is
protected with HTTP Basic using a dedicated scrape account, and stays closed if no password is configured.

```bash
docker compose --profile monitoring up -d --build
```

| Tool | URL | Login |
|---|---|---|
| Prometheus | http://localhost:9090 (Status → Targets shows `kotopai` UP) | none |
| Grafana | http://localhost:3000 → dashboard **Koto Pai - Application Overview** | `admin` / `kotopai_grafana` (change with `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD`) |

- `METRICS_PASSWORD` (default `kotopai_metrics` in Compose) is the scrape password; `METRICS_USER` defaults to `prometheus`.
- The datasource and dashboard are provisioned from `monitoring/`, so nothing needs clicking together by hand.
- The default `docker compose up` does not start Prometheus or Grafana.
- On a hosted deployment, leave `METRICS_PASSWORD` unset to keep metrics closed, or set it and point a
  Grafana Cloud / Prometheus scrape job at `https://<your-app>/actuator/prometheus` with basic auth.
- Uptime checks on the live site can use `/actuator/health` (public).

---

## 3. Environment Variables Reference

| Variable | Default (Compose) | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod,demo` | `prod` = PostgreSQL; add `demo` to create demo users/outings; `h2` = in-memory (local only). Use plain `prod` for a real deployment |
| `PORT` | `8080` | Port the app listens on (hosting platforms such as Render set this for you) |
| `DB_HOST` | `postgres` | Hostname of PostgreSQL server |
| `DB_PORT` | `5432` | Port of PostgreSQL server |
| `DB_NAME` | `kotopai` | Database name |
| `DB_USER` | `kotopai` | Database username |
| `DB_PASSWORD` | `kotopai_secret` | Database password |
| `DB_SCHEMA` | `public` | Database schema name |
| `DB_SSLMODE` | `disable` | SSL mode (`disable`, `require`, `verify-full`) |
| `HOST_APP_PORT` | `8080` | Host port mapped to container port 8080 |
| `METRICS_USER` / `METRICS_PASSWORD` | `prometheus` / `kotopai_metrics` (Compose) | Scrape account for `/actuator/prometheus`; metrics stay closed when the password is empty |
| `HOST_PROMETHEUS_PORT` / `HOST_GRAFANA_PORT` | `9090` / `3000` | Host ports for the monitoring profile |
| `KOTOPAI_ADMIN_USER_CODE` | empty (no admin bootstrap) | Handle of the administrator. See [Administrator account](#administrator-account) |
| `KOTOPAI_ADMIN_EMAIL` / `KOTOPAI_ADMIN_PASSWORD` | empty | Only needed when that user does not exist yet: the e-mail and a password of at least 8 characters for the new account |
| `KOTOPAI_ADMIN_DISPLAY_NAME` | `Administrator` | Display name for a newly created administrator |

### Administrator account

A hosted deployment has no built-in administrator and no account with a published password. The demo `admin` user
(password `password123`) is created only for local runs (`dev` / `h2`), never under `demo` on a hosted deployment.

To get a real administrator, set the settings above. On every start the app does the following, and it is safe to
repeat:

- `KOTOPAI_ADMIN_USER_CODE` empty: nothing happens.
- The user already exists (they registered normally): they are promoted to ADMIN. Their password is not touched, so
  only the user code is needed.
- The user does not exist: an ADMIN account is created from `KOTOPAI_ADMIN_EMAIL` and `KOTOPAI_ADMIN_PASSWORD`.
  A bad value is logged and skipped; the app still starts.

Set the password only as a secret in the hosting dashboard; it is never logged. Admins see platform analytics and can
search users and change roles. They cannot see anyone's outings or debts.

Without the bootstrap, the same result needs one manual SQL statement:
`UPDATE users SET role = 'ADMIN' WHERE user_code = '<handle>';`

---

## 4. Standalone Docker Run (External DB / Cloud Supabase)

If connecting Koto Pai to a managed PostgreSQL database (e.g. Supabase, Render, Neon, RDS):

```bash
docker build -t kotopai:latest .

docker run -d \
  --name kotopai-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=db.your-supabase-id.supabase.co \
  -e DB_PORT=5432 \
  -e DB_NAME=postgres \
  -e DB_USER=postgres \
  -e DB_PASSWORD=your_secure_password \
  -e DB_SSLMODE=require \
  kotopai:latest
```

---

## 5. Cloud Platform Deployments

### Render (app) + Neon (database)
1. **Neon:** create a project and database; copy the host, database, user and password from the connection details (use the direct host, not a URL).
2. **Render:** New → Web Service → connect the repository → Runtime **Docker** (it uses the `Dockerfile`).
3. Environment variables on the service:
   - `SPRING_PROFILES_ACTIVE` = `prod` (add `,demo` only if you want the demo accounts on the live site)
   - `DB_HOST`, `DB_PORT` (`5432`), `DB_NAME`, `DB_USER`, `DB_PASSWORD` from Neon
   - `DB_SSLMODE` = `require` (Neon needs SSL)
   - `KOTOPAI_ADMIN_USER_CODE` = the handle of your own account, to make it the administrator (see
     [Administrator account](#administrator-account))
4. Health check path: `/actuator/health` (public, returns `{"status":"UP"}`).
5. Render sets `PORT` itself; the app reads it. No port setting is needed.

The same steps work on Railway or Fly.io (they also build the `Dockerfile` and provide `PORT`).

Live site: https://koto-pai-aatd.onrender.com (Render free tier sleeps after about 15 minutes without traffic and takes
about a minute to wake; the monitoring below keeps it awake).

### CI/CD (GitHub Actions, `.github/workflows/ci.yml`)
| Job | Runs on | What it does |
|---|---|---|
| Unit tests | every push and pull request | `./mvnw test` with the coverage gate |
| Integration tests | after unit tests | `./mvnw verify -DskipUTs=true` |
| Docker image builds and starts | after unit tests | builds the `Dockerfile`, starts it next to a throw-away PostgreSQL and waits for `/actuator/health` to be `UP` |
| Deploy to Render | **only pushes to `main`**, after all three above pass | calls the Render deploy hook for that exact commit, then waits for the public health check |

- The deploy hook lives in the repository secret `RENDER_DEPLOY_HOOK_URL` (Settings → Secrets and variables → Actions). It is never in the code.
- Render's own Auto-Deploy is **off**, so a deploy only happens when this pipeline asks for it.
- `main` is protected: changes arrive by pull request with at least one approval.
- The final health wait cannot tell old from new if the new build fails (Render keeps serving the old version), so also glance at the Render dashboard after a release.

### Monitoring the live site
1. **Uptime check (free):** create an HTTP(s) monitor in UptimeRobot, Better Stack or similar for
   `https://koto-pai-aatd.onrender.com/actuator/health`, every 5 minutes, alert if the body does not contain `"status":"UP"`.
   Pinging every 5 minutes also stops the free instance from going to sleep.
2. **Metrics dashboard for the live site (runs on your machine):** `METRICS_PASSWORD` must be set on the Render service
   (the endpoint answers 401 to everyone without it; with no password configured it is closed completely).
   ```powershell
   $env:LIVE_METRICS_PASSWORD = "<the METRICS_PASSWORD set on Render>"
   docker compose -f docker-compose.live-monitoring.yml up -d
   ```
   - Grafana: http://localhost:3001 (`admin` / `kotopai_grafana`) → dashboard **Koto Pai - Application Overview**
   - Prometheus: http://localhost:9091 → Status → Targets, `kotopai` should be UP
   - Stop with `docker compose -f docker-compose.live-monitoring.yml down` (with `LIVE_METRICS_PASSWORD` still set in
     that shell, or set it to any value). It runs beside the local stack
     (http://localhost:3000 is the *local* copy of the app).
   - Scraping every 15 s also keeps the free Render instance awake while it runs.
   - Prefer an always-on, shareable dashboard? Use hosted Grafana Cloud instead (next section).

#### Hosted dashboard with Grafana Cloud (free, always on, public URL)
Prometheus and Grafana above run on your laptop, so they only work while it is on. Grafana Cloud runs both for you:
1. Create a free account at https://grafana.com (a "stack" such as `<name>.grafana.net` is created for you).
2. In the stack: **Connections → Add new connection → "Metrics Endpoint"** (scrapes a URL for you). Fill in:
   - Scrape job name: `kotopai`
   - URL: `https://koto-pai-aatd.onrender.com/actuator/prometheus`
   - Authentication: Basic, username `prometheus`, password = the `METRICS_PASSWORD` set on Render
   - Test the connection, then save. Metrics appear within a couple of minutes.
3. **Dashboards → New → Import → Upload JSON file** → `monitoring/grafana-cloud/kotopai-dashboard.json`,
   choose your Grafana Cloud Prometheus data source (named like `grafanacloud-<stack>-prom`) and import.
   Pick `kotopai` in the **Scrape job** dropdown at the top of the dashboard.
- That file is the same dashboard as `monitoring/grafana/dashboards/kotopai.json`, made portable (data source and job name are chosen on import).
- The password is typed into Grafana Cloud's form only; it is never stored in this repository.
- Grafana Cloud's scraper also wakes the free Render instance, so the first minute after a long idle can show a gap.
3. **Logs:** Render dashboard → Logs, filter by `ERROR` after a release.

