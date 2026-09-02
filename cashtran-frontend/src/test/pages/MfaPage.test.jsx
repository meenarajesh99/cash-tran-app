import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, afterEach, vi } from "vitest";

import { MemoryRouter, Routes, Route } from "react-router-dom";

import MfaPage from "../../pages/MfaPage";
import * as api from "../../api/authApi";

// Spy on useNavigate to assert redirects. We mock it here so tests can
// observe navigation calls without relying on the router internals.
const mockedNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

describe("MfaPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it("shows an error when MFA token is missing", async () => {
    render(
      <MemoryRouter initialEntries={["/mfa"]}>
        <Routes>
          <Route path="/mfa" element={<MfaPage />} />
        </Routes>
      </MemoryRouter>,
    );

    // Submit without a token (location.state undefined). The verify button
    // is disabled until a 6-digit code is entered, so type a code first.
    const input = screen.getByLabelText(/Authenticator Code/i);

    await userEvent.type(input, "123456");

    const button = screen.getByRole("button", { name: /verify code/i });

    await userEvent.click(button);

    expect(await screen.findByText(/MFA session is missing/i)).toBeInTheDocument();
  });

  it("submits the code and navigates on success", async () => {
    const verifyMock = vi
      .spyOn(api, "verifyMfaLogin")
      .mockResolvedValue({ data: { token: "real-token", user: { username: "alice" } } });

    // Render with location state containing username and mfaToken
    render(
      <MemoryRouter
        initialEntries={[{ pathname: "/mfa", state: { username: "alice", mfaToken: "token-123" } }]}
      >
        <Routes>
          <Route path="/mfa" element={<MfaPage />} />
        </Routes>
      </MemoryRouter>,
    );

    const input = screen.getByLabelText(/Authenticator Code/i);

    await userEvent.type(input, "123456");

    const button = screen.getByRole("button", { name: /verify code/i });

    await userEvent.click(button);

    await waitFor(() => expect(verifyMock).toHaveBeenCalledWith("token-123", "123456"));

    // localStorage should be populated and navigate called
    await waitFor(() => {
      expect(localStorage.getItem("cashtran_token")).toBe("real-token");
      expect(localStorage.getItem("cashtran_user")).toBe(JSON.stringify({ username: "alice" }));
      expect(mockedNavigate).toHaveBeenCalledWith("/dashboard");
    });
  });
});

