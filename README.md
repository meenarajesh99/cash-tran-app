# Cash Tran

Cash Tran is a peer-to-peer payment application for transferring CT Bucks between users. It provides a React web interface backed by a Spring Boot API and PostgreSQL database.

## Features

- Register and log in with JWT authentication; newly registered users can enroll in MFA and verify an authenticator code at login.
- View account details and balances, update an email address, and find users/accounts.
- Send a transfer immediately or request money from another user. Review pending requests and approve or reject them.
- View transfer history and download a transaction statement as a PDF.
- Request a password reset by email.
- Review API documentation and health information through Swagger/OpenAPI and Spring Boot Actuator.

## Technology and layout

- **Frontend:** React 19, Vite, React Router, Material UI, and Nginx
- **Backend:** Java 21, Spring Boot 3.5, Spring Security, Maven, and the OpenTelemetry Java agent
- **Database:** PostgreSQL with Flyway schema migrations and Hibernate schema validation
- **Deployment:** Docker and Docker Compose locally; Docker web services on Render
- **Observability:** OpenTelemetry Collector configured to export traces, metrics, and logs to Grafana Cloud

```text
cashtran-frontend/   React application, tests, and frontend Dockerfile
cashtran-server/     Spring Boot API, tests, Flyway migrations, and backend Dockerfile
otel/config.yaml     OpenTelemetry Collector pipelines and Grafana Cloud exporter
docker-compose.yml   Local database, API, frontend, and telemetry collector
docker-compose-prod.yml
render.yaml          Render backend and frontend service configuration
```

## Run locally

Install Docker Desktop with Docker Compose. Create a private `.env` file in the repository root; do not commit it. The Compose file uses the following database and JWT settings:

```dotenv
DB_NAME=postgres
DB_USERNAME=postgres
DB_PASSWORD=<your-local-database-password>
JWT_BASE64_SECRET=<your-generated-base64-secret>
```

The Compose stack includes an OpenTelemetry Collector that exports to Grafana Cloud. Configure these values too, or adjust/disable the collector if you are not using Grafana Cloud:

```dotenv
GRAFANA_CLOUD_OTLP_ENDPOINT=<your-OTLP-endpoint>
GRAFANA_CLOUD_INSTANCE_ID=<your-instance-id>
GRAFANA_CLOUD_API_KEY=<your-api-key>
```

Start the stack from the repository root:

```bash
docker compose up --build
```

The frontend is at `http://localhost:3000`, the backend at `http://localhost:8080`, and PostgreSQL is exposed on local port `5433` by default. The `db_data` named volume preserves PostgreSQL data between container restarts. Flyway applies migrations at backend startup; the SQL migration files are in `cashtran-server/src/main/resources/db/migration/`.

Stop the stack with `docker compose down`. To also delete the local database volume, run `docker compose down --volumes`; this permanently deletes local database data.

### Run frontend or backend separately

For the frontend, install Node.js and run these commands from `cashtran-frontend`:

```bash
npm ci
npm run dev
npm test
npm run lint
npm run build
```

For the backend, install Java 21 and use the Maven wrapper from `cashtran-server`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The backend needs a reachable PostgreSQL database and the appropriate Spring profile/environment variables. The frontend's Vite development server proxies `/api` requests to `http://localhost:8080`.

## Database and profiles

PostgreSQL is the application's persistent store. Hibernate validates the schema, while Flyway manages schema changes through versioned SQL migrations. Back up the database before applying production changes; do not use database-volume deletion as a production reset method.

The backend has Spring profiles for local, Docker, Neon, and production environments. The local Compose stack selects `docker`; Render selects `prod`. The production profile expects:

