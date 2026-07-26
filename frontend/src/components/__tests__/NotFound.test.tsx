import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import NotFound from "../NotFound";

describe("NotFound Component", () => {
  it("renders 404 heading and link to home page", () => {
    render(
      <BrowserRouter>
        <NotFound />
      </BrowserRouter>,
    );

    expect(screen.getByText("Error 404")).toBeInTheDocument();
    expect(screen.getByText("Page not found")).toBeInTheDocument();

    const homeLink = screen.getByRole("link", { name: "Go back home" });
    expect(homeLink).toBeInTheDocument();
    expect(homeLink).toHaveAttribute("href", "/");
  });
});
