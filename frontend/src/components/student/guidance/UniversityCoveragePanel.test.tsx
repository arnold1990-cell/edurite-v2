import { describe, it, expect, vi } from "vitest";
import { act, create } from "react-test-renderer";
import { UniversityCoveragePanel } from "./UniversityCoveragePanel";
const get = vi.hoisted(() => vi.fn());
vi.mock("@/services/apiClient", () => ({ apiClient: { get } }));
describe("university coverage", () => {
  it("separates registered institutions from verified programme coverage and shows failures", async () => {
    get.mockResolvedValue({ data: { publicInstitutions: 26, directoryStatus: "Government snapshot", limitation: "Coverage is partial.", institutions: [{ institution: { universityName: "Fixture University", institutionType: "TRADITIONAL", registrationStatus: "PUBLIC_ESTABLISHED" }, indexedProgrammes: 0, retrievedPages: 0, queuedPages: 4, status: "Information not yet verified.", unavailableSources: [{ url: "https://fixture.ac.za/programmes", status: "ACCESS_DENIED", reason: "Robots denied" }] }] } });
    let view!: ReturnType<typeof create>;
    act(() => { view = create(<UniversityCoveragePanel />); });
    expect(get).not.toHaveBeenCalled();
    await act(async () => { await view.root.findByType("button").props.onClick(); });
    expect(get).toHaveBeenCalledWith("/ai/university-intelligence/coverage");
    const content = JSON.stringify(view.toJSON());
    expect(content).toContain("26"); expect(content).toContain("Coverage is partial"); expect(content).toContain("Robots denied");
    act(() => view.unmount());
  });
});
