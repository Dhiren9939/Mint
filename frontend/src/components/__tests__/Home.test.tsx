import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import Home from "../Home";

describe("Home Component", () => {
  it("renders heading and SendCard by default", () => {
    render(<Home />);

    expect(
      screen.getByText("Send files and text with temporary retrieval keys"),
    ).toBeInTheDocument();
    expect(screen.getByText("Choose what to send")).toBeInTheDocument();
  });

  it("switches to RecieveCard when Recieve mode is selected", () => {
    render(<Home />);

    const recieveModeBtn = screen.getByRole("button", { name: "Recieve" });
    fireEvent.click(recieveModeBtn);

    expect(screen.getByText("Enter your file code")).toBeInTheDocument();
  });
});
