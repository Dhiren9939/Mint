import { describe, it, expect, vi, beforeEach } from "vitest";
import confirmUpload from "../confirmUpload";
import api from "../index";

vi.mock("../index", async () => {
  const actual = await vi.importActual("../index");
  return {
    ...actual,
    default: {
      patch: vi.fn(),
    },
  };
});

describe("confirmUpload API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("sends fileKey and fileCode to /api/v1/file", async () => {
    const mockResponse = {
      data: {
        success: true,
        data: {
          fileCode: "a1b2c3",
          expiresAt: "2026-07-26T20:00:00",
          maxDownloadCount: 5,
          fileState: "READY",
        },
      },
    };
    vi.mocked(api.patch).mockResolvedValueOnce(mockResponse);

    const result = await confirmUpload("uploads/key.txt", "a1b2c3");

    expect(api.patch).toHaveBeenCalledWith("/api/v1/file", {
      fileKey: "uploads/key.txt",
      fileCode: "a1b2c3",
    });
    expect(result).toEqual(mockResponse);
  });
});
