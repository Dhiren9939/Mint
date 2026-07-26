import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import GlassCard from "../GlassCard";

describe("GlassCard Component", () => {
  it("renders children correctly inside the card container", () => {
    render(
      <GlassCard>
        <div data-testid="card-child">Glass content</div>
      </GlassCard>,
    );

    expect(screen.getByTestId("card-child")).toBeInTheDocument();
    expect(screen.getByText("Glass content")).toBeInTheDocument();
  });
});
