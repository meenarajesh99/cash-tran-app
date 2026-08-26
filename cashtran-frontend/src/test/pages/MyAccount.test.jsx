import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";

import MyAccount from "../../pages/MyAccount";
import { AuthContext } from "../../auth/AuthProvider";
import { updateEmail } from "../../api/authApi";

vi.mock("../../api/authApi", () => ({
  updateEmail: vi.fn(),
}));

const mockUser = {
  username: "alice",
  email: "alice@example.com",
};

const mockUpdateUser = vi.fn();

function renderMyAccount() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider
        value={{
          user: mockUser,
          updateUser: mockUpdateUser,
        }}
      >
        <MyAccount />
      </AuthContext.Provider>
    </MemoryRouter>,
  );
}

function getEmailInput() {
  return screen.getByRole("textbox", {
    name: /email/i,
  });
}

function getUpdateButton() {
  return screen.getByRole("button", {
    name: "Update Email",
  });
}

describe("MyAccount", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the username and current email", () => {
    renderMyAccount();

    expect(
      screen.getByRole("textbox", {
        name: /username/i,
      }),
    ).toHaveValue("alice");

    expect(getEmailInput()).toHaveValue("alice@example.com");
  });

  it("disables the username field", () => {
    renderMyAccount();

    expect(
      screen.getByRole("textbox", {
        name: /username/i,
      }),
    ).toBeDisabled();
  });

  it("allows the user to change their email", async () => {
    const user = userEvent.setup();

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.type(emailInput, "new@example.com");

    expect(emailInput).toHaveValue("new@example.com");
  });

  it("rejects a blank email without calling updateEmail", async () => {
    const user = userEvent.setup();

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.click(getUpdateButton());

    expect(await screen.findByText("Email is required.")).toBeInTheDocument();

    expect(updateEmail).not.toHaveBeenCalled();
    expect(mockUpdateUser).not.toHaveBeenCalled();
  });

  it("rejects an email containing only whitespace without calling updateEmail", async () => {
    const user = userEvent.setup();

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.type(emailInput, "   ");

    await user.click(getUpdateButton());

    expect(await screen.findByText("Email is required.")).toBeInTheDocument();

    expect(updateEmail).not.toHaveBeenCalled();
    expect(mockUpdateUser).not.toHaveBeenCalled();
  });

  it("trims the email before calling updateEmail", async () => {
    const user = userEvent.setup();

    updateEmail.mockResolvedValue({
      data: {
        username: "alice",
        email: "new@example.com",
      },
    });

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.type(emailInput, "  new@example.com  ");

    await user.click(getUpdateButton());

    await waitFor(() => {
      expect(updateEmail).toHaveBeenCalledWith("new@example.com");
    });
  });

  it("updates the user after a successful email update", async () => {
    const updatedUser = {
      username: "alice",
      email: "new@example.com",
    };

    const user = userEvent.setup();

    updateEmail.mockResolvedValue({
      data: updatedUser,
    });

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.type(emailInput, "new@example.com");

    await user.click(getUpdateButton());

    await waitFor(() => {
      expect(mockUpdateUser).toHaveBeenCalledWith(updatedUser);
    });
  });

  it("displays success message after successful email update", async () => {
    const user = userEvent.setup();

    updateEmail.mockResolvedValue({
      data: {
        username: "alice",
        email: "new@example.com",
      },
    });

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.type(emailInput, "new@example.com");

    await user.click(getUpdateButton());

    expect(
      await screen.findByText("Email updated successfully."),
    ).toBeInTheDocument();
  });

  it("displays API message when updateEmail fails", async () => {
    const user = userEvent.setup();

    updateEmail.mockRejectedValue({
      response: {
        data: {
          message: "Email already registered",
        },
      },
    });

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.type(emailInput, "existing@example.com");

    await user.click(getUpdateButton());

    expect(
      await screen.findByText("Email already registered"),
    ).toBeInTheDocument();

    expect(mockUpdateUser).not.toHaveBeenCalled();
  });

  it("displays response data when API error does not contain a message", async () => {
    const user = userEvent.setup();

    updateEmail.mockRejectedValue({
      response: {
        data: "Invalid email address",
      },
    });

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.type(emailInput, "invalid@example.com");

    await user.click(getUpdateButton());

    expect(
      await screen.findByText("Invalid email address"),
    ).toBeInTheDocument();

    expect(mockUpdateUser).not.toHaveBeenCalled();
  });

  it("displays generic error when API request fails without response data", async () => {
    const user = userEvent.setup();

    updateEmail.mockRejectedValue(new Error("Network error"));

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.type(emailInput, "new@example.com");

    await user.click(getUpdateButton());

    expect(
      await screen.findByText("Unable to update email."),
    ).toBeInTheDocument();

    expect(mockUpdateUser).not.toHaveBeenCalled();
  });

  it("displays the API error when the email is already registered", async () => {
    updateEmail.mockRejectedValue({
      response: {
        status: 409,
        data: {
          message: "Email already registered",
        },
      },
    });
    const user = userEvent.setup();

    renderMyAccount();

    const emailInput = screen.getByRole("textbox", {
      name: /email/i,
    });

    await user.clear(emailInput);
    await user.type(emailInput, "existing@example.com");

    await user.click(screen.getByRole("button", { name: /update email/i }));

    expect(
      await screen.findByText("Email already registered"),
    ).toBeInTheDocument();
  });

  it("disables the update button while the request is in progress", async () => {
    const user = userEvent.setup();

    let resolveRequest;

    updateEmail.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRequest = resolve;
        }),
    );

    renderMyAccount();

    const emailInput = getEmailInput();

    await user.clear(emailInput);
    await user.type(emailInput, "new@example.com");

    await user.click(getUpdateButton());

    expect(getUpdateButton()).toBeDisabled();

    expect(screen.getByRole("progressbar")).toBeInTheDocument();

    resolveRequest({
      data: {
        username: "alice",
        email: "new@example.com",
      },
    });

    await waitFor(() => {
      expect(getUpdateButton()).not.toBeDisabled();
    });

    expect(screen.getByText("Email updated successfully.")).toBeInTheDocument();

    expect(mockUpdateUser).toHaveBeenCalledWith({
      username: "alice",
      email: "new@example.com",
    });
  });

  it("clears a previous error when a new submission starts", async () => {
    const user = userEvent.setup();

    updateEmail
      .mockRejectedValueOnce({
        response: {
          data: {
            message: "Email already registered",
          },
        },
      })
      .mockResolvedValueOnce({
        data: {
          username: "alice",
          email: "new@example.com",
        },
      });

    renderMyAccount();

    const emailInput = getEmailInput();

    // First submission
    await user.clear(emailInput);
    await user.type(emailInput, "existing@example.com");

    await user.click(getUpdateButton());

    expect(
      await screen.findByText("Email already registered"),
    ).toBeInTheDocument();

    // Second submission
    await user.clear(emailInput);
    await user.type(emailInput, "new@example.com");

    await user.click(getUpdateButton());

    expect(
      await screen.findByText("Email updated successfully."),
    ).toBeInTheDocument();

    expect(
      screen.queryByText("Email already registered"),
    ).not.toBeInTheDocument();

    expect(mockUpdateUser).toHaveBeenCalledWith({
      username: "alice",
      email: "new@example.com",
    });
  });
});
