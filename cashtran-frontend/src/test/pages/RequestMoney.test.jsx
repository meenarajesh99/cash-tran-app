import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthContext } from "../../auth/AuthProvider";

const { getUsers, createMoneyRequest } = vi.hoisted(() => ({
  getUsers: vi.fn(),
  createMoneyRequest: vi.fn(),
}));

vi.mock("../../api/authApi", () => ({ getUsers, createMoneyRequest }));

import RequestMoney from "../../pages/RequestMoney";

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user: { id: 1, username: "alice" } }}>
        <RequestMoney />
      </AuthContext.Provider>
    </MemoryRouter>,
  );
}

describe("RequestMoney", () => {
  it("creates a money request for another user", async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({
      data: [
        { id: 1, username: "alice" },
        { id: 2, username: "bob" },
      ],
    });
    createMoneyRequest.mockResolvedValue({ data: { transferId: 102 } });

    renderPage();

    await user.click(screen.getByRole("combobox", { name: "Select User" }));
    await user.click(screen.getByRole("option", { name: "bob" }));
    await user.type(screen.getByRole("spinbutton", { name: /amount/i }), "20");
    await user.click(screen.getByRole("button", { name: "Request Money" }));

    await waitFor(() => expect(createMoneyRequest).toHaveBeenCalledWith(2, 20));
    expect(screen.getByText("Money request created successfully!")).toBeInTheDocument();
  });

  it("rejects a non-positive request amount without calling the API", async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ data: [{ id: 2, username: "bob" }] });

    renderPage();

    await user.click(screen.getByRole("combobox", { name: "Select User" }));
    await user.click(screen.getByRole("option", { name: "bob" }));
    await user.type(screen.getByRole("spinbutton", { name: /amount/i }), "0");
    await user.click(screen.getByRole("button", { name: "Request Money" }));

    expect(screen.getByText("Amount must be greater than zero")).toBeInTheDocument();
    expect(createMoneyRequest).not.toHaveBeenCalled();
  });
});
