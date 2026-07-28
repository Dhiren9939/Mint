import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import RecieveCard from "../RecieveCard";
import getDownloadLink from "../../api/getDownloadLink";
import toast from "react-hot-toast";

vi.mock("../../api/getDownloadLink");
vi.mock("react-hot-toast", () => ({
  default: {
    error: vi.fn(),
    loading: vi.fn().mockReturnValue("toast-id"),
    success: vi.fn(),
  },
}));

describe("RecieveCard Component", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders input field and download button initially", () => {
    render(<RecieveCard />);

    expect(screen.getByPlaceholderText("0a9z2x")).toBeInTheDocument();
    const submitBtn = screen.getByRole("button", { name: /Download File/i });
    expect(submitBtn).toBeDisabled();
  });

  it("shows error toast if file code is invalid pattern", async () => {
    render(<RecieveCard />);

    const input = screen.getByPlaceholderText("0a9z2x");
    fireEvent.change(input, { target: { value: "short" } });

    const submitBtn = screen.getByRole("button", { name: /Download File/i });
    expect(submitBtn).not.toBeDisabled();

    fireEvent.click(submitBtn);

    expect(toast.error).toHaveBeenCalledWith("Enter a valid file code.");
    expect(getDownloadLink).not.toHaveBeenCalled();
  });

  it("calls getDownloadLink and window.open on valid code submission", async () => {
    const windowOpenSpy = vi.spyOn(window, "open").mockImplementation(() => null);

    vi.mocked(getDownloadLink).mockResolvedValueOnce({
      data: {
        success: true,
        message: "Success",
        data: {
          fileUrl: "http://s3.download.url/file.pdf",
          expiresAt: "2026-07-26T20:00:00",
          dowloadCount: 1,
          maxDownloadCount: 5,
        },
      },
      status: 200,
      statusText: "OK",
      headers: {},
      config: {} as never,
    });

    render(<RecieveCard />);

    const input = screen.getByPlaceholderText("0a9z2x");
    fireEvent.change(input, { target: { value: "a1b2c3" } });

    const submitBtn = screen.getByRole("button", { name: /Download File/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(getDownloadLink).toHaveBeenCalledWith("a1b2c3");
      expect(windowOpenSpy).toHaveBeenCalledWith("http://s3.download.url/file.pdf");
      expect(toast.success).toHaveBeenCalledWith("File downloaded.", { id: "toast-id" });
    });

    windowOpenSpy.mockRestore();
  });
});
