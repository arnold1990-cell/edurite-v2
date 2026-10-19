import { describe, expect, it, vi } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { CareerComparison } from "./CareerComparison";
import type { GuidanceCareer } from "./careerGuidanceModel";

const career: GuidanceCareer = {
  key: "software",
  title: "Software Engineer",
  category: "Technology",
  description: "Recorded software description.",
  reasons: ["Recorded interest in technology."],
  improvements: [],
  ai: false,
};
describe("career comparison", () => {
  it("compares real descriptions and evidence, preferring another career in the same category", () => {
    const html = renderToStaticMarkup(
      <CareerComparison
        initialCareer={career}
        careers={[
          career,
          {
            ...career,
            key: "teacher",
            title: "Teacher",
            category: "Education",
            description: "Teaching description.",
          },
          {
            ...career,
            key: "developer",
            title: "Web Developer",
            description: "Web development description.",
          },
        ]}
        onClose={vi.fn()}
      />,
    );
    expect(html).toContain("<th>Software Engineer</th><th>Web Developer</th>");
    expect(html).toContain("Recorded software description.");
    expect(html).toContain("Web development description.");
    expect(html).not.toContain("<td>Teaching description.</td>");
    expect(html).not.toContain("94%");
  });
  it("does not invent a second recommendation when only one is available", () => {
    const html = renderToStaticMarkup(
      <CareerComparison
        initialCareer={career}
        careers={[career]}
        onClose={vi.fn()}
      />,
    );
    expect(html).toContain("Choose second career");
    expect(html).toContain("Not selected");
  });
});