- `DB_URL` — JDBC PostgreSQL connection URL
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_BASE64_SECRET`

When using Neon, configure its connection details as protected environment variables. The `application-neon.properties` profile uses `NEON_DB_URL` and `NEON_DB_USERNAME`; select that profile only when those settings are provided. Do not put database URLs or credentials in this README or in source control.

## Deploy to Render

`render.yaml` defines two Docker-based web services on the `main` branch:

- **cashtran-backend:** built from `cashtran-server/Dockerfile`, running the Spring `prod` profile.
- **cashtran-frontend:** built from `cashtran-frontend/Dockerfile`.

Connect the repository to Render and configure the required environment variables in the Render dashboard or a private environment group. The YAML sets non-secret profile, port, and token-expiration values. The backend needs the production database variables above and a strong, unique `JWT_BASE64_SECRET`. The comments in `render.yaml` mention `DATABASE_URL`, but `application-prod.properties` reads `DB_URL`; configure `DB_URL` (or explicitly map your provider's URL to that name).

The frontend's `VITE_API_URL` must be set to the backend API origin at build time. The configured URL in `render.yaml` is an example; replace it if the Render service URL changes. The frontend Nginx configuration also proxies `/api/` to the configured backend host, so keep `cashtran-frontend/nginx.conf` consistent with the deployed backend.

With `autoDeploy` enabled, commits pushed to the configured branch trigger deployments. You can also deploy either service from its Render page using **Manual Deploy → Deploy latest commit**.

`docker-compose-prod.yml` is a separate Compose deployment definition, not the Render configuration. It includes Splunk logging and has its own required environment variables; verify its Dockerfile paths, database settings, and logging prerequisites before using it.

### Email configuration

Password reset and other email functionality requires working SMTP configuration. Supply the appropriate `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_PASSWORD` through the local or deployment environment, and verify that the SMTP provider allows the connection. Never commit SMTP credentials.

## Observability and Grafana

In the local Compose stack, the backend Docker image starts the OpenTelemetry Java agent. Compose configures it to send OTLP telemetry to the `otel-collector` service. The Collector accepts OTLP over gRPC on port `4317` and HTTP on `4318`, batches telemetry, and exports traces, metrics, and logs using the Grafana Cloud endpoint and credentials in its environment. Keep those credentials in `.env` locally and in the deployment platform's secret manager in hosted environments.

Render's current `render.yaml` sets the backend service name and OTLP protocol, but does not configure a Grafana OTLP endpoint, authentication, or a Collector service. Consequently, the Render configuration alone does **not** complete telemetry export to Grafana Cloud. To observe Render traffic in Grafana, configure a reachable Collector or a supported Grafana OTLP endpoint, set `OTEL_EXPORTER_OTLP_ENDPOINT`, and provide authentication through the agent's supported OTLP headers or the Collector. Keep endpoint credentials in Render's secret environment settings, not in `render.yaml`.

To generate a small amount of safe backend traffic, open PowerShell in the repository root. The Render backend URL currently configured by this project is `https://cashtran-backend.onrender.com`; verify it against the service URL shown in your Render dashboard if it has changed. You can check the health endpoint first:

```powershell
Invoke-WebRequest "https://cashtran-backend.onrender.com/actuator/health"
```

Then run the script against Render:

```powershell
.\scripts\generate-grafana-traffic.ps1 -BackendUrl "https://cashtran-backend.onrender.com" -DurationSeconds 300 -IntervalSeconds 5
```

The script defaults to `http://localhost:8080` for five minutes if `-BackendUrl` is omitted. It sends a small, rate-limited set of GET requests to the API root, health endpoint, balance, and transfers endpoints. The balance and transfers calls are intentionally unauthenticated and may return `401` or `403`; they are read-only and do not create or change account data.

The script accepts only HTTP(S) URLs, limits each run to 30 minutes, and makes no write requests. Sending requests to a hosted service may incur usage or wake a scaled-to-zero service. Allow a short time for telemetry batching/export, then inspect the `cashtran-backend` service in Grafana Explore. The script creates backend HTTP traffic; telemetry will appear only if the Java agent and exporter/Collector are configured and healthy.

## API and diagnostics

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Actuator health: `http://localhost:8080/actuator/health`

Authentication and account registration are under `/api/auth`. Authenticated account and transfer endpoints are under `/api`, including balances, users, immediate sends, requests, request approval/rejection, and PDF statements.

## Security

- Keep `.env`, database connection details, JWT signing secrets, SMTP passwords, and Grafana API keys out of Git and public documentation.
- Use unique secrets per environment and rotate credentials if they were ever committed or shared.
- Restrict production database and actuator access according to your operational requirements.
