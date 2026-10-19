import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { aiGuidanceService } from "@/services/aiGuidanceService";
import type { CareerAdviceResponse, StudentProfile } from "@/types";
import type {
  CareerResearchJob,
  CareerResearchReport,
} from "@/types/careerResearch";
import { useAuth } from "@/hooks/useAuth";

export function validateCareerGuidance(value: unknown): CareerAdviceResponse {
  const response = value as CareerAdviceResponse | null;
  if (
    !response ||
    !Array.isArray(response.recommendedCareers) ||
    !response.recommendedCareers.length ||
    !response.recommendedCareers.every(
      (item) =>
        item &&
        typeof item.name === "string" &&
        item.name.trim() &&
        typeof item.reason === "string" &&
        item.reason.trim() &&
        (item.improvements == null ||
          (Array.isArray(item.improvements) &&
            item.improvements.every((text) => typeof text === "string"))),
    )
  ) {
    throw new Error(
      "No valid career guidance was returned. Please try generating again.",
    );
  }
  return {
    ...response,
    recommendedCareers: response.recommendedCareers.map((item) => ({
      ...item,
      name: item.name.trim(),
      reason: item.reason.trim(),
      matchScore: null,
      improvements: item.improvements ?? [],
    })),
  };
}

export function hasGuidanceEvidence(
  profile?: StudentProfile,
  inputsUsed?: string[],
) {
  return Boolean(
    profile &&
    (inputsUsed?.length ||
      profile.subjectAchievements?.some(
        (item) =>
          item.subjectName.trim() &&
          ((item.markPercentage != null &&
            Number.isFinite(item.markPercentage) &&
            item.markPercentage >= 0 &&
            item.markPercentage <= 100) ||
            (item.achievementLevel != null &&
              Number.isInteger(item.achievementLevel) &&
              item.achievementLevel >= 1 &&
              item.achievementLevel <= 7)),
      ) ||
      profile.interests?.some((item) => item.trim()) ||
      profile.skills?.some((item) => item.trim()) ||
      profile.careerGoals?.trim() ||
      profile.preferredCareer?.trim()),
  );
}

export function guidanceError(error: unknown) {
  const value = error as {
    status?: number;
    code?: string;
    message?: string;
  } | null;
  if (value?.status === 401)
    return "Your session has expired. Sign in again to generate AI guidance.";
  if (value?.status === 403)
    return "AI guidance is not available with your current access. Review your subscription or try again after access is restored.";
  if (value?.status === 429)
    return "Your AI interaction limit has been reached or requests are temporarily limited. Review your usage before retrying.";
  if (value?.status === 400 || value?.status === 422)
    return "Your saved information could not be used for guidance. Review your academic results and profile, then retry.";
  if (value?.message?.startsWith("No valid career guidance"))
    return value.message;
  return "AI guidance could not be generated. Check your connection and try again. If the problem continues, the AI service may be temporarily unavailable.";
}

export function validateResearchJob(value: unknown): CareerResearchJob {
  const job = value as CareerResearchJob;
  if (
    !job ||
    !["IDLE", "QUEUED", "RUNNING", "SUCCEEDED", "FAILED", "STALE"].includes(
      job.status,
    )
  )
    throw new Error("No valid career guidance was returned. Please retry.");
  if (job.status === "SUCCEEDED") {
    const report = job.report;
    if (
      !report ||
      report.schemaVersion !== "career-research-v1" ||
      !Array.isArray(report.careers) ||
      !report.careers.length ||
      !report.careers.every(
        (c) =>
          c &&
          typeof c.id === "string" &&
          typeof c.title === "string" &&
          c.title.trim() &&
          typeof c.explanation === "string" &&
          c.explanation.trim() &&
          Array.isArray(c.improvementAreas),
      ) ||
      !Array.isArray(report.programmes) ||
      !Array.isArray(report.sources) ||
      !report.academic ||
      !Array.isArray(report.academic.subjects) ||
      !report.interpretation ||
      typeof report.interpretation.assessment !== "string" ||
      !report.interpretation.assessment.trim() ||
      !Array.isArray(report.interpretation.nextSteps) ||
      !Array.isArray(report.funding) ||
      !Array.isArray(report.alternativePathways) ||
      !Array.isArray(report.limitations) ||
      !Array.isArray(report.inputsUsed) ||
      !Number.isFinite(Date.parse(report.generatedAt))
    )
      throw new Error(
        "No valid career guidance was returned. Please try generating again.",
      );
  }
  if (job.status === "SUCCEEDED" && job.report) {
    const r = job.report;
    const strings = (v: unknown): v is string[] =>
      Array.isArray(v) && v.every((t) => typeof t === "string");
    const programmes = [...r.programmes, ...r.alternativePathways];
    if (
      !strings(r.academic.curricula) ||
      !strings(r.academic.interests) ||
      !strings(r.academic.skills) ||
      !strings(r.academic.limitations) ||
      !Array.isArray(r.academic.relativeStrengths) ||
      !Array.isArray(r.academic.improvementPriorities) ||
      !strings(r.interpretation.nextSteps) ||
      !strings(r.interpretation.sourceUrls) ||
      typeof r.interpretation.researchSummary !== "string" ||
      !strings(r.inputsUsed) ||
      !strings(r.limitations) ||
      typeof r.rankingMethod !== "string" ||
      !r.careers.every(
        (c) =>
          strings(c.relevantSubjects) &&
          strings(c.strengths) &&
          strings(c.improvementAreas) &&
          strings(c.qualifications) &&
          strings(c.programmeIds) &&
          typeof c.category === "string",
      ) ||
      !programmes.every(
        (p) =>
          p &&
          typeof p.id === "string" &&
          typeof p.name === "string" &&
          typeof p.institution === "string" &&
          typeof p.verificationStatus === "string" &&
          strings(p.careerIds) &&
          p.eligibility &&
          typeof p.eligibility.status === "string" &&
          Array.isArray(p.eligibility.subjects) &&
          strings(p.eligibility.gaps) &&
          strings(p.eligibility.uncheckedConditions),
      ) ||
      !r.sources.every(
        (source) =>
          source &&
          typeof source.url === "string" &&
          typeof source.status === "string",
      ) ||
      !r.funding.every(
        (f) =>
          f && typeof f.externalId === "string" && typeof f.title === "string",
      )
    ) {
      throw new Error(
        "No valid career guidance was returned. Please try generating again.",
      );
    }
  }
  return job;
}

