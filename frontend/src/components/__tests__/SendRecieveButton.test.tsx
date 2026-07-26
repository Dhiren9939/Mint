import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import SendRecieveButton from "../SendRecieveButton";

describe("SendRecieveButton Component", () => {
  it("renders Send and Recieve buttons and appropriate description text", () => {
    const setIsSendMock = vi.fn();
    render(<SendRecieveButton isSend={true} setIsSend={setIsSendMock} />);

    expect(screen.getByRole("button", { name: "Send" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Recieve" })).toBeInTheDocument();
    expect(
      screen.getByText("Upload and share a temporary code."),
    ).toBeInTheDocument();
  });

  it("calls setIsSend(true) when Send button is clicked", () => {
    const setIsSendMock = vi.fn();
    render(<SendRecieveButton isSend={false} setIsSend={setIsSendMock} />);

    fireEvent.click(screen.getByRole("button", { name: "Send" }));
    expect(setIsSendMock).toHaveBeenCalledWith(true);
  });

  it("calls setIsSend(false) when Recieve button is clicked", () => {
    const setIsSendMock = vi.fn();
    render(<SendRecieveButton isSend={true} setIsSend={setIsSendMock} />);

    fireEvent.click(screen.getByRole("button", { name: "Recieve" }));
    expect(setIsSendMock).toHaveBeenCalledWith(false);
  });

  it("displays receive mode description when isSend is false", () => {
    const setIsSendMock = vi.fn();
    render(<SendRecieveButton isSend={false} setIsSend={setIsSendMock} />);

    expect(
      screen.getByText("Enter a code to retrieve shared files."),
    ).toBeInTheDocument();
  });
});
