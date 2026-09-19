# AdServe — Production Cloud Deployment Guide (Vercel + Railway)

This guide documents the complete end-to-end deployment of the **AdServe — Mini Ad Serving & Campaign Analytics Platform** to **Vercel** (React Frontend) and **Railway** (Spring Boot Backend + MySQL + Redis + Kafka).

---

## 1. Deployment Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                    Vercel Global Edge                       │
│           React 18 + Vite SPA + Tailwind + Recharts         │
│          Static CDN + vercel.json SPA Route Rewrites        │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTPS API Calls (Axios)
                               │ VITE_API_BASE_URL
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Railway Production Platform                 │
│         Spring Boot 3.2.5 Backend (Java 17 Temurin)         │
│             Configured with application-prod.yml            │
│            CORS_ALLOWED_ORIGINS = https://<vercel-app>       │
└───────────────┬──────────────┬──────────────┬───────────────┘
                │              │              │
                ▼              ▼              ▼
┌──────────────────────┐ ┌──────────────┐ ┌───────────────────┐
│     Railway MySQL    │ │ Railway Redis│ │   Railway Kafka   │
│   InnoDB Relational  │ │ In-Memory TTL│ │  KRaft / Upstash  │
│   JPA ddl-auto=update│ │ Cache Layer  │ │  Event Streaming  │
└──────────────────────┘ └──────────────┘ └───────────────────┘
```

---

## 2. Prerequisites & Accounts

1. **GitHub Account**: A public or private repository containing this codebase.
2. **Railway Account** ([railway.com](https://railway.com)): Hosting the Spring Boot backend, MySQL, Redis, and Kafka.
3. **Vercel Account** ([vercel.com](https://vercel.com)): Hosting the React + Vite frontend.

---

## 3. Step 1: Push Code to GitHub

Ensure all sensitive files (`.env`, `node_modules/`, `target/`, `dist/`) are ignored by `.gitignore`.

```bash
# Initialize and commit codebase
git init
git add .
git commit -m "feat: complete AdServe Phases 1-10 with Vercel and Railway production configuration"

# Add your GitHub remote and push
git remote add origin https://github.com/<your-username>/<your-repo>.git
git branch -M main
git push -u origin main
```

---

## 4. Step 2: Deploy Supporting Services on Railway

In your Railway project dashboard ([railway.com/new](https://railway.com/new)):

### A. Deploy Railway MySQL
1. Click **+ New Service** → **Database** → **MySQL**.
2. Railway will provision a managed MySQL 8.0 instance and generate native environment variables:
   - `MYSQLHOST` (Internal hostname, e.g. `mysql.railway.internal`)
   - `MYSQLPORT` (Default `3306`)
   - `MYSQLDATABASE` (Default `railway`)
   - `MYSQLUSER` (Default `root`)
   - `MYSQLPASSWORD` (Generated secure root password)
3. Copy or reference these variables in your backend service.

### B. Deploy Railway Redis
1. Click **+ New Service** → **Database** → **Redis**.
2. Railway provisions Redis 7 and provides:
   - `REDISHOST` (Internal hostname, e.g. `redis.railway.internal`)
   - `REDISPORT` (Default `6379`)
   - `REDISPASSWORD` (Generated secure Redis password)

### C. Deploy Apache Kafka on Railway
You have two robust options for Kafka in production:
- **Option 1: Railway Docker Service (Included in repo)**:
  - Add **+ New Service** → **Docker Image** → `apache/kafka:3.7.0`.
  - Set Environment Variables:
    - `KAFKA_NODE_ID`: `1`
    - `KAFKA_PROCESS_ROLES`: `broker,controller`
    - `KAFKA_LISTENERS`: `PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093`
    - `KAFKA_ADVERTISED_LISTENERS`: `PLAINTEXT://kafka.railway.internal:9092`
    - `KAFKA_CONTROLLER_QUORUM_VOTERS`: `1@kafka.railway.internal:9093`
    - `KAFKA_CONTROLLER_LISTENER_NAMES`: `CONTROLLER`
    - `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR`: `1`
    - `CLUSTER_ID`: `4L622nShTUiBenYhTxtumQ`
