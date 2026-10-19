import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { create, act, type ReactTestRenderer } from "react-test-renderer";
import {
  QueryClient,
  QueryClientProvider,
  onlineManager,
} from "@tanstack/react-query";
import { StaticRouter } from "react-router-dom/server";
import type { CareerAdviceResponse, StudentProfile } from "@/types";
import { StudentCareerMatchPage } from "./StudentCareerMatchPage";

const state = vi.hoisted(() => ({
  userId: "learner-a",
  profile: undefined as StudentProfile | undefined,
  remaining: 9,
  profileError: false,
  profilePending: false,
  accessError: false,
  evidence: [] as string[],
  queryKeys: [] as string[],
  saved: {} as Record<string, any>,
  savedGrade: {} as Record<string, string | undefined>,
  requestKeys: [] as (string | undefined)[],
  generate: vi.fn(),
}));
vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: state.userId } }),
}));
vi.mock("@/services/aiGuidanceService", () => ({
  aiGuidanceService: {
    careerResearchStatus: async () => {
      if (
        state.saved[state.userId] &&
        state.savedGrade[state.userId] !== state.profile?.selectedGrade
      )
        return {
          status: "STALE",
          error: "Your saved evidence changed.",
          report: null,
        };
      return (
        state.saved[state.userId] ?? {
          status: "IDLE",
          jobId: null,
          report: null,
        }
      );
    },
    startCareerResearch: async (requestKey?: string) => {
      state.requestKeys.push(requestKey);
      const owner = state.userId;
      const grade = state.profile?.selectedGrade;
      const value = await state.generate();
      const job = value?.status
        ? value
        : {
            jobId: "test-job",
            status: "SUCCEEDED",
            report: researchFixture(value),
          };
      state.saved[owner] = job;
      state.savedGrade[owner] = grade;
      return job;
    },
  },
}));
vi.mock("@/hooks/useAppQuery", () => ({
  useAppQuery: ({ queryKey }: { queryKey: string[] }) => {
    state.queryKeys.push(queryKey[0]);
    return {
      data:
        queryKey[0] === "me"
          ? state.profile
          : queryKey[0] === "recs"
            ? {
                suggestedCareers: [{ title: "Unrequested catalogue career" }],
                inputsUsed: state.evidence,
              }
            : queryKey[0] === "student-transcripts"
              ? []
              : undefined,
      isPending: queryKey[0] === "me" && state.profilePending,
      isError: queryKey[0] === "me" && state.profileError,
      refetch: vi.fn(),
    };
  },
}));
vi.mock("@/features/subscriptions/access", () => ({
  RequireEntitlement: ({ children }: { children: React.ReactNode }) => children,
  AiUsageDisplay: () => (
    <p>{10 - state.remaining} of 10 AI interactions used</p>
  ),
  useSubscriptionAccess: () => ({
    data: {
      accessAllowed: true,
      entitlements: ["PERSONALISED_CAREER_RECOMMENDATIONS"],
      aiUsage: { remaining: state.remaining },
    },
    isError: state.accessError,
    refetch: vi.fn(),
  }),
}));
vi.mock("./StudentPages", () => ({
  StudentPsychometricPage: () => <div>Assessment questions and history</div>,
}));
vi.mock("@/components/student/guidance/CareerGuidanceDetails", () => ({
  CareerGuidanceDetails: () => <section>Selected career details</section>,
  CareerSaveButton: () => <button>Save Career</button>,
}));

