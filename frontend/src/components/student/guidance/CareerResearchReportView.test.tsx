import { afterEach, describe, expect, it, vi } from "vitest";
import { act, create, type ReactTestRenderer } from "react-test-renderer";
import { StaticRouter } from "react-router-dom/server";
import type {
  CareerResearchReport,
  ResearchProgramme,
} from "@/types/careerResearch";
import {
  CareerResearchReportView,
  ResearchProgrammeCollection,
} from "./CareerResearchReportView";
import { validateResearchJob } from "./useCareerGuidance";

vi.mock("./CareerGuidanceDetails", () => ({
  CareerSaveButton: () => <button>Save Career</button>,
}));
const programme = (id: string, country: string): ResearchProgramme => ({
  id,
  institution: `Institution ${id}`,
  country,
  name: `Accounting ${id}`,
  qualification: "Degree",
  sourceUrl: `https://official.example/${id}`,
  verificationStatus: "Admission requirements not yet verified.",
  eligibility: {
    status: "Insufficient information",
    studentAps: null,
    requiredAps: null,
    subjects: [],
    gaps: [],
    uncheckedConditions: ["No verified thresholds."],
  },
  careerIds: ["career-0"],
});
const report = (): CareerResearchReport => ({
  schemaVersion: "career-research-v1",
  generatedAt: "2026-10-18T12:00:00Z",
  academic: {
    percentageAverage: null,
    subjects: [],
    relativeStrengths: [],
    improvementPriorities: [],
    curricula: [],
    interests: ["Accounting"],
    skills: [],
    limitations: [],
  },
  careers: Array.from({ length: 16 }, (_, i) => ({
    id: `career-${i}`,
    title: `Career ${i}`,
    category: i ? "Finance" : "Technology",
    description: "",
    rankingWeight: 10,
    suitability: "Evidence-aligned exploration",
    explanation: "Declared interest supports exploring this catalogue career.",
    relevantSubjects: [],
    strengths: [],
    improvementAreas: [],
    qualifications: [],
    programmeIds: i ? [] : ["one", "two"],
  })),
  programmes: [programme("one", "Botswana"), programme("two", "Zimbabwe")],
  alternativePathways: [],
  funding: [
    {
      externalId: "expired",
      title: "Expired scholarship",
      deadline: "2001-01-01",
    },
    {
      externalId: "unknown",
      title: "Confirm opening",
      applicationLink: "javascript:alert(1)",
    },
  ],
  interpretation: {
    assessment: "Evidence supports further exploration.",
    researchSummary: "Requirements remain unverified.",
    nextSteps: ["Review the official source."],
    sourceUrls: [],
  },
  sources: [],
  inputsUsed: ["Declared interests"],
  limitations: [],
  rankingMethod: "Internal ordering, not a probability.",
});
let view: ReactTestRenderer;
afterEach(() => {
  if (view) act(() => view.unmount());
});
const mount = (content: React.ReactNode) =>
  act(() => {
    view = create(<StaticRouter location="/student/career-match">{content}</StaticRouter>);
  });
const content = () => JSON.stringify(view.toJSON());
describe("career research report", () => {
  it("progressively reveals every supported career beyond the first twelve", () => {
    mount(<CareerResearchReportView report={report()} />);
    expect(content()).not.toContain('"Career 15"');
    const more = view.root
      .findAllByType("button")
      .find((b) =>
        b.children.some(
          (t) => typeof t === "string" && t.startsWith("Load more careers"),
        ),
      )!;
    act(() => more.props.onClick());
    expect(content()).toContain("Career 15");
  });
  it("filters actual careers by search and programme country", () => {
    mount(<CareerResearchReportView report={report()} />);
    const search = view.root.findAllByType("input")[0];
    act(() => search.props.onChange({ target: { value: "Career 15" } }));
    expect(content()).toContain("Career 15");
    expect(content()).not.toContain('"Career 0"');
    act(() => search.props.onChange({ target: { value: "" } }));
    const country = view.root.findAllByType("select")[1];
    act(() => country.props.onChange({ target: { value: "Botswana" } }));
    expect(content()).toContain('"Career 0"');
    expect(content()).not.toContain('"Career 1"');
  });
  it("shows recorded eligibility conditions and working source links without fabricating thresholds", () => {
    mount(
      <ResearchProgrammeCollection
        programmes={[programme("one", "Botswana")]}
      />,
    );
    act(() => view.root.findByType("button").props.onClick());
    expect(content()).toContain("No verified thresholds.");
    expect(content()).toContain("https://official.example/one");
    expect(content()).not.toContain("Meets published requirements");
  });
  it("excludes expired funding and unsafe external links", () => {
    mount(<CareerResearchReportView report={report()} />);
    expect(content()).not.toContain("Expired scholarship");
    expect(content()).not.toContain("javascript:");
    expect(content()).toContain("Deadline and open status not verified");
  });
  it("rejects malformed persisted report structures before rendering them", () => {
    const invalid = report();
    invalid.programmes[0].eligibility = null as never;
    expect(() =>
      validateResearchJob({ status: "SUCCEEDED", report: invalid }),
    ).toThrow("No valid career guidance");
  });
});
