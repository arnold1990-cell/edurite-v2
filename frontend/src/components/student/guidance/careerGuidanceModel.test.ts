import { describe, expect, it } from "vitest";
import {
  careerCategory,
  careerPathLink,
  compareRequirement,
  filterCareers,
  guidanceCareers,
  subjectResult,
  transcriptName,
} from "./careerGuidanceModel";
import type { Recommendation } from "@/types";

const recommendations: Recommendation = {
  suggestedCareers: [
    {
      id: "1",
      title: "Software Engineer",
      score: 94,
      rationale:
        "Your saved technology interest relates to this career. Mathematics supports analytical work.",
    },
  ],
  suggestedBursaries: [],
  suggestedCoursesOrImprovements: [],
  profileImprovementTips: [],
  modelVersion: "test",
};
describe("career guidance evidence presentation", () => {
  it("uses actual recommendations without exposing internal ranking scores", () => {
    const cards = guidanceCareers(recommendations, undefined, [
      {
        id: "1",
        title: "Software Engineer",
        industry: "Technology",
        description: "Builds software.",
      },
    ]);
    expect(cards[0]).toMatchObject({
      id: "1",
      title: "Software Engineer",
      description: "Builds software.",
      ai: false,
    });
    expect(cards[0].reasons).toHaveLength(2);
    expect(cards[0]).not.toHaveProperty("score");
    expect(cards[0]).not.toHaveProperty("matchScore");
  });
  it("preserves AI evidence without padding reasons, and only saves exact catalogue identities", () => {
    const cards = guidanceCareers(
      undefined,
      {
        recommendedCareers: [
          {
            name: "Data Scientist",
            matchScore: 99,
            reason: "Saved interest in data.",
            improvements: ["Add subject results."],
          },
        ],
      },
      [{ id: "wrong", title: "Data Scientist Assistant" }],
    );
    expect(cards[0].id).toBeUndefined();
    expect(cards[0].reasons).toEqual(["Saved interest in data."]);
    expect(cards[0].improvements).toEqual(["Add subject results."]);
    expect(JSON.stringify(cards)).not.toContain("99");
  });
  it("filters real results by category and case-insensitive search; never promotes weights to strong matches", () => {
    const cards = guidanceCareers(recommendations);
    expect(filterCareers(cards, "All Careers", "  SOFTWARE ")).toHaveLength(1);
    expect(filterCareers(cards, "Technology", "")).toHaveLength(1);
    expect(filterCareers(cards, "Health Sciences", "")).toHaveLength(0);
    expect(filterCareers(cards, "Strong Matches", "")).toHaveLength(0);
    expect(filterCareers(cards, "All Careers", "unrelated")).toHaveLength(0);
  });
  it.each([
    ["70%", 72, undefined, "Met"],
    ["70%", 65, undefined, "Gap"],
    ["70%", 0, undefined, "Gap"],
    ["Level 5", undefined, 6, "Met"],
    ["Level 5", undefined, 4, "Gap"],
    ["70% or Level 5", 90, 7, "Unknown"],
    ["English Home Language 60%", 90, 7, "Unknown"],
    ["70%", undefined, 7, "Unknown"],
    ["Level 5", 90, undefined, "Unknown"],
    ["70%", 101, undefined, "Unknown"],
    ["150%", 100, undefined, "Unknown"],
  ])(
    "compares only explicit thresholds (%s)",
    (requirement, mark, level, expected) => {
      expect(
        compareRequirement(requirement, {
          subjectName: "Maths",
          markPercentage: mark,
          achievementLevel: level,
        }),
      ).toBe(expected);
    },
  );
  it("keeps missing marks missing and achievement levels on their original scale", () => {
    expect(subjectResult({ subjectName: "Maths", markPercentage: 0 })).toBe(
      "0%",
    );
    expect(subjectResult({ subjectName: "Maths", achievementLevel: 6 })).toBe(
      "Level 6",
    );
    expect(subjectResult()).toBe("Not recorded");
    expect(subjectResult({ subjectName: "Maths", markPercentage: 105 })).toBe(
      "Not recorded",
    );
  });
  it("groups careers for display and encodes existing pathway routes", () => {
    expect(careerCategory("Nurse")).toBe("Health Sciences");
    expect(careerCategory("Teacher")).toBe("Education");
    expect(careerPathLink("Arts & Design", "study-plan")).toBe(
      "/student/career-explorer?section=study-plan&career=Arts%20%26%20Design",
    );
    expect(
      transcriptName(
        "documents/user/transcript-12345678-1234-1234-1234-123456789abc-results.pdf",
      ),
    ).toBe("results.pdf");
    expect(transcriptName('documents/results.pdf?signature=private')).toBe('results.pdf');
  });
});
