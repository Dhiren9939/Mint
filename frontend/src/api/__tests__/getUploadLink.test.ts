import { describe, it, expect, vi, beforeEach } from "vitest";
import getUploadLink from "../getUploadLink";
import api, { ExpiryDuration } from "../index";

vi.mock("../index", async () => {
  const actual = await vi.importActual("../index");
  return {
    ...actual,
    default: {
      post: vi.fn(),
    },
  };
});

describe("getUploadLink API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("sends correct request payload to /api/v1/file/upload", async () => {
    const mockResponse = {
      data: {
        success: true,
        data: {
          fileUrl: "http://s3.upload.url",
          fileCode: "a1b2c3",
          fileKey: "uploads/key.txt",
          expiresAt: "2026-07-26T20:00:00",
          maxDownloadCount: 5,
          fileState: "PENDING",
        },
      },
    };
    vi.mocked(api.post).mockResolvedValueOnce(mockResponse);

    const dummyFile = new File(["hello world"], "hello.txt", {
      type: "text/plain",
    });
    const result = await getUploadLink(dummyFile, ExpiryDuration.MINUTES15, 5);

    expect(api.post).toHaveBeenCalledWith("/api/v1/file/upload", {
      fileName: "hello.txt",
      contentType: "application/octet-stream",
      contentSize: 11,
      expiryDuration: "MINUTES15",
      maxDownloadCount: 5,
    });
    expect(result).toEqual(mockResponse);
  });
});
