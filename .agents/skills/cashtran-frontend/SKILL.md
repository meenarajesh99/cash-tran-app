---
name: cashtran-frontend
description: Develop and troubleshoot the CashTran React frontend using React, Vite, Material UI, Axios, React Router, and the existing authentication architecture.
---

# CashTran Frontend Skill

## Technology

- React 19
- Vite
- Material UI
- Axios
- React Router

## Architecture

Pages are located in:

src/pages/

Authentication is handled through:

src/auth/AuthProvider.jsx

API communication is handled through:

src/api/

Axios configuration is:

src/api/axiosClient.js

## Authentication

The JWT is stored using:

cashtran_token

Do not introduce a second token storage mechanism.

Use the existing Axios interceptor for authenticated requests.

## Changes

Before modifying a component:

1. Inspect its existing state management.
2. Inspect related API functions.
3. Check routing.
4. Check authentication requirements.
5. Preserve existing Material UI patterns.

After frontend changes:

npm run build

When appropriate:

npm run lint