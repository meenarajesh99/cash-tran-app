import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthContext } from "../../auth/AuthProvider";

const { getUsers, sendTransfer } = vi.hoisted(() => ({
  getUsers: vi.fn(),
  sendTransfer: vi.fn(),
}));

vi.mock("../../api/authApi", () => ({ getUsers, sendTransfer }));

import SendTransfer from "../../pages/SendTransfer";

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user: { id: 1, username: "alice" } }}>
        <SendTransfer />
      </AuthContext.Provider>
    </MemoryRouter>,
  );
}

describe("SendTransfer", () => {
  it("sends money to a selected recipient after confirmation", async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({
      data: [
        { id: 1, username: "alice" },
        { id: 2, username: "bob" },
      ],
    });
    sendTransfer.mockResolvedValue({ data: { transferId: 101 } });

    renderPage();

    await user.click(screen.getByRole("combobox", { name: "Select Recipient" }));
    await user.click(screen.getByRole("option", { name: /bob/i }));
    await user.type(screen.getByLabelText("Amount"), "25");
    await user.click(screen.getByRole("button", { name: "Send Money" }));

    expect(screen.getByRole("dialog", { name: "Confirm Transfer" })).toHaveTextContent(
      "Send $25 to bob?",
    );

    await user.click(screen.getByRole("button", { name: "Confirm" }));

    await waitFor(() => expect(sendTransfer).toHaveBeenCalledWith(2, 25));
    expect(screen.getByText("Transfer successful. Transaction ID: 101")).toBeInTheDocument();
    expect(screen.queryByRole("option", { name: /alice/i })).not.toBeInTheDocument();
  });

  it("requires a recipient before opening confirmation", async () => {
    const user = userEvent.setup();
    getUsers.mockResolvedValue({ data: [{ id: 2, username: "bob" }] });

    renderPage();

    await user.click(screen.getByRole("button", { name: "Send Money" }));

    expect(screen.getByText("Please select a recipient")).toBeInTheDocument();
    expect(sendTransfer).not.toHaveBeenCalled();
  });
});
