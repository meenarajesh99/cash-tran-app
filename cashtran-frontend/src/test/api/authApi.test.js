import { describe, expect, it, vi } from "vitest";

vi.mock("../../api/axiosClient.js", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

import api from "../../api/axiosClient.js";
import {
  approveTransfer,
  authLogin,
  authRegister,
  createMoneyRequest,
  downloadTransactionHistory,
  rejectTransfer,
  sendTransfer,
} from "../../api/authApi.js";

describe("auth API client", () => {
  it("sends login and registration payloads to the public auth endpoints", () => {
    authLogin("alice", "password");
    authRegister("alice", "password", "alice@example.com");

    expect(api.post).toHaveBeenNthCalledWith(1, "/api/auth/login", {
      username: "alice",
      password: "password",
    });
    expect(api.post).toHaveBeenNthCalledWith(2, "/api/auth/register", {
      username: "alice",
      password: "password",
      email: "alice@example.com",
    });
  });

  it("uses the transfer endpoints and expected request payloads", async () => {
    sendTransfer(42, 12.5);
    createMoneyRequest(8, 20);
    approveTransfer(3);
    rejectTransfer(4);
    downloadTransactionHistory();

    expect(api.post).toHaveBeenNthCalledWith(1, "/api/transfers/send", {
      userId: 42,
      amount: 12.5,
    });
    expect(api.post).toHaveBeenNthCalledWith(2, "/api/requests", {
      userId: 8,
      amount: 20,
    });
    expect(api.put).toHaveBeenNthCalledWith(1, "/api/transfers/3/approve");
    expect(api.put).toHaveBeenNthCalledWith(2, "/api/transfers/4/reject");
    expect(api.get).toHaveBeenCalledWith("/api/transfers/statement", {
      responseType: "blob",
    });
  });
});
