import React from "react";
import { render, screen, cleanup } from "@testing-library/react";
import { describe, expect, it, beforeEach, afterEach, vi } from "vitest";
import { AuthContext } from "../auth/AuthProvider";

vi.mock("../pages/Login", () => ({
  default: () => <div>Login page</div>,
}));

vi.mock("../pages/Register", () => ({
  default: () => <div>Register page</div>,
}));

vi.mock("../pages/Dashboard", () => ({
  default: () => <div>Dashboard page</div>,
}));

vi.mock("../pages/ForgotPassword", () => ({
  default: () => <div>Forgot Password page</div>,
}));

vi.mock("../pages/ResetPassword", () => ({
  default: () => <div>Reset Password page</div>,
}));

vi.mock("../pages/Users", () => ({
  default: () => <div>Users page</div>,
}));

vi.mock("../pages/Transfers", () => ({
  default: () => <div>Transfers page</div>,
}));

vi.mock("../pages/SendTransfer", () => ({
  default: () => <div>Send page</div>,
}));

vi.mock("../pages/RequestMoney", () => ({
  default: () => <div>Request page</div>,
}));

import App from "../App";

function renderApp(user) {
  return render(
    <AuthContext.Provider value={{ user }}>
      <App />
    </AuthContext.Provider>,
  );
}

describe("App routes", () => {
  beforeEach(() => {
    window.history.pushState({}, "", "/");
  });

  afterEach(() => {
    cleanup();
  });

  it("redirects unauthenticated users away from protected routes", () => {
    window.history.pushState({}, "", "/dashboard");

    renderApp(null);

    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("redirects authenticated users away from public routes", () => {
    window.history.pushState({}, "", "/login");

    renderApp({
      id: 1,
      username: "alice",
    });

    expect(screen.getByText("Dashboard page")).toBeInTheDocument();
  });
});
