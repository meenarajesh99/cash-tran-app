---
name: cashtran-backend
description: Develop and troubleshoot the CashTran Spring Boot backend using the project's existing architecture, JPA, PostgreSQL, Flyway, JWT, and REST conventions.
---

# CashTran Backend Skill

## Technology

- Java 21
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JJWT
- Maven

## Before making changes

1. Inspect the existing implementation.
2. Identify the controller, service, repository, entity, DTO, and migration involved.
3. Understand existing authentication and authorization behavior.
4. Do not introduce a new dependency unless necessary.
5. Explain significant architectural changes before implementing them.

## REST API changes

When adding or modifying an endpoint:

1. Inspect existing controller conventions.
2. Check whether authentication is required.
3. Check existing DTO patterns.
4. Check repository methods and database relationships.
5. Implement the smallest change necessary.
6. Add or update tests.
7. Run the Maven build.

## Database changes

Never modify the database schema directly for a persistent application change.

Use Flyway migrations.

Migration files belong in:

src/main/resources/db/migration/

Follow the existing naming convention:

V#__description.sql

Before creating a migration:

1. Inspect the current schema.
2. Check existing Flyway migrations.
3. Check the corresponding JPA entity.
4. Explain the schema change.

## Security

Never expose:

- JWT secrets
- database passwords
- API keys
- credentials
- environment secrets

Do not commit secrets.

Preserve existing JWT authentication unless the task specifically requires changing it.

## Testing

After backend changes:

./mvnw clean package

When appropriate, also run:

./mvnw test

Report:

- tests executed
- tests passed
- tests failed
- build result