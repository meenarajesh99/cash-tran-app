import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";

const { getTransfers, downloadTransactionHistory } = vi.hoisted(() => ({ getTransfers: vi.fn(), downloadTransactionHistory: vi.fn() }));
vi.mock("../../api/authApi", () => ({ getTransfers, downloadTransactionHistory }));
import Transfers from "../../pages/Transfers";

describe("Transfers", () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => "blob:statement");
    URL.revokeObjectURL = vi.fn();
  });

  it("filters transfer history and downloads a statement", async () => {
    const user = userEvent.setup();
    getTransfers.mockResolvedValue({ data: [{ transferId: 8, accountTo: "bob", amount: 20, transferStatusDesc: "Completed" }, { transferId: 9, accountTo: "carol", amount: 10, transferStatusDesc: "Pending" }] });
    downloadTransactionHistory.mockResolvedValue({ data: new Blob(["statement"]) });
    render(<MemoryRouter><Transfers /></MemoryRouter>);

    await screen.findByText("#8");
    await user.type(screen.getByPlaceholderText("Search transfers..."), "9");
    expect(screen.queryByText("#8")).not.toBeInTheDocument();
    expect(screen.getByText("#9")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Download Statement" }));
    await waitFor(() => expect(downloadTransactionHistory).toHaveBeenCalled());
    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:statement");
  });
});
