import { act, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useContext } from "react";

const { authLogin, authRegister } = vi.hoisted(() => ({
  authLogin: vi.fn(),
  authRegister: vi.fn(),
}));

vi.mock("../../api/authApi", () => ({ authLogin, authRegister }));

import { AuthContext, AuthProvider } from "../../auth/AuthProvider.jsx";

function Consumer() {
  const { user, login, logout, register } = useContext(AuthContext);
  return (
    <>
      <span data-testid="username">{user?.username ?? "anonymous"}</span>
      <button onClick={() => login("alice", "password")}>login</button>
      <button onClick={logout}>logout</button>
      <button
        onClick={() => register("alice", "password", "alice@example.com")}
      >
        register
      </button>
    </>
  );
}

describe("AuthProvider", () => {
  it("restores a previously persisted user", () => {
    localStorage.setItem(
      "cashtran_user",
      JSON.stringify({ id: 1, username: "alice" }),
    );
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    );

    expect(screen.getByTestId("username")).toHaveTextContent("alice");
  });

  it("persists the token and user after login, then clears both on logout", async () => {
    authLogin.mockResolvedValue({
      data: { token: "test-token", user: { id: 1, username: "alice" } },
    });
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    );

    await act(async () =>
      screen.getByRole("button", { name: "login" }).click(),
    );

    await waitFor(() =>
      expect(screen.getByTestId("username")).toHaveTextContent("alice"),
    );
    expect(localStorage.getItem("cashtran_token")).toBe("test-token");
    expect(JSON.parse(localStorage.getItem("cashtran_user"))).toEqual({
      id: 1,
      username: "alice",
    });

    await act(async () =>
      screen.getByRole("button", { name: "logout" }).click(),
    );
    expect(screen.getByTestId("username")).toHaveTextContent("anonymous");
    expect(localStorage.getItem("cashtran_token")).toBeNull();
    expect(localStorage.getItem("cashtran_user")).toBeNull();
  });

  it("forwards registration details to the API", async () => {
    authRegister.mockResolvedValue({ data: {} });
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    );

    await act(async () =>
      screen.getByRole("button", { name: "register" }).click(),
    );

    expect(authRegister).toHaveBeenCalledWith(
      "alice",
      "password",
      "alice@example.com",
    );
  });
});
