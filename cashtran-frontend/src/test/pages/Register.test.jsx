import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthContext } from "../../auth/AuthProvider";
import Register from "../../pages/Register";

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

function renderPage(register) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ register }}>
        <Register />
      </AuthContext.Provider>
    </MemoryRouter>,
  );
}

function getPasswordInputs() {
  const inputs = document.querySelectorAll('input[type="password"]');

  expect(inputs).toHaveLength(2);

  return inputs;
}

describe("Register", () => {
  it("blocks mismatched passwords before calling the API", async () => {
    const user = userEvent.setup();
    const register = vi.fn();

    renderPage(register);

    await user.type(
      screen.getByRole("textbox", { name: /username/i }),
      "alice",
    );

    await user.type(
      screen.getByRole("textbox", { name: /email/i }),
      "alice@example.com",
    );

    const passwordInputs = getPasswordInputs();

    await user.type(passwordInputs[0], "Password1!");
    await user.type(passwordInputs[1], "Different1!");

    await user.click(screen.getByRole("button", { name: /^register$/i }));

    expect(
      await screen.findByText("Passwords do not match"),
    ).toBeInTheDocument();

    expect(register).not.toHaveBeenCalled();
  });

  it("submits valid registration details and shows confirmation", async () => {
    const user = userEvent.setup();
    const register = vi.fn().mockResolvedValue({});

    renderPage(register);

    await user.type(
      screen.getByRole("textbox", { name: /username/i }),
      "alice",
    );

    await user.type(
      screen.getByRole("textbox", { name: /email/i }),
      "alice@example.com",
    );

    const passwordInputs = getPasswordInputs();

    await user.type(passwordInputs[0], "Password1!");
    await user.type(passwordInputs[1], "Password1!");

    await user.click(screen.getByRole("button", { name: /^register$/i }));

    await waitFor(() => {
      expect(register).toHaveBeenCalledWith(
        "alice",
        "Password1!",
        "alice@example.com",
      );
    });

    expect(
      screen.getByText("Registration successful. Redirecting to login..."),
    ).toBeInTheDocument();
  });
});
