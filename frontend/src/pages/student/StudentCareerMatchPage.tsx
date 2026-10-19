import { Link, useSearchParams } from "react-router-dom";
import {
  ArrowRight,
  BarChart3,
  GraduationCap,
  FileText,
  RefreshCw,
  Sparkles,
  UserRound,
} from "lucide-react";
import { useAppQuery } from "@/hooks/useAppQuery";
import { useAuth } from "@/hooks/useAuth";
import { recommendationService } from "@/services/recommendationService";
import { studentService } from "@/services/studentService";
import { featureModulesService } from "@/services/featureModulesService";
import {
  RequireEntitlement,
  AiUsageDisplay,
  useSubscriptionAccess,
} from "@/features/subscriptions/access";
import { StudentPsychometricPage } from "./StudentPages";
import { SummaryCardIllustration } from "@/components/student/guidance/SummaryCardIllustration";
import { transcriptName } from "@/components/student/guidance/careerGuidanceModel";
import { CareerResearchReportView } from "@/components/student/guidance/CareerResearchReportView";
import {
  hasGuidanceEvidence,
  useCareerGuidance,
} from "@/components/student/guidance/useCareerGuidance";
import "@/components/student/guidance/career-guidance.css";

export function StudentCareerMatchPage() {
  const [params] = useSearchParams();
  const { user } = useAuth();
  return (
    <section className="cg-page student-page-section">
      {params.get("step") === "assessment" ? (
        <>
          <h1 className="student-page-title">
            <Sparkles aria-hidden="true" />
            AI Career Guidance
          </h1>
          <Link className="cg-text-link" to="/student/career-match">
            Back to Career Match
          </Link>
          <RequireEntitlement feature="CAREER_ASSESSMENT">
            <StudentPsychometricPage />
          </RequireEntitlement>
        </>
      ) : (
        <RequireEntitlement feature="PERSONALISED_CAREER_RECOMMENDATIONS">
          <CareerMatches key={user?.id} />
        </RequireEntitlement>
      )}
    </section>
  );
}

