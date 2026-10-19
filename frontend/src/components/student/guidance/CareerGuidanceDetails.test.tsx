import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { StaticRouter } from "react-router-dom/server";
import type { StudentProfile, UniversityRequirementMatch } from "@/types";
import {
  AcademicRequirements,
  CareerGuidanceDetails,
  CareerSaveButton,
} from "./CareerGuidanceDetails";
import type { GuidanceCareer } from "./careerGuidanceModel";

const state = vi.hoisted(() => ({
  tab: "Overview",
  saved: ["career-1"],
  error: false,
}));
vi.mock("react", async (importOriginal) => {
  const actual = await importOriginal<typeof import("react")>();
  return {
    ...actual,
    useState: (initial: unknown) =>
      initial === "Overview" ? [state.tab, vi.fn()] : actual.useState(initial),
  };
});
vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "student-1" } }),
}));
vi.mock("@/features/subscriptions/access", () => ({
  useSubscriptionAccess: () => ({
    data: { entitlements: ["CAREER_ROADMAP_PERSONALISED"] },
  }),
}));
vi.mock("@tanstack/react-query", () => ({
  useQueryClient: () => ({ invalidateQueries: vi.fn() }),
  useMutation: () => ({ mutate: vi.fn() }),
}));
vi.mock("@/hooks/useAppQuery", () => ({
  useAppQuery: ({ queryKey }: { queryKey: string[] }) => ({
    data: queryKey[0] === "saved-careers" ? state.saved : [],
    isError: state.error,
    refetch: vi.fn(),
  }),
}));

const career: GuidanceCareer = {
  key: "career-1",
  id: "career-1",
  title: "Software Engineer",
  category: "Technology",
  description: "Catalogue description.",
  reasons: ["Your saved technology interest relates to this career."],
  improvements: ["Review your Mathematics result."],
  ai: true,
};
const profile = {
  id: "student-1",
  selectedGrade: "Grade 11",
  subjectAchievements: [
    { subjectName: "Mathematics", markPercentage: 72, verified: true },
    { subjectName: "English", markPercentage: 59, verified: false },
  ],
} as StudentProfile;
const programme: UniversityRequirementMatch = {
  id: "programme-1",
  institutionName: "Recorded Institution",
  qualificationName: "Recorded qualification",
  mathematicsRequirement: "70%",
  englishRequirement: "60%",
  physicalSciencesRequirement: "Level 5",
  verified: true,
  verificationBadge: "Verified",
  requirementStatus: "Unknown",
  apsRequired: 30,
};
const render = (element: React.ReactNode) =>
  renderToStaticMarkup(
    <StaticRouter location="/student/career-match">{element}</StaticRouter>,
  );

describe("career guidance details", () => {
  beforeEach(() => {
    state.tab = "Overview";
    state.error = false;
    state.saved = ["career-1"];
  });
  it.each([
    "Overview",
    "Requirements",
    "Universities",
    "Career Path",
    "Skills",
    "Opportunities",
  ])(
    "renders the %s tab with accessible tab relationships and empty states",
    (tab) => {
      state.tab = tab;
      const html = render(
        <CareerGuidanceDetails career={career} profile={profile} />,
      );
      expect(html).toContain('role="tablist"');
      expect(html).toContain('role="tabpanel"');
      expect(html.match(/aria-selected="true"/g)).toHaveLength(1);
      expect(html).not.toContain("94%");
      if (tab === "Overview") expect(html).toContain(career.reasons[0]);
      if (tab === "Requirements")
        expect(html).toContain("No verified programme requirements");
      if (tab === "Universities")
        expect(html).toContain("No linked university programmes");
      if (tab === "Career Path") expect(html).toContain("Grade 11");
      if (tab === "Skills") expect(html).toContain(career.improvements[0]);
      if (tab === "Opportunities")
        expect(html).toContain("No current funding recommendations");
    },
  );
  it("compares actual results only with verified programme thresholds and leaves missing results unknown", () => {
    const html = render(
      <AcademicRequirements programmes={[programme]} profile={profile} />,
    );
    expect(html).toContain("72%");
    expect(html).toContain("59%");
    expect(html).toContain("Requirement met");
    expect(html).toContain("Gap to address");
    expect(html).toContain("Cannot determine");
    expect(html).toContain("Not recorded");
    expect(html).not.toContain("Eligible");
  });
  it("does not compare unverified programme requirements or show unverified student marks as verified", () => {
    expect(
      render(
        <AcademicRequirements
          programmes={[{ ...programme, verified: false }]}
          profile={profile}
        />,
      ),
    ).toContain("No verified programme requirements");
    const html = render(
      <AcademicRequirements
        programmes={[programme]}
        profile={{
          ...profile,
          subjectAchievements: [
            { subjectName: "Mathematics", markPercentage: 72, verified: false },
          ],
        }}
      />,
    );
    expect(html).toContain("Meets recorded threshold · unverified");
    expect(html).not.toContain("cg-met");
  });
  it("shows save state from the real saved-career IDs and avoids saving unmatched AI names", () => {
    expect(
      render(<CareerSaveButton id="career-1" title="Software Engineer" />),
    ).toContain("Unsave Software Engineer");
    state.saved = [];
    expect(
      render(<CareerSaveButton id="career-1" title="Software Engineer" />),
    ).toContain("Save Software Engineer");
    const unmatched = render(<CareerSaveButton title="Unknown profession" />);
    expect(unmatched).toContain("Find in Career Explorer");
    expect(unmatched).not.toContain("<button");
  });
  it("shows retry actions instead of presenting API failures as empty programme results", () => {
    state.error = true;
    state.tab = "Universities";
    const html = render(
      <CareerGuidanceDetails career={career} profile={profile} />,
    );
    expect(html).toContain("Programme requirements could not load");
    expect(html).not.toContain("No linked university programmes");
  });
  it("preserves the selected career in existing programme, study plan and roadmap routes", () => {
    const html = render(
      <CareerGuidanceDetails career={career} profile={profile} />,
    );
    expect(html).toContain("section=study-plan&amp;career=Software%20Engineer");
    expect(html).toContain(
      "section=career-path&amp;career=Software%20Engineer",
    );
    expect(html).toContain("section=programmes&amp;q=Software%20Engineer");
  });
});
