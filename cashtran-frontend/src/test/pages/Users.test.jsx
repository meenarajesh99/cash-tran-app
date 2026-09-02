import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, afterEach, vi } from "vitest";

import { MemoryRouter, Routes, Route } from "react-router-dom";

import UsersPage from "../../pages/Users";
import * as api from "../../api/authApi";

const mockedNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

describe("Users page", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders users from the API and navigates when a user card is clicked", async () => {
    const users = [
      { id: 1, username: "alice" },
      { id: 2, username: "bob" },
    ];

    vi.spyOn(api, "getUsers").mockResolvedValue({ data: users });

    render(
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route path="/" element={<UsersPage />} />
        </Routes>
      </MemoryRouter>,
    );

    // Wait for users to be rendered
    await waitFor(() => {
      expect(screen.getByText("alice")).toBeInTheDocument();
      expect(screen.getByText("bob")).toBeInTheDocument();
    });

    // Click the first user card
    const alice = screen.getByText("alice");

    await userEvent.click(alice);

    // Expect navigation to have been called with the transfer send URL
    expect(mockedNavigate).toHaveBeenCalled();
  });

  it("shows no users found when API returns empty list", async () => {
    vi.spyOn(api, "getUsers").mockResolvedValue({ data: [] });

    render(
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route path="/" element={<UsersPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText(/no users found/i)).toBeInTheDocument();
    });
  });
});