const valid: CareerAdviceResponse = {
  recommendedCareers: [
    {
      name: "Data Analyst",
      matchScore: null,
      reason: "Your recorded Mathematics result supports analytical work.",
      improvements: ["Review programme entry requirements."],
    },
  ],
};
function researchFixture(value: CareerAdviceResponse) {
  return {
    schemaVersion: "career-research-v1",
    generatedAt: new Date().toISOString(),
    academic: {
      grade: "Grade 11",
      percentageAverage: 72,
      subjects: [],
      relativeStrengths: [],
      improvementPriorities: [],
      curricula: [],
      interests: [],
      skills: [],
      limitations: [],
    },
    careers: value?.recommendedCareers?.map((career, index) => ({
      id: `career-${index}`,
      title: career.name,
      category: "Technology",
      description: "",
      rankingWeight: 15,
      suitability: "Evidence-aligned exploration",
      explanation: career.reason,
      relevantSubjects: [],
      strengths: [],
      improvementAreas: career.improvements ?? [],
      qualifications: [],
      programmeIds: [],
    })),
    programmes: [],
    alternativePathways: [],
    funding: [],
    interpretation: {
      assessment: "Recorded academic evidence supports exploration.",
      researchSummary: "No programme requirements verified.",
      nextSteps:
        value?.recommendedCareers?.flatMap((c) => c.improvements ?? []) ?? [],
      sourceUrls: [],
    },
    sources: [],
    inputsUsed: [],
    limitations: [],
    rankingMethod: "Internal catalogue weights, not probabilities.",
  };
}
let renderer: ReactTestRenderer;
let client: QueryClient;
const page = () => (
  <QueryClientProvider client={client}>
    <StaticRouter location="/student/career-match">
      <StudentCareerMatchPage />
    </StaticRouter>
  </QueryClientProvider>
);
const mount = async () => {
  await act(async () => {
    renderer = create(page());
  });
};
const flush = async () => {
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 15));
  });
};
const text = () => {
  const read = (node: unknown): string =>
    Array.isArray(node)
      ? node.map(read).join("")
      : typeof node === "string" || typeof node === "number"
        ? String(node)
        : node && typeof node === "object" && "children" in node
          ? read(node.children)
          : "";
  return read(renderer.toJSON());
};
const button = (label: string) =>
  renderer.root
    .findAllByType("button")
    .find((node) => node.children.some((child) => child === label))!;
const click = async (label = "Generate AI Guidance") => {
  await act(async () => {
    button(label).props.onClick();
  });
  await flush();
};
function deferred() {
  let resolve!: (value: unknown) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise((yes, no) => {
    resolve = yes;
    reject = no;
  });
  return { promise, resolve, reject };
}