// A GET only reads durable status/results. A POST starts one guarded, server-
// deduplicated background job; neither polling nor retries consume AI credits.
export function useCareerGuidance(scope: string, blockReason?: string) {
  const { user } = useAuth();
  const client = useQueryClient();
  const inFlight = useRef(false);
  const requestKey = useRef<string>();
  const started = useRef<string | null>(null);
  const key = ["career-research", user?.id, scope];
  const status = useQuery({
    queryKey: key,
    queryFn: async () =>
      validateResearchJob(await aiGuidanceService.careerResearchStatus()),
    enabled: Boolean(user?.id),
    retry: false,
    refetchInterval: (q) =>
      ["QUEUED", "RUNNING"].includes(q.state.data?.status ?? "") ? 1500 : false,
  });
  useEffect(() => {
    if (["SUCCEEDED", "FAILED", "STALE"].includes(status.data?.status ?? "")) {
      void client.invalidateQueries({ queryKey: ["subscription-access"] });
      void client.invalidateQueries({
        queryKey: ["career-research-study-options", user?.id],
      });
    }
  }, [status.data?.status, status.data?.jobId, user?.id, client]);
  const mutation = useMutation({
    mutationKey: ["career-research-start", user?.id, scope],
    retry: false,
    networkMode: "always",
    mutationFn: async () => {
      await client.cancelQueries({ queryKey: key });
      const job = validateResearchJob(
        await aiGuidanceService.startCareerResearch(requestKey.current),
      );
      started.current = job.jobId;
      client.setQueryData(key, job);
      return job;
    },
    onSettled: () => {
      void client.invalidateQueries({ queryKey: ["subscription-access"] });
    },
  });
  const tracking = ["QUEUED", "RUNNING"].includes(status.data?.status ?? "");
  const generate = async (retrySameRequest = false) => {
    if (inFlight.current || tracking || blockReason) return;
    if (!retrySameRequest || !requestKey.current)
      requestKey.current = crypto.randomUUID();
    inFlight.current = true;
    mutation.reset();
    try {
      await mutation.mutateAsync();
    } catch {
      /* Reported below; no automatic chargeable retry. */
    } finally {
      inFlight.current = false;
    }
  };
  const report: CareerResearchReport | undefined =
    status.data?.status === "SUCCEEDED" &&
    !mutation.isPending &&
    !mutation.isError &&
    !status.isError
      ? (status.data.report ?? undefined)
      : undefined;
  const result = report
    ? {
        report,
        scope,
        generatedAt: report.generatedAt,
        saved: started.current !== status.data?.jobId,
        response: {
          recommendedCareers: report.careers.map((c) => ({
            name: c.title,
            reason: c.explanation,
            improvements: c.improvementAreas,
            matchScore: null,
          })),
        } as CareerAdviceResponse,
      }
    : undefined;
  const error = mutation.isError
    ? guidanceError(mutation.error)
    : status.isError
      ? "Research progress could not be retrieved. Retry the status check; no new AI interaction is needed."
      : status.data?.status === "FAILED" || status.data?.status === "STALE"
        ? status.data.error || "Research could not be completed. Please retry."
        : undefined;
  return {
    generate,
    result,
    isPending: mutation.isPending || tracking,
    isSubmitting: mutation.isPending,
    isTracking: tracking,
    canRetryStatus: Boolean(blockReason) || tracking || status.isError,
    stage: status.data?.stage,
    error,
    retry: async () => {
      if (blockReason || tracking || status.isError) {
        const fresh = await status.refetch();
        if (
          fresh.data &&
          ["QUEUED", "RUNNING", "SUCCEEDED"].includes(fresh.data.status)
        )
          mutation.reset();
        return;
      }
      await generate(true);
    },
  };
}
