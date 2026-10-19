import { useSearchParams, Link } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { useAppQuery } from "@/hooks/useAppQuery";
import { aiGuidanceService } from "@/services/aiGuidanceService";
import { ResearchProgrammeCollection } from "./CareerResearchReportView";
import { validateResearchJob } from "./useCareerGuidance";
import "./career-guidance.css";
export function SavedResearchStudyOptions() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const query = useAppQuery({
    queryKey: ["career-research-study-options", user?.id],
    enabled: Boolean(user?.id),
    staleTime: 0,
    refetchInterval: (q) =>
      ["QUEUED", "RUNNING"].includes(q.state.data?.status ?? "") ? 1500 : false,
    queryFn: async () =>
      validateResearchJob(await aiGuidanceService.careerResearchStatus()),
  });
  return (
    <section className="cg-page student-page-section">
      <h1 className="student-page-title">Your Career Research Study Options</h1>
      {query.isPending && (
        <p role="status">Loading saved programme recommendations...</p>
      )}
      {["QUEUED", "RUNNING"].includes(query.data?.status ?? "") && (
        <p role="status">
          Research is in progress. {query.data?.stage} Programme recommendations
          will appear after successful completion.
        </p>
      )}
      {query.isError && (
        <p role="alert">
          Saved research could not load.{" "}
          <button className="cg-text-button" onClick={() => query.refetch()}>
            Retry
          </button>
        </p>
      )}
      {query.data?.status === "SUCCEEDED" && query.data.report ? (
        <ResearchProgrammeCollection
          programmes={query.data.report.programmes}
          initialCareer={params.get("career") || undefined}
        />
      ) : (
        !query.isPending &&
        !query.isError &&
        !["QUEUED", "RUNNING"].includes(query.data?.status ?? "") && (
          <p>
            Your career research is not complete or needs refreshing.{" "}
            <Link className="cg-text-link" to="/student/career-match">
              Open AI Career Guidance
            </Link>
          </p>
        )
      )}
    </section>
  );
}
