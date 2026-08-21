import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthContext } from "../../auth/AuthProvider";

const { getBalance, getTransfers, getPendingSentTransfers, getPendingReceivedTransfers, approveTransfer, rejectTransfer } = vi.hoisted(() => ({
  getBalance: vi.fn(), getTransfers: vi.fn(), getPendingSentTransfers: vi.fn(), getPendingReceivedTransfers: vi.fn(), approveTransfer: vi.fn(), rejectTransfer: vi.fn(),
}));
vi.mock("../../api/authApi", () => ({ getBalance, getTransfers, getPendingSentTransfers, getPendingReceivedTransfers, approveTransfer, rejectTransfer }));
import Dashboard from "../../pages/Dashboard";

function setDashboardData() {
  getBalance.mockResolvedValue({ data: 50 });
  getTransfers.mockResolvedValue({ data: [] });
  getPendingSentTransfers.mockResolvedValue({ data: [] });
  getPendingReceivedTransfers.mockResolvedValue({ data: [{ transferId: 8, transferTypeDesc: "Request", transferStatusDesc: "Pending", accountFromUsername: "bob", accountToUsername: "alice", amount: 20 }] });
}

describe("Dashboard", () => {
  it("approves and rejects requests received by the logged-in user, then refreshes", async () => {
    const user = userEvent.setup();
    setDashboardData(); approveTransfer.mockResolvedValue({}); rejectTransfer.mockResolvedValue({});
    render(<MemoryRouter><AuthContext.Provider value={{ user: { username: "alice" }, logout: vi.fn() }}><Dashboard /></AuthContext.Provider></MemoryRouter>);

    await screen.findByText("Transfer #8");
    await user.click(screen.getByRole("button", { name: "Approve" }));
    await waitFor(() => expect(approveTransfer).toHaveBeenCalledWith(8));
    await user.click(screen.getByRole("button", { name: "Reject" }));
    await waitFor(() => expect(rejectTransfer).toHaveBeenCalledWith(8));
    expect(getBalance.mock.calls.length).toBeGreaterThanOrEqual(3);
  });

  it("shows an error when dashboard data cannot load", async () => {
    getBalance.mockRejectedValue({ response: { data: "Balance unavailable" } });
    render(<MemoryRouter><AuthContext.Provider value={{ user: { username: "alice" }, logout: vi.fn() }}><Dashboard /></AuthContext.Provider></MemoryRouter>);

    expect(await screen.findByText("Balance unavailable")).toBeInTheDocument();
  });
});
