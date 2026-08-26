import { beforeEach, describe, expect, it, vi } from "vitest";

const { create, use } = vi.hoisted(() => ({ create: vi.fn(), use: vi.fn() }));

vi.mock("axios", () => ({ default: { create } }));

describe("axios client", () => {
  beforeEach(() => {
    create.mockReturnValue({ interceptors: { request: { use }, response: { use } } });
  });

  it("attaches the CashTran token to authenticated requests", async () => {
    localStorage.setItem("cashtran_token", "jwt-token");
    const { default: api } = await import("../../api/axiosClient.js");
    const interceptor = use.mock.calls[0][0];
    const config = interceptor({ headers: {} });

    expect(api).toBeDefined();
    expect(config.headers.Authorization).toBe("Bearer jwt-token");
  });
});
