# AGENTS.md - Cash Tran Development Guide

## Project Overview

**Cash Tran** is a peer-to-peer payment system for transferring "CT Bucks" between users. It's a full-stack application with:
- **Frontend**: React 19 + Vite (port 3000)
- **Backend**: Spring Boot 3.5 + PostgreSQL (port 8080)
- **Database**: PostgreSQL with Flyway migrations
- **Deployment**: Docker Compose orchestration

The system implements two transfer workflows: **Send** (immediate) and **Request** (pending approval).

## Architecture Patterns

### Core Data Flow
1. **JWT Authentication**: User login → Backend returns JWT token → Frontend stores in `localStorage` → Axios interceptor auto-attaches to all API requests
2. **Transfer Model**: Two types (Send/Request) × Three statuses (Pending/Approved/Rejected) = account balance updates on approval
3. **Account Isolation**: User account balance tied to one-to-one relationship with `cashtran_user` via `account` table

### Request/Response Pattern
- Controllers receive `Principal principal` for current authenticated user via Spring Security
- Extract username → lookup user ID via `UserRepository.findIdByUsername(username)`
- All authenticated endpoints have `@PreAuthorize("isAuthenticated()")` class-level annotation
- DTOs used for API contracts; models for domain logic

### Service Layer Approach
- **Repositories** contain business logic (e.g., `TransferRepository.newTransfer()`, `TransferRepository.acceptRequest()`)
- **Services** handle cross-cutting concerns (e.g., `AuthService` for token generation)
- Direct repository access from controllers without intermediate service layer is common pattern here

## Critical Developer Workflows

### Build & Run Backend
```bash
cd cashtran-server
./mvnw clean package          # Full build with tests
./mvnw spring-boot:run       # Run locally (needs PostgreSQL at localhost:5432)
```
- Maven wrapper included; no global Maven required
- Application profiles: `local`, `docker`, `neon`, `prod` (set via `SPRING_PROFILES_ACTIVE`)
- Environment variables override `application.properties` (e.g., `DB_HOST`, `JWT_BASE64_SECRET`)

### Build & Run Frontend
```bash
cd cashtran-frontend
npm install
npm run dev                   # Vite dev server with HMR
npm run build                # Production build
npm run lint                 # ESLint check
npm run format               # Prettier formatting
```
- Vite automatically picks up `VITE_API_URL` env var (defaults to `http://localhost:8080`)
- Token stored as `cashtran_token` in `localStorage`

### Docker Local Development
```bash
# Root directory docker-compose.yml brings up full stack
docker-compose up --build    # First run; includes DB migrations via Flyway

# View logs
docker-compose logs -f backend
docker-compose logs -f db
```
- Database port: 5433 (mapped from container 5432) to avoid conflicts
- DB_PASSWORD must be set as env var; no default provided
- Health check on DB waits for `pg_isready` before backend starts

### Database Migrations
- Flyway enabled by default (`spring.flyway.enabled=true`)
- SQL files in `src/main/resources/db/migration/` with naming pattern `V#__*.sql`
- `V1__after_jpa.sql` creates all core tables and sequences
- To add new migrations: create `V2__*.sql` in the directory; Flyway auto-runs on startup

### Debugging & Testing
- SLF4J logging configured at DEBUG level for `com.perscholas` package
- Swagger UI available at `http://localhost:8080/swagger-ui.html` (configured in `application.properties`)
- Test profile: `application-test.properties` with in-memory config
- Repository methods return domain objects directly; no ORM lazy-loading gotchas

## Project-Specific Conventions

