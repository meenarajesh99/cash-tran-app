import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, test, vi } from "vitest";
import ForgotPassword from "../../pages/ForgotPassword";
import * as authApi from "../../api/authApi";
import { MemoryRouter } from "react-router-dom";

function renderPage() {
  return render(
    <MemoryRouter>
      <ForgotPassword />
    </MemoryRouter>,
  );
}

vi.mock("../../api/authApi");

describe("Forgot Password", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("forgotPasswordSubmitsEmail", async () => {
    const user = userEvent.setup();

    authApi.forgotPassword.mockResolvedValue({
      data: { message: "Password reset email sent" },
    });

    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>,
    );
    const emailInput = screen.getByLabelText(/email/i);
    const submitButton = screen.getByRole("button", {
      name: /forgot password|reset password|submit/i,
    });

    await user.type(emailInput, "test@example.com");
    await user.click(submitButton);

    await waitFor(() => {
      expect(authApi.forgotPassword).toHaveBeenCalledWith("test@example.com");
    });
  });

  test("forgotPasswordDisplaysSuccessMessage", async () => {
    const user = userEvent.setup();

    authApi.forgotPassword.mockResolvedValue({
      data: {
        message: "Password reset email sent",
      },
    });

    renderPage();

    await user.type(screen.getByLabelText(/email/i), "test@example.com");

    await user.click(
      screen.getByRole("button", {
        name: /reset password/i,
      }),
    );

    expect(
      await screen.findByText(
        /if an account exists with this email, password reset instructions will be sent/i,
      ),
    ).toBeInTheDocument();
  });

  test("forgotPasswordDisplaysApiError", async () => {
    const user = userEvent.setup();

    authApi.forgotPassword.mockRejectedValue({
      response: {
        data: {
          message: "Email address not found",
        },
      },
    });

    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText(/email/i), "unknown@example.com");

    await user.click(
      screen.getByRole("button", {
        name: /forgot password|reset password|submit/i,
      }),
    );

    expect(
      await screen.findByText(/email address not found/i),
    ).toBeInTheDocument();
  });
});
