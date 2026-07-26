import { describe, it, expect, vi, beforeEach } from "vitest";
import uploadFile from "../uploadFile";
import axios from "axios";

vi.mock("axios", () => ({
  default: {
    put: vi.fn(),
  },
}));

describe("uploadFile API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("calls axios.put with file and custom content-type header", async () => {
    (axios.put as any).mockResolvedValueOnce({ status: 200 });

    const dummyFile = new File(["test data"], "test.pdf", {
      type: "application/pdf",
    });

    await uploadFile(dummyFile, "http://s3.upload.url", "application/pdf");

    expect(axios.put).toHaveBeenCalledWith(
      "http://s3.upload.url",
      dummyFile,
      {
        headers: {
          "Content-Type": "application/pdf",
        },
      },
    );
  });
});