describe("AI Career Guidance generation lifecycle", () => {
  beforeEach(() => {
    state.userId = "learner-a";
    state.saved = {};
    state.requestKeys = [];
    state.savedGrade = {};
    state.remaining = 9;
    state.profileError = false;
    state.profilePending = false;
    state.accessError = false;
    state.evidence = [];
    state.queryKeys = [];
    state.profile = {
      id: "profile-a",
      firstName: "Actual",
      lastName: "Student",
      selectedGrade: "Grade 11",
      profileCompleted: false,
      profileCompleteness: 67,
      subjectAchievements: [{ subjectName: "Mathematics", markPercentage: 72 }],
      interests: [],
      skills: [],
      experience: [],
      qualifications: [],
    };
    state.generate.mockReset();
    client = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
  });
  afterEach(() => {
    if (renderer) act(() => renderer.unmount());
    client.clear();
    onlineManager.setOnline(true);
  });
  it("renders only the compact header, real summary cards and generation action initially", async () => {
    await mount();
    expect(state.generate).not.toHaveBeenCalled();
    expect(text()).toContain("Actual Student");
    expect(text()).toContain("72% average");
    expect(text()).toContain("1 recorded subjects");
    expect(text()).toContain("Transcript not uploaded");
    expect(text()).toContain("AI guidance not generated yet");
    for (const unwanted of [
      "Your AI Career Recommendations",
      "Generated AI guidance",
      "Unrequested catalogue career",
      "Recommended Next Steps",
      "Career Path",
      "Your Subject Analysis",
    ])
      expect(text()).not.toContain(unwanted);
    expect(state.queryKeys).not.toContain("guidance-career-catalogue");
    const links = renderer.root
      .findAllByType("a")
      .map((node) => node.props.href);
    expect(links).toContain("/student/documents");
    expect(links).toContain("/student/profile?section=academic");
  });
  it("hides results while pending, prevents same-tick duplicate requests and reveals only a valid response", async () => {
    const request = deferred();
    state.generate.mockReturnValue(request.promise);
    await mount();
    const generate = button("Generate AI Guidance").props.onClick;
    await act(async () => {
      generate();
      generate();
    });
    await flush();
    expect(state.generate).toHaveBeenCalledTimes(1);
    expect(text()).toContain("Generating your personalised career guidance...");
    expect(button("Generating...").props.disabled).toBe(true);
    expect(text()).not.toContain("Your AI Career Recommendations");
    await act(async () => {
      request.resolve(valid);
    });
    await flush();
    expect(text()).toContain("Your AI Career Recommendations");
    expect(text()).toContain("Data Analyst");
    expect(text()).toContain("1 AI career recommendations");
    expect(text()).toContain(valid.recommendedCareers[0].reason);
    expect(text()).not.toContain("Unrequested catalogue career");
  });
  it.each([
    undefined,
    {},
    { recommendedCareers: [] },
    { recommendedCareers: [{ name: "Partial", reason: "" }] },
  ])(
    "rejects invalid or partial responses without revealing result headings (%s)",
    async (payload) => {
      state.generate.mockResolvedValue(payload);
      await mount();
      await click();
      expect(text()).toContain("No valid career guidance");
      expect(text()).not.toContain("Your AI Career Recommendations");
      expect(text()).toContain("AI guidance not generated yet");
    },
  );
  it.each([
    [undefined, "connection"],
    [500, "connection"],
    [503, "temporarily unavailable"],
    [401, "session has expired"],
    [403, "current access"],
    [429, "interaction limit"],
    [422, "saved information"],
  ])(
    "handles an error (%s) and lets a deliberate retry succeed",
    async (status, message) => {
      state.generate
        .mockRejectedValueOnce(Object.assign(new Error("failure"), { status }))
        .mockResolvedValueOnce(valid);
      await mount();
      await click();
      expect(state.generate).toHaveBeenCalledTimes(1);
      expect(text()).toContain(message);
      expect(text()).not.toContain("Your AI Career Recommendations");
      await click("Retry AI Guidance");
      expect(state.generate).toHaveBeenCalledTimes(2);
      expect(text()).toContain("Your AI Career Recommendations");
    },
  );
  it("hides previous results during regeneration and after a failed refresh", async () => {
    const request = deferred();
    state.generate
      .mockResolvedValueOnce(valid)
      .mockReturnValueOnce(request.promise);
    await mount();
    await click();
    await click("Refresh Recommendations");
    expect(text()).not.toContain("Your AI Career Recommendations");
    await act(async () => {
      request.reject(new Error("offline"));
    });
    await flush();
    expect(text()).not.toContain("Data Analyst");
    expect(text()).toContain("Retry AI Guidance");
  });
  it.each([
    "quota",
    "missing-profile",
    "profile-pending",
    "profile-error",
    "access-error",
    "missing-evidence",
  ])("does not invoke AI when blocked by %s", async (reason) => {
    if (reason === "quota") state.remaining = 0;
    if (reason === "missing-profile") state.profile = undefined;
    if (reason === "profile-pending") state.profilePending = true;
    if (reason === "profile-error") state.profileError = true;
    if (reason === "access-error") state.accessError = true;
    if (reason === "missing-evidence")
      state.profile = { ...state.profile!, subjectAchievements: [] };
    await mount();
    expect(button("Generate AI Guidance").props.disabled).toBe(true);
    await click();
    expect(state.generate).not.toHaveBeenCalled();
    expect(text()).not.toContain("Your AI Career Recommendations");
  });
  it("allows optional assessment evidence without inventing academic results", async () => {
    state.profile = { ...state.profile!, subjectAchievements: [] };
    state.evidence = ["Saved assessment evidence"];
    state.generate.mockResolvedValue(valid);
    await mount();
    expect(button("Generate AI Guidance").props.disabled).toBe(false);
    await click();
    expect(state.generate).toHaveBeenCalledTimes(1);
  });
  it("keeps results during a background usage refresh but hides them after profile edits", async () => {
    state.generate.mockResolvedValue(valid);
    await mount();
    await click();
    state.remaining = 8;
    await act(async () => {
      renderer.update(page());
    });
    expect(text()).toContain("Your AI Career Recommendations");
    state.profile = { ...state.profile!, selectedGrade: "Grade 12" };
    await act(async () => {
      renderer.update(page());
    });
    expect(text()).not.toContain("Your AI Career Recommendations");
    expect(text()).toContain("AI guidance not generated yet");
  });
  it("does not leak an old account response into the next account after an in-flight request", async () => {
    const request = deferred();
    state.generate.mockReturnValue(request.promise);
    await mount();
    await click();
    state.userId = "learner-b";
    await act(async () => {
      renderer.update(page());
    });
    await act(async () => {
      request.resolve(valid);
    });
    await flush();
    expect(text()).not.toContain("Data Analyst");
    expect(text()).toContain("AI guidance not generated yet");
  });
  it("preserves the assessment route", async () => {
    await act(async () => {
      renderer = create(
        <QueryClientProvider client={client}>
          <StaticRouter location="/student/career-match?step=assessment">
            <StudentCareerMatchPage />
          </StaticRouter>
        </QueryClientProvider>,
      );
    });
    expect(text()).toContain("Assessment questions and history");
    expect(text()).toContain("Back to Career Match");
    expect(state.generate).not.toHaveBeenCalled();
  });
  it("does not log React console errors during generation, failure and retry interactions", async () => {
    const errors = vi.spyOn(console, "error");
    try {
      state.generate
        .mockRejectedValueOnce(new Error("offline"))
        .mockResolvedValueOnce(valid);
      await mount();
      await click();
      await click("Retry AI Guidance");
      expect(text()).toContain("Your AI Career Recommendations");
      expect(errors).not.toHaveBeenCalled();
    } finally {
      errors.mockRestore();
    }
  });
  it("reports an offline generation failure without queueing another interaction on reconnect", async () => {
    onlineManager.setOnline(false);
    state.generate.mockRejectedValue(new Error("offline"));
    await mount();
    await click();
    expect(text()).toContain("Check your connection");
    expect(state.generate).toHaveBeenCalledTimes(1);
    onlineManager.setOnline(true);
    await flush();
    expect(state.generate).toHaveBeenCalledTimes(1);
  });
  it("retrieves a successful saved report without starting or charging another generation", async () => {
    state.saved[state.userId] = {
      jobId: "saved-job",
      status: "SUCCEEDED",
      report: researchFixture(valid),
    };
    state.savedGrade[state.userId] = state.profile?.selectedGrade;
    await mount();
    await flush();
    expect(text()).toContain("Data Analyst");
    expect(text()).toContain("Saved report generated on");
    expect(state.generate).not.toHaveBeenCalled();
  });
  it("tracks a queued job through status GETs and only reveals the completed report", async () => {
    state.generate.mockResolvedValue({
      jobId: "queued-job",
      status: "QUEUED",
      stage: "Checking official sources",
      report: null,
    });
    await mount();
    await click();
    expect(text()).not.toContain("Your AI Career Recommendations");
    expect(
      renderer.root.findAllByType("button").some((node) => node.props.disabled),
    ).toBe(true);
    state.saved[state.userId] = {
      jobId: "queued-job",
      status: "SUCCEEDED",
      report: researchFixture(valid),
    };
    await act(async () => {
      await client.invalidateQueries({ queryKey: ["career-research"] });
    });
    await flush();
    expect(text()).toContain("Data Analyst");
    expect(state.generate).toHaveBeenCalledTimes(1);
  });
  it("reuses the request key after an ambiguous network failure", async () => {
    state.generate
      .mockRejectedValueOnce(new Error("Network response lost"))
      .mockResolvedValueOnce(valid);
    await mount();
    await click();
    await click("Retry AI Guidance");
    expect(state.requestKeys).toHaveLength(2);
    expect(state.requestKeys[0]).toBeTruthy();
    expect(state.requestKeys[1]).toBe(state.requestKeys[0]);
  });
});
