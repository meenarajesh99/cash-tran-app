My CashTran project currently has little or no automated test coverage.

I want to establish the initial automated test suite for the existing application.

DO NOT immediately start changing production code.

First perform a complete testing audit.

Backend:
- Inspect the Spring Boot project structure.
- Identify controllers and REST endpoints.
- Identify services and business logic.
- Identify repositories and custom queries.
- Identify security/JWT/authentication logic.
- Identify important DTOs and validation.
- Identify existing backend tests, if any.

Frontend:
- Inspect the React/Vite project structure.
- Identify pages/components.
- Identify API modules.
- Identify authentication/state-management logic.
- Identify important user interactions.
- Identify existing frontend tests, if any.
- Determine the testing framework already configured.

Then create a prioritized test plan.

Prioritize these CashTran areas:

1. Registration
2. Login/authentication
3. JWT/security
4. Account/balance
5. Send transfer
6. Request money
7. Approve request
8. Reject request
9. Transfer history
10. Pending transfers
11. Users
12. Statement generation
13. Important frontend authentication behavior
14. Dashboard behavior
15. Send/request money user interactions

After the audit, implement the tests.

For backend:
- Use the project's existing testing framework.
- Add unit tests for business logic.
- Add repository tests where repository behavior is important.
- Add controller/API tests for important REST endpoints.
- Add integration tests where multiple layers need to be verified.
- Include success, validation, authorization, error, and boundary cases where appropriate.

For frontend:
- Use the project's existing testing framework.
- Test important component behavior and user interactions.
- Test API interaction behavior where appropriate.
- Test authentication behavior.
- Test error states and validation.

Do not modify production behavior just to make tests pass.

After creating the tests:

1. Run the backend test suite.
2. Run the frontend test suite.
3. Run the frontend production build.
4. Fix test/setup problems where appropriate.
5. Report:
    - Production files changed
    - Test files created
    - Test files updated
    - Tests executed
    - Tests passing
    - Tests failing
    - Remaining test gaps

Do not claim the project is fully tested simply because the test suite passes. Identify areas that still lack meaningful coverage.