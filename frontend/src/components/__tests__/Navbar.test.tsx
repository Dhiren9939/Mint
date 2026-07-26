import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import Navbar from "../Navbar";

describe("Navbar Component", () => {
  it("renders logo and brand title correctly", () => {
    render(<Navbar onAboutClick={vi.fn()} />);

    expect(screen.getByAltText("Mint logo")).toBeInTheDocument();
    expect(screen.getByText("Mint")).toBeInTheDocument();
  });

  it("triggers onAboutClick callback when About button is clicked", () => {
    const onAboutClickMock = vi.fn();
    render(<Navbar onAboutClick={onAboutClickMock} />);

    fireEvent.click(screen.getByRole("button", { name: /about/i }));
    expect(onAboutClickMock).toHaveBeenCalledTimes(1);
  });
});
