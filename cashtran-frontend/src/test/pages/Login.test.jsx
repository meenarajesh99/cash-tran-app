import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AuthContext } from "../../auth/AuthProvider";
import Login from "../../pages/Login";

describe("Login", () => {
  it("shows the authentication error returned by the login action", async () => {
    const user = userEvent.setup();
    const login = vi.fn().mockRejectedValue({ response: { data: "Invalid credentials" } });
    render(<MemoryRouter><AuthContext.Provider value={{ login }}><Login /></AuthContext.Provider></MemoryRouter>);

    await user.type(screen.getByLabelText("Username"), "alice");
    await user.type(screen.getByLabelText("Password"), "wrong-password");
    await user.click(screen.getByRole("button", { name: "Login" }));

    await waitFor(() => expect(login).toHaveBeenCalledWith("alice", "wrong-password"));
    expect(screen.getByText("Invalid credentials")).toBeInTheDocument();
  });
});