- **Option 2: Upstash Kafka (Serverless / Managed)**:
  - Create a free Kafka cluster at [upstash.com](https://upstash.com).
  - Copy the bootstrap server URL and SASL credentials.

---

## 5. Step 3: Deploy Spring Boot Backend to Railway

1. In the same Railway project, click **+ New Service** → **GitHub Repo** → select your repository.
2. Railway detects the root `Dockerfile` and `railway.json`.
3. Go to **Variables** tab and configure the production environment variables:

| Variable | Recommended Production Value | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates `application-prod.yml` |
| `DB_HOST` | `${{MySQL.MYSQLHOST}}` | References MySQL service internal host |
| `DB_PORT` | `${{MySQL.MYSQLPORT}}` | References MySQL service port |
| `DB_NAME` | `${{MySQL.MYSQLDATABASE}}` | References MySQL database name |
| `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` | References MySQL username |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` | References MySQL password |
| `REDIS_HOST` | `${{Redis.REDISHOST}}` | References Redis service internal host |
| `REDIS_PORT` | `${{Redis.REDISPORT}}` | References Redis service port |
| `REDIS_PASSWORD` | `${{Redis.REDISPASSWORD}}` | References Redis service password |
| `KAFKA_BOOTSTRAP_SERVERS`| `kafka.railway.internal:9092` | References Kafka broker internal host:port |
| `JWT_SECRET` | *(Generate a 64+ character random hex key)* | HMAC-SHA256 token signing secret |
| `JWT_EXPIRATION` | `86400000` | 24-hour expiration in ms |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` *(update with Vercel URL in Step 5)* | Permitted CORS origins |
| `SERVER_PORT` | `8080` | Default listening port |

4. Go to **Settings** → **Networking** → Click **Generate Domain**.
   - Railway will generate an HTTPS public URL, for example:
     `https://adserve-backend-production.up.railway.app`
5. Verify health:
   ```bash
   curl https://adserve-backend-production.up.railway.app/actuator/health
   # Expected response: {"status":"UP", ...}
   ```

---

## 6. Step 4: Deploy React Frontend to Vercel

1. Log in to [vercel.com](https://vercel.com) and click **Add New...** → **Project**.
2. Select your GitHub repository.
3. Configure the build settings:
   - **Framework Preset**: `Vite`
   - **Root Directory**: `adserve-frontend` *(or leave as `./` if using root `vercel.json`)*
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
4. Add **Environment Variables**:
   - `VITE_API_BASE_URL`: The actual Railway backend URL from Step 3:
     `https://adserve-backend-production.up.railway.app`
5. Click **Deploy**.
6. Once deployment completes, Vercel will assign a public URL, for example:
   `https://adserve-frontend-xxxxx.vercel.app`

---

## 7. Step 5: Synchronize Backend CORS

1. Return to the Railway dashboard for the backend service.
2. Under **Variables**, update `CORS_ALLOWED_ORIGINS` to include your actual Vercel URL:
   ```text
   CORS_ALLOWED_ORIGINS=https://adserve-frontend-xxxxx.vercel.app,http://localhost:5173
   ```
3. Railway will automatically redeploy the backend with the new allowed origin.
4. Verify that browser requests from Vercel no longer trigger CORS blockages.

---

## 8. Step 6: End-to-End Production Smoke Test

Run the following verification checklist against the deployed platform:

| Test Item | Verification Method | Expected Result |
|---|---|---|
| **1. Frontend Loading** | Open `https://<vercel-app>.vercel.app` | React SPA renders with dark mode, sidebar, and Tailwind styling. |
| **2. SPA Route Refresh** | Directly refresh `/dashboard` or `/campaigns` | Page refreshes without 404 error (verified by `vercel.json` rewrites). |
| **3. User Registration** | Navigate to `/register` and submit new advertiser | 200 OK, creates user and advertiser entity in MySQL. |
| **4. Login & JWT** | Login at `/login` with seeded `admin@adserve.com` | Receives signed JWT and redirects to Admin Dashboard. |
| **5. Campaign Creation** | Create campaign targeted to `IN`, `ANDROID`, `GAMING` | 201 Created in MySQL. |
| **6. Advertisement Creation** | Create advertisement creative for the campaign | 201 Created, status ACTIVE. |
| **7. Ad Serving Engine** | Call `GET https://<railway-app>/api/ad-server/serve?country=IN&device=ANDROID&category=GAMING` | 200 OK, returns winner ad creative JSON. |
| **8. Redis Cache HIT** | Call the serve endpoint a second time | Redis HIT in < 10 ms; cache hit counter increments. |
| **9. Kafka Impression** | Call `POST /api/ad-server/{id}/impression` | Kafka producer dispatches; consumer persists to MySQL asynchronously. |
| **10. Kafka Click** | Call `POST /api/ad-server/{id}/click` | Kafka consumer increments click count. |
| **11. CTR Analytics** | Refresh `/dashboard` and check Campaign Analytics | Shows impressions, clicks, and calculated CTR % from live database. |
| **12. Authorization Check** | Login as advertiser and attempt to view another advertiser's ads | HTTP 403 Forbidden correctly returned. |

---

## 9. Troubleshooting & FAQ

### 1. Vercel routes return 404 on page refresh
- **Cause**: Single-page applications require all paths to be routed through `index.html`.
- **Solution**: Ensure `adserve-frontend/vercel.json` contains:
  ```json
  { "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }
  ```

### 2. Browser console shows CORS error (`No 'Access-Control-Allow-Origin' header`)
- **Cause**: `CORS_ALLOWED_ORIGINS` in Railway does not match the exact Vercel protocol and domain.
- **Solution**: Verify `CORS_ALLOWED_ORIGINS=https://<your-exact-app>.vercel.app` (do not include trailing slashes).

### 3. Backend fails to connect to MySQL (`Access denied` or `Connection refused`)
- **Cause**: Incorrect database credentials or port.
- **Solution**: Ensure you are using Railway's internal service variables `${{MySQL.MYSQLHOST}}`, `${{MySQL.MYSQLUSER}}`, etc., or matching environment variables in `application-prod.yml`.

### 4. Redis fails with `NOAUTH Authentication required`
- **Cause**: Cloud Redis requires password authentication, but `REDIS_PASSWORD` was omitted.
- **Solution**: Set `REDIS_PASSWORD` in Railway variables. `application-prod.yml` automatically passes it to `spring.data.redis.password`.

---

## 10. Production Security Checklist

- [x] **No hardcoded secrets**: All passwords, tokens, and keys externalized to environment variables.
- [x] **Git protection**: `.env`, `.env.local`, `target/`, `node_modules/`, `dist/` excluded in `.gitignore`.
- [x] **Non-destructive JPA**: `spring.jpa.hibernate.ddl-auto` set to `update` in `application-prod.yml` (never `create-drop`).
- [x] **Actuator security**: Management endpoints restricted to `health`, `info`, and `metrics`; sensitive configuration environments disabled.
- [x] **Non-root container**: Backend Docker image executes under system user `appuser` (UID 1001).
- [x] **Strict CORS**: Wildcard `*` prohibited in production; exact Vercel origin required.
- [x] **HTTPS enforcement**: Automatic SSL/TLS termination provided by Vercel and Railway edge routers.
