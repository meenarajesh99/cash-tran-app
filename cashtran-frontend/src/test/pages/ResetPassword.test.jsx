import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, beforeEach, test, expect, vi } from "vitest";
import ResetPassword from "../../pages/ResetPassword";
import * as authApi from "../../api/authApi";
import { MemoryRouter } from "react-router-dom";

vi.mock("../../api/authApi", () => ({
  resetPassword: vi.fn(),
}));

describe("Reset Password", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    window.history.pushState({}, "", "/reset-password?token=test-reset-token");
  });

  test("resetPasswordSubmitsTokenAndNewPassword", async () => {
    const user = userEvent.setup();

    authApi.resetPassword.mockResolvedValue({
      data: { message: "Password reset successful" },
    });

    render(
      <MemoryRouter initialEntries={["/reset-password?token=test-reset-token"]}>
        <ResetPassword />
      </MemoryRouter>,
    );

    const passwordInputs = screen.getAllByLabelText(/password/i);

    await user.type(passwordInputs[0], "NewPassword123!");
    await user.type(passwordInputs[1], "NewPassword123!");

    await user.click(
      screen.getByRole("button", {
        name: /reset password|submit/i,
      }),
    );

    await waitFor(() => {
      expect(authApi.resetPassword).toHaveBeenCalledWith(
        "test-reset-token",
        "NewPassword123!",
      );
    });
  });

  test("resetPasswordRejectsMismatchedPasswords", async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={["/reset-password?token=test-reset-token"]}>
        <ResetPassword />
      </MemoryRouter>,
    );

    const passwordInputs = screen.getAllByLabelText(/password/i);

    await user.type(passwordInputs[0], "NewPassword123!");
    await user.type(passwordInputs[1], "DifferentPassword123!");

    await user.click(
      screen.getByRole("button", {
        name: /reset password|submit/i,
      }),
    );

    expect(
      await screen.findByText(/passwords do not match/i),
    ).toBeInTheDocument();

    expect(authApi.resetPassword).not.toHaveBeenCalled();
  });
});
