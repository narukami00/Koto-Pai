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
- Default Seed Administrator: `admin` / `password123`

### Stop Services
```bash
docker compose down
```

### Reset Database Volume (Fresh Start)
```bash
docker compose down -v
```

---

## 3. Environment Variables Reference

| Variable | Default (Compose) | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Active Spring profile (`prod` or `h2`) |
| `DB_HOST` | `postgres` | Hostname of PostgreSQL server |
| `DB_PORT` | `5432` | Port of PostgreSQL server |
| `DB_NAME` | `kotopai` | Database name |
| `DB_USER` | `kotopai` | Database username |
| `DB_PASSWORD` | `kotopai_secret` | Database password |
| `DB_SCHEMA` | `public` | Database schema name |
| `DB_SSLMODE` | `disable` | SSL mode (`disable`, `require`, `verify-full`) |
| `HOST_APP_PORT` | `8080` | Host port mapped to container port 8080 |

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

### Render / Railway / Fly.io
1. Connect repository root containing `Dockerfile`.
2. Set environment variables:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE` (from your PostgreSQL addon or Supabase).
3. Set health check path to `/actuator/health`.
4. Expose port `8080`.

