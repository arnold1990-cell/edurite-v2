import { act, create, type ReactTestRenderer } from "react-test-renderer";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StaticRouter } from "react-router-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SavedResearchStudyOptions } from "./SavedResearchStudyOptions";

const api = vi.hoisted(() => ({ read: vi.fn(), start: vi.fn() }));
vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "owner" } }),
}));
vi.mock("@/services/aiGuidanceService", () => ({
  aiGuidanceService: {
    careerResearchStatus: api.read,
    startCareerResearch: api.start,
  },
}));
// Schema validation has dedicated malformed-report tests; this test isolates the read-only view.
vi.mock("./useCareerGuidance", () => ({
  validateResearchJob: (value: unknown) => value,
}));
let view: ReactTestRenderer;
let client: QueryClient;
afterEach(() => {
  if (view) act(() => view.unmount());
  client?.clear();
  vi.clearAllMocks();
});
describe("saved research Study Options", () => {
  it("tracks shared background status and renders only successfully completed programme results", async () => {
    api.read.mockResolvedValue({
      status: "RUNNING",
      stage: "Checking sources",
      report: null,
    });
    client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    await act(async () => {
      view = create(
        <QueryClientProvider client={client}>
          <StaticRouter location="/student/study-options?section=research">
            <SavedResearchStudyOptions />
          </StaticRouter>
        </QueryClientProvider>,
      );
    });
    await act(async () => {
      await new Promise((r) => setTimeout(r, 20));
    });
    expect(JSON.stringify(view.toJSON())).toContain("Research is in progress");
    expect(JSON.stringify(view.toJSON())).not.toContain(
      "University and Programme Recommendations",
    );
    api.read.mockResolvedValue({
      status: "SUCCEEDED",
      report: { programmes: [] },
    });
    await act(async () => {
      await client.invalidateQueries({
        queryKey: ["career-research-study-options", "owner"],
      });
    });
    await act(async () => {
      await new Promise((r) => setTimeout(r, 20));
    });
    expect(JSON.stringify(view.toJSON())).toContain(
      "University and Programme Recommendations",
    );
    expect(api.start).not.toHaveBeenCalled();
  });
});
