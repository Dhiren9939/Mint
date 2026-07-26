import { describe, it, expect, vi, beforeEach } from "vitest";
import getDownloadLink from "../getDownloadLink";
import api from "../index";

vi.mock("../index", async () => {
  const actual = await vi.importActual("../index");
  return {
    ...actual,
    default: {
      get: vi.fn(),
    },
  };
});

describe("getDownloadLink API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("calls GET /api/v1/file/{fileCode}", async () => {
    const mockResponse = {
      data: {
        success: true,
        data: {
          fileUrl: "http://s3.download.url",
          expiresAt: "2026-07-26T20:00:00",
          downloadCount: 1,
          maxDownloadCount: 5,
        },
      },
    };
    (api.get as any).mockResolvedValueOnce(mockResponse);

    const result = await getDownloadLink("a1b2c3");

    expect(api.get).toHaveBeenCalledWith("/api/v1/file/a1b2c3");
    expect(result).toEqual(mockResponse);
  });
});