### API Endpoint Structure
- Auth endpoints: `/api/auth/login`, `/api/auth/register` (public)
- User data: `/api/users`, `/api/username/{accountId}`, `/api/account/{id}`
- Account: `/api/balance` (returns user's balance)
- Transfers: `/api/transfers` (list), `/api/transfers/{id}` (detail), `/api/transfers/send` (send money)
- Requests: `/api/requests` (create), `/api/transfer/{transferId}/accept`, `/api/transfer/{transferId}/reject`

### Frontend Component Structure
- **Pages** in `src/pages/`: Dashboard, Login, Register, SendTransfer, Transfers, Users
- **Auth context** in `src/auth/AuthProvider.jsx`: wraps app, manages JWT state
- **Route protection**: `ProtectedRoute` component requires authenticated user; redirects to `/login` if missing
- **API layer** in `src/api/`: `authApi.js` exports functions (e.g., `sendTransfer(userId, amount)`), `axiosClient.js` handles interceptor

### Recent Frontend fixes (2026-08-25)

- Summary: lint and unit-test failures in the frontend were fixed so `npm run lint` reports no errors and all frontend tests pass locally.
- Commands run (frontend folder):
  - `npm run lint`
  - `npm test`

- Key changes made (files updated):
  - `src/test/api/axiosClient.test.js` — mock `axios.create()` to return both `request` and `response` interceptor objects so the real client can register both interceptors in tests.
  - `src/pages/MyAccount.jsx` — surface plain-string API error responses (when `response.data` is a string) so the UI and tests show the exact server message.
  - `src/auth/AuthProvider.jsx` — use named React hook imports and add a small ESLint exception to avoid a fast-refresh rule; preserved existing behavior.
  - `src/pages/*` (Login, Dashboard, SendTransfer, RequestMoney, Transfers, Users, ResetPassword, ForgotPassword) — replace default React imports with named hook imports, convert `React.useContext` to `useContext`, and wrap/inline async calls inside `useEffect` (async IIFE) to satisfy ESLint rules about calling setState in effects and exhaustive-deps.
  - Test files updated to match helpers and to use `userEvent.setup()` consistently (`src/test/pages/MyAccount.test.jsx`, `src/test/auth/AuthProvider.test.jsx`, `src/test/App.test.jsx`, `src/test/pages/ForgotPassword.test.jsx`).

- Result: After these changes the frontend test run completed with all tests passing (37 tests), and ESLint reported no errors (a few runtime React warnings remain during tests — see "Remaining issues" below).

- Remaining issues / notes:
  - Several tests emit React runtime warnings about props forwarded to DOM elements (e.g., `textAlign`, `justifyContent`, `InputProps`, `inputProps`, `alignItems`) coming from Material UI usage. These are warnings and do not fail tests, but the UI code can be adjusted to avoid forwarding those props to plain DOM nodes (use `sx` or ensure props are applied to MUI components rather than native DOM elements).
  - There are a few console.log statements retained in components used in tests for debuggability. If you prefer a cleaner test output, we can remove or gate these logs behind a development-only flag.
  - `AuthProvider.jsx` includes a small `eslint-disable` for the fast-refresh rule; an alternative is to move the context into its own file to satisfy the rule without disabling it.


### Backend Model Naming
- Table: `cashtran_user`; Entity: `User`
- Tables use snake_case; columns map to camelCase Java properties via JPA
- Sequences for ID generation start at 1001 (users), 2001 (accounts) to leave room for system data
- All transfer validations (non-zero amount, sufficient balance) enforced in repository layer

### Database Constraints
- Check constraints on `transfer` table prevent negative amounts and ensure `account_from ≠ account_to`
- See raw DDL in `cashtran.sql` for exact constraint definitions
- Foreign keys enforce referential integrity; deletions cascade or fail based on constraint config

## Integration Points & External Dependencies

### JWT Token Management
- Generated by `AuthService.login()` using JJWT library (versions 0.12.1)
- Tokens signed with base64-encoded secret (`JWT_BASE64_SECRET` env var)
- Expiration: 24 hours default (86400 seconds), 7 days for "remember me" (604800)
- Backend validates token in request header `Authorization: Bearer <token>`

### Cross-Origin & API Integration
- Frontend makes requests to backend via `VITE_API_URL` (injected at build time)
- Axios client timeout: 10 seconds
- Token auto-attached via interceptor; no manual header management needed in API calls

### Microservice-Ready Structure
- Each domain (Auth, Account, Transfer) has dedicated controller, though currently monolithic
- Repositories define service boundaries; could be extracted to separate modules
- DTOs shield internal model changes from API consumers

## Common Pitfalls & Debugging Tips

1. **Token Expires Silently**: Frontend doesn't auto-refresh JWT. On 401, redirect to login manually in axios error interceptor.
2. **Database Sequence Gaps**: If inserts fail, check sequences haven't been manually reset or reached `NO MAXVALUE` boundary.
3. **Missing Environment Variables**: Backend fails silently if `JWT_BASE64_SECRET` not set; logs won't show why token creation fails.
4. **CORS Issues**: Check `VITE_API_URL` matches actual backend URL; frontend Vite proxy setup not configured (direct axios calls expected).
5. **Flyway Lock**: If migrations hang, check `flyway_schema_history` table isn't locked in `pg_isready` state.

## Key Files for Understanding Critical Behavior

| File                                                                                       | Purpose |
|--------------------------------------------------------------------------------------------|---------|
| `cashtran-server/pom.xml`                                                                  | Maven dependencies & build config; note Spring Boot 3.5, JJWT 0.12.1 |
| `cashtran-server/src/main/resources/application.properties`                                | Environment-driven config; profiles determine DB/JWT settings |
| `cashtran-server/src/main/java/com/perscholas/cashtran/controller/AppController.java`      | Core API logic for transfers/balance; demonstrates Principal usage |
| `cashtran-server/src/main/java/com/perscholas/cashtran/repository/TransferRepository.java` | Interface defining transfer/request workflows; implementations handle account balance updates |
| `cashtran-frontend/src/api/axiosClient.js`                                                 | Token interceptor; critical for understanding JWT flow to backend |
| `cashtran-frontend/src/auth/AuthProvider.jsx`                                              | State management for logged-in user; controls ProtectedRoute access |
| `docker-compose.yml`                                                                       | Service orchestration; sets env vars for all components; dependency ordering |
| `cashtran-server/src/main/resources/db/migration/V1__after_jpa.sql`                        | DB schema; defines `cashtran_user`, `account`, `transfer` tables with constraints |

## AI Coding Agent Rules

- Do not expose, print, commit, or modify secrets such as JWT secrets,
  database passwords, API keys, or credentials.
- Before making significant changes, explain the proposed approach.
- When modifying existing code, preserve the established architecture unless
  there is a clear reason to improve it. If proposing an architectural change,
  explain the tradeoffs first.
- Prefer existing project patterns and dependencies over introducing new libraries.
- Do not change database schema without creating or explaining the appropriate Flyway migration.
- Run backend tests after backend changes.
- Run `npm run build` after frontend changes.
- Do not remove existing functionality unless explicitly requested.
- Preserve existing JWT and Spring Security behavior unless the task specifically
  requires changing authentication or authorization.
- Check the existing implementation before creating duplicate functionality.
- When fixing a bug, identify the root cause before modifying code.
- After making changes, summarize:
    1. Files changed
    2. What changed
    3. Tests/build commands executed
    4. Any remaining issues

## Testing Requirement

Every new feature, bug fix, refactor, or behavior change must include
appropriate automated tests or updates to existing tests.