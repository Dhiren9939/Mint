import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import Panel from "../Panel";

describe("Panel Component", () => {
  it("renders children correctly inside the panel container", () => {
    render(
      <Panel>
        <div data-testid="card-child">Panel content</div>
      </Panel>,
    );

    expect(screen.getByTestId("card-child")).toBeInTheDocument();
    expect(screen.getByText("Panel content")).toBeInTheDocument();
  });
});
