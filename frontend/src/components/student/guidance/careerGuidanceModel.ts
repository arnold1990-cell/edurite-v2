import type {
  Career,
  CareerAdviceResponse,
  Recommendation,
  StudentSubjectAchievement,
} from "@/types";

export const careerCategories = [
  "All Careers",
  "Strong Matches",
  "Technology",
  "Health Sciences",
  "Engineering",
  "Business",
  "Education",
  "Other Careers",
] as const;
export type GuidanceCareer = {
  key: string;
  id?: string;
  title: string;
  category: string;
  description: string;
  reasons: string[];
  improvements: string[];
  ai: boolean;
};

// Display grouping only. These keywords never influence ranking or eligibility.
export function careerCategory(text: string) {
  if (/software|data |comput|technology|cyber|developer| IT\b/i.test(text))
    return "Technology";
  if (/engineer|architect|construction/i.test(text)) return "Engineering";
  if (/health|nurs|doctor|medic|pharma|physio|dent|psycholog/i.test(text))
    return "Health Sciences";
  if (/teach|educat|lectur|tutor/i.test(text)) return "Education";
  if (/business|financ|account|market|manag|econom|entrepreneur/i.test(text))
    return "Business";
  return "Other Careers";
}

export function guidanceCareers(
  data?: Recommendation,
  ai?: CareerAdviceResponse,
  catalogue: Career[] = [],
): GuidanceCareer[] {
  const find = (title: string) =>
    catalogue.find(
      (item) => item.title.trim().toLowerCase() === title.trim().toLowerCase(),
    );
  if (ai)
    return ai.recommendedCareers.map((item, index) => {
      const career = find(item.name);
      return {
        key: `ai-${index}-${item.name}`,
        id: career?.id,
        title: item.name,
        category: careerCategory(`${career?.industry ?? ""} ${item.name}`),
        description: career?.description ?? "",
        reasons: splitReasons(item.reason),
        improvements: item.improvements ?? [],
        ai: true,
      };
    });
  return (data?.suggestedCareers ?? []).map((item) => {
    const career = catalogue.find((c) => c.id === item.id) ?? find(item.title);
    return {
      key: item.id,
      id: item.id,
      title: item.title,
      category: careerCategory(`${career?.industry ?? ""} ${item.title}`),
      description: career?.description ?? "",
      reasons: splitReasons(item.rationale),
      improvements: [],
      ai: false,
    };
  });
}

function splitReasons(text: string) {
  return (text ?? "")
    .split(/\n+|(?<=[.!?])\s+(?=[A-Z])/)
    .map((part) => part.replace(/^[-•]\s*/, "").trim())
    .filter(Boolean);
}

export function filterCareers(
  items: GuidanceCareer[],
  category: string,
  search: string,
) {
  // Current API supplies no per-career validated strong-match level. Do not turn
  // its internal ordering weights or an AI-generated percentage into one.
  return items.filter(
    (item) =>
      category !== "Strong Matches" &&
      (category === "All Careers" || item.category === category) &&
      `${item.title} ${item.category} ${item.description} ${item.reasons.join(" ")}`
        .toLowerCase()
        .includes(search.trim().toLowerCase()),
  );
}

export function subjectResult(subject?: StudentSubjectAchievement) {
  if (
    subject?.markPercentage != null &&
    Number.isFinite(subject.markPercentage) &&
    subject.markPercentage >= 0 &&
    subject.markPercentage <= 100
  )
    return `${subject.markPercentage}%`;
  if (subject?.achievementLevel != null)
    return `Level ${subject.achievementLevel}`;
  return "Not recorded";
}

// Only unambiguous percentage/level requirements can be compared. Compound,
// alternative and prose requirements remain unknown and are shown verbatim.
export function compareRequirement(
  requirement: string | undefined,
  subject?: StudentSubjectAchievement,
) {
  const value = requirement?.trim() ?? "";
  const percent = value.match(
    /^(?:(?:minimum|at least|min\.?)\s*)?(\d{1,3})\s*%\+?$/i,
  );
  const level = value.match(/^(?:minimum\s+)?(?:level\s+)([1-7])\+?$/i);
  const recorded = percent
    ? subject?.markPercentage
    : level
      ? subject?.achievementLevel
      : null;
  const required = percent
    ? Number(percent[1])
    : level
      ? Number(level[1])
      : null;
  if (
    recorded == null ||
    !Number.isFinite(recorded) ||
    required == null ||
    required > 100 ||
    recorded < 0 ||
    (level && (!Number.isInteger(recorded) || recorded < 1 || recorded > 7)) ||
    (percent && recorded > 100)
  )
    return "Unknown";
  return recorded >= required ? "Met" : "Gap";
}

export const careerPathLink = (title: string, section = "career-path") =>
  `/student/career-explorer?section=${section}&career=${encodeURIComponent(title)}`;

export function transcriptName(path?: string) {
  if (!path) return "";
  const filename = path.split(/[?#]/, 1)[0].split('/').filter(Boolean).pop() ?? "";
  return filename.replace(/^transcript-[0-9a-f-]{36}-/i, "");
}