function CareerMatches() {
  const { user } = useAuth();
  const enabled = Boolean(user?.id);
  const access = useSubscriptionAccess();
  const profile = useAppQuery({
    queryKey: ["me", user?.id],
    queryFn: studentService.getMe,
    enabled,
  });
  const transcripts = useAppQuery({
    queryKey: ["student-transcripts", user?.id],
    queryFn: studentService.transcripts,
    enabled,
  });
  const aps = useAppQuery({
    queryKey: ["student-aps-profile", user?.id],
    queryFn: featureModulesService.apsProfile,
    enabled,
  });
  // Catalogue evidence can validate an assessment-only profile, but is never
  // rendered as an AI generation result.
  const evidence = useAppQuery({
    queryKey: ["recs", user?.id],
    queryFn: recommendationService.mine,
    enabled,
  });
  const p = profile.data;
  const scope = JSON.stringify([
    user?.id,
    p?.selectedGrade,
    p?.qualificationLevel,
    p?.subjectAchievements,
    p?.interests,
    p?.skills,
    p?.careerGoals,
    p?.preferredCareer,
    p?.transcriptFileUrl,
  ]);
  const blockReason = !enabled
    ? "Sign in to generate AI guidance."
    : profile.isPending
      ? "Loading your student profile..."
      : profile.isError || !p
        ? "Your profile could not load. Retry before generating guidance."
        : access.isPending
          ? "Checking AI access and usage..."
          : access.isError
            ? "AI access could not be checked. Retry before generating guidance."
            : !access.data?.accessAllowed ||
                !access.data.entitlements.includes(
                  "PERSONALISED_CAREER_RECOMMENDATIONS",
                )
              ? "Your subscription does not currently allow AI guidance."
              : access.data.aiUsage.remaining <= 0
                ? "No AI interactions remain. Review your plan and usage before generating again."
                : !hasGuidanceEvidence(p, evidence.data?.inputsUsed)
                  ? evidence.isPending
                    ? "Loading your saved academic and assessment evidence..."
                    : "Add academic results, interests, skills or an assessment before generating personalised guidance."
                  : undefined;
  const ai = useCareerGuidance(scope, blockReason);
  const latest = [...(transcripts.data ?? [])]
    .reverse()
    .find((item) => item.documentId === p?.transcriptFileUrl);
  const subjects = p?.subjectAchievements ?? [];
  const marks = subjects.flatMap((item) =>
    item.markPercentage != null &&
    Number.isFinite(item.markPercentage) &&
    item.markPercentage >= 0 &&
    item.markPercentage <= 100
      ? [item.markPercentage]
      : [],
  );
  const transcriptStatus =
    profile.isPending || transcripts.isPending
      ? "Checking transcript..."
      : profile.isError || transcripts.isError
        ? "Transcript status unavailable"
        : !p?.transcriptFileUrl
          ? "Transcript not uploaded"
          : latest?.verifiedAt
            ? "Transcript reviewed"
            : latest?.extraction.subjects.length
              ? "Results extracted - awaiting review"
              : "Transcript uploaded - review academic results";
  return (
    <>
      <header className="cg-header">
        <div>
          <h1 className="student-page-title">
            <Sparkles aria-hidden="true" />
            AI Career Guidance
          </h1>
          <p className="student-page-subtitle">
            Discover career opportunities based on your academic performance,
            interests, strengths, and future goals.
          </p>
        </div>
        <span className="cg-ai-badge">
          <Sparkles size={14} /> Gemini AI
        </span>
      </header>
      <div className="cg-summary">
        <article className="cg-panel ed-panel">
          <div className="cg-card-heading">
            <span className="cg-summary-icon">
              <UserRound size={20} aria-hidden="true" />
            </span>
            <h2>My Academic Profile</h2>
          </div>
          <div className="cg-summary-main">
            <div className="cg-summary-copy">
              {profile.isPending ? (
                <p role="status">Loading your academic profile...</p>
              ) : profile.isError || !p ? (
                <p role="alert">
                  Your profile could not load.{" "}
                  <button
                    className="cg-text-button"
                    onClick={() => profile.refetch()}
                  >
                    Retry profile
                  </button>
                </p>
              ) : (
                <>
                  <strong className="cg-summary-value">
                    {[p?.firstName, p?.lastName].filter(Boolean).join(" ") ||
                      user?.fullName ||
                      "Student profile"}
                  </strong>
                  <p className="cg-profile-fact">
                    <GraduationCap size={16} aria-hidden="true" />
                    <span>
                      {p?.selectedGrade ||
                        p?.qualificationLevel ||
                        "Grade not recorded"}
                    </span>
                  </p>
                  <p className="cg-profile-fact">
                    <FileText size={16} aria-hidden="true" />
                    <span>{subjects.length} recorded subjects</span>
                  </p>
                  <p className="cg-profile-fact">
                    <BarChart3 size={16} aria-hidden="true" />
                    <span>
                      {marks.length
                        ? `${Math.round(marks.reduce((sum, mark) => sum + mark, 0) / marks.length)}% average across ${marks.length} recorded percentage results`
                        : "Academic average not available"}
                    </span>
                  </p>
                </>
              )}
            </div>
            <SummaryCardIllustration kind="academic" />
          </div>
          {!profile.isPending &&
            !profile.isError &&
            p?.profileCompleteness != null && (
              <div className="cg-completion">
                <span>
                  Profile completion <strong>{p.profileCompleteness}%</strong>
                </span>
                <progress
                  value={p.profileCompleteness}
                  max={100}
                  aria-label="Profile completion"
                />
              </div>
            )}
          <Link className="cg-text-link" to="/student/profile?section=academic">
            Update Student Profile <ArrowRight size={14} />
          </Link>
        </article>
        <article className="cg-panel ed-panel">
          <div className="cg-card-heading">
            <span className="cg-summary-icon">
              <FileText size={20} aria-hidden="true" />
            </span>
            <h2>Transcript Analysis</h2>
          </div>
          <div className="cg-summary-main">
            <div className="cg-summary-copy">
              <strong className="cg-summary-value">{transcriptStatus}</strong>
              {!p?.transcriptFileUrl &&
                !profile.isPending &&
                !profile.isError && (
                  <p className="cg-summary-explanation">
                    Upload your academic transcript to help EduRite understand
                    your results and recommend suitable career pathways.
                  </p>
                )}
              {p?.transcriptFileUrl && (
                <>
                  <p className="cg-filename">
                    {transcriptName(p.transcriptFileUrl)}
                  </p>
                  {latest && (
                    <>
                      <p>{latest.extraction.message}</p>
                      {latest.extraction.subjects.length > 0 && (
                        <p>
                          {latest.extraction.subjects
                            .map((item) => item.subjectName)
                            .join(", ")}
                        </p>
                      )}
                    </>
                  )}
                  {p.curriculum && <p>{p.curriculum}</p>}
                  {aps.data?.status !== "UNAVAILABLE" &&
                    aps.data?.totalAps != null && (
                      <p>
                        General APS: {aps.data.totalAps}
                        {aps.data.status === "PROVISIONAL"
                          ? " (provisional)"
                          : ""}
                      </p>
                    )}
                </>
              )}
              {transcripts.isError && (
                <button
                  className="cg-text-button"
                  onClick={() => transcripts.refetch()}
                >
                  Retry transcript status
                </button>
              )}
            </div>
            <SummaryCardIllustration kind="transcript" />
          </div>
          <Link
            className="cg-text-link"
            to={
              p?.transcriptFileUrl
                ? "/student/profile?section=academic"
                : "/student/documents"
            }
          >
            {p?.transcriptFileUrl
              ? "View Academic Results"
              : "Upload Academic Transcript"}{" "}
            <ArrowRight size={14} />
          </Link>
        </article>
        <article className="cg-panel ed-panel">
          <div className="cg-card-heading">
            <span className="cg-summary-icon">
              <Sparkles size={20} aria-hidden="true" />
            </span>
            <h2>AI Analysis Status</h2>
          </div>
          <div className="cg-summary-main">
            <div className="cg-summary-copy">
              <strong className="cg-summary-value">
                {ai.isPending
                  ? "Generating your personalised career guidance..."
                  : ai.result
                    ? `${ai.result.response.recommendedCareers.length} AI career recommendations`
                    : "AI guidance not generated yet"}
              </strong>
              {!ai.result && !ai.isPending && (
                <p className="cg-summary-explanation">
                  Generate personalised career recommendations based on your
                  academic profile, interests, and goals.
                </p>
              )}
              {ai.result && (
                <p>
                  {ai.result.saved
                    ? "Saved report generated on "
                    : "Generated in this session on "}
                  {new Date(ai.result.generatedAt).toLocaleString()}.
                </p>
              )}
            </div>
            <SummaryCardIllustration kind="ai" />
          </div>
          {ai.result && (
            <button
              className="cg-text-link"
              disabled={Boolean(blockReason)}
              onClick={() => void ai.generate()}
            >
              <RefreshCw size={14} />
              Refresh Recommendations
            </button>
          )}
        </article>
      </div>
      <section
        className="cg-panel ed-panel cg-ai-action"
        aria-label="Generate AI guidance"
      >
        <div className="cg-generation-copy">
          <span className="cg-generation-icon" aria-hidden="true">
            <Sparkles size={24} />
          </span>
          <div>
            <h2>Generate AI Guidance</h2>
            <p>
              Use your saved profile and academic evidence to explore suitable
              careers.
            </p>
            <AiUsageDisplay />
          </div>
        </div>
        <button
          className="cg-button ep-primary"
          disabled={ai.isPending || Boolean(blockReason)}
          onClick={() => void ai.generate()}
        >
          <Sparkles size={16} />
          {ai.isPending ? "Generating..." : "Generate AI Guidance"}
        </button>
        {ai.isPending && (
          <p className="cg-feedback" role="status">
            <RefreshCw size={16} className="cg-spin" />
            Generating your personalised career guidance... {ai.stage}
          </p>
        )}
        {blockReason && !ai.isPending && (
          <p className="cg-generation-notice" role="status">
            {blockReason}
            {access.isError && (
              <button
                className="cg-text-button"
                onClick={() => access.refetch()}
              >
                Retry access
              </button>
            )}
          </p>
        )}
        {ai.error && (
          <div className="cg-feedback" role="alert">
            <p>{ai.error}</p>
            <div className="cg-inline-links">
              <button
                className="cg-text-button"
                disabled={
                  ai.isSubmitting ||
                  (Boolean(blockReason) && !ai.canRetryStatus)
                }
                onClick={() => void ai.retry()}
              >
                Retry AI Guidance
              </button>
              <Link
                className="cg-text-link"
                to="/student/profile?section=academic"
              >
                Review profile
              </Link>
              <Link className="cg-text-link" to="/student/subscription">
                View plan and usage
              </Link>
              <Link className="cg-text-link" to="/auth/login">
                Sign in again
              </Link>
            </div>
          </div>
        )}
      </section>
      {ai.result && <CareerResearchReportView report={ai.result.report} />}
    </>
  );
}
