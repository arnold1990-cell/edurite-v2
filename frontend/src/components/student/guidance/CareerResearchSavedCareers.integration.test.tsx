import { act, create, type ReactTestRenderer } from "react-test-renderer";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StaticRouter } from "react-router-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";
import { CareerSaveButton } from "./CareerGuidanceDetails";
import { SavedCareers } from "../career/SavedCareers";

const fixture = vi.hoisted(() => ({
  ids: [] as string[],
  career: {
    id: "2286e46d-5f4e-446d-b60a-a1ba7e630201",
    title: "Accountant",
    industry: "Finance",
  },
  save: vi.fn(),
  remove: vi.fn(),
  open: vi.fn(),
}));
vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "student-owner" } }),
}));
vi.mock("@/services/studentService", () => ({
  studentService: {
    savedCareers: async () => [...fixture.ids],
    saveCareer: async (id: string) => {
      fixture.save(id);
      if (!fixture.ids.includes(id)) fixture.ids.push(id);
    },
    unsaveCareer: async (id: string) => {
      fixture.remove(id);
      fixture.ids = fixture.ids.filter((value) => value !== id);
    },
  },
}));
vi.mock("@/services/careerService", () => ({
  careerService: {
    details: async (id: string) => {
      if (id !== fixture.career.id) throw new Error("Unknown catalogue ID");
      return fixture.career;
    },
  },
}));

let view: ReactTestRenderer;
let client: QueryClient;
const flush = () =>
  act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 25));
  });
afterEach(() => {
  if (view) act(() => view.unmount());
  client?.clear();
  fixture.ids = [];
  vi.clearAllMocks();
});
describe("guidance to Career Explorer saved-career integration", () => {
  it("persists the canonical career ID, refreshes Explorer, and supports removing the same saved career", async () => {
    client = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    await act(async () => {
      view = create(
        <QueryClientProvider client={client}>
          <StaticRouter location="/student/career-match">
            <CareerSaveButton
              id={fixture.career.id}
              title={fixture.career.title}
            />
            <SavedCareers
              roadmaps={[]}
              loading={false}
              retry={() => {}}
              error={false}
              onRoadmap={() => {}}
              onCareer={fixture.open}
            />
          </StaticRouter>
        </QueryClientProvider>,
      );
    });
    await flush();
    expect(JSON.stringify(view.toJSON())).toContain("No saved careers yet");
    await act(async () => {
      view.root
        .findAllByType("button")
        .find((b) => b.props["aria-label"] === "Save Accountant")!
        .props.onClick();
    });
    await flush();
    await flush();
    expect(fixture.save).toHaveBeenCalledExactlyOnceWith(fixture.career.id);
    expect(fixture.ids).toEqual([fixture.career.id]);
    expect(JSON.stringify(view.toJSON())).not.toContain("No saved careers yet");
    act(() =>
      view.root
        .findAllByType("button")
        .find((b) => b.props.className === "ec-study")!
        .props.onClick(),
    );
    expect(fixture.open).toHaveBeenCalledExactlyOnceWith("Accountant");
    await act(async () => {
      view.root
        .findAllByType("button")
        .find(
          (b) =>
            b.props["aria-label"] === "Remove Accountant from saved careers",
        )!
        .props.onClick();
    });
    await flush();
    await flush();
    expect(fixture.remove).toHaveBeenCalledExactlyOnceWith(fixture.career.id);
    expect(fixture.ids).toEqual([]);
    expect(JSON.stringify(view.toJSON())).toContain("No saved careers yet");
    expect(
      view.root
        .findAllByType("button")
        .some((b) => b.props["aria-label"] === "Save Accountant"),
    ).toBe(true);
  });
});
