import { useId, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  BookOpen,
  Bookmark,
  BriefcaseBusiness,
  ChartNoAxesCombined,
  CodeXml,
  GraduationCap,
  HeartPulse,
  HardHat,
  Route,
  Sparkles,
  Wallet,
} from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { useAppQuery } from "@/hooks/useAppQuery";
import { useSubscriptionAccess } from "@/features/subscriptions/access";
import { featureModulesService } from "@/services/featureModulesService";
import { studentService } from "@/services/studentService";
import { bursaryService } from "@/services/bursaryService";
import { InstitutionLogo } from "@/components/institutions/InstitutionLogo";
import { resolveInstitutionDisplay } from "@/lib/institutionRegistry";
import type { StudentProfile, UniversityRequirementMatch } from "@/types";
import {
  careerPathLink,
  compareRequirement,
  subjectResult,
  type GuidanceCareer,
} from "./careerGuidanceModel";

const categoryIcons = {
  Technology: CodeXml,
  "Health Sciences": HeartPulse,
  Engineering: HardHat,
  Business: ChartNoAxesCombined,
  Education: GraduationCap,
  "Other Careers": BriefcaseBusiness,
};

// Decorative category illustrations use the same icon system as the portal.
export function CareerVisual({ category }: { category: string }) {
  const Icon =
    categoryIcons[category as keyof typeof categoryIcons] ?? BriefcaseBusiness;
  return (
    <div className="cg-career-visual" aria-hidden="true">
      <span className="cg-art-ring" />
      <span className="cg-art-dot" />
      <div className="cg-art-card">
        <Icon size={53} strokeWidth={1.25} />
      </div>
      <span className="cg-art-caption">{category}</span>
      <Sparkles className="cg-art-spark" size={20} />
    </div>
  );
}

export function CareerSaveButton({
  id,
  title,
}: {
  id?: string;
  title: string;
}) {
  const { user } = useAuth();
  const client = useQueryClient();
  const saved = useAppQuery({
    queryKey: ["saved-careers", user?.id],
    queryFn: studentService.savedCareers,
    enabled: Boolean(user?.id && id),
  });
  const isSaved = Boolean(id && saved.data?.includes(id));
  const save = useMutation({
    mutationFn: () =>
      isSaved
        ? studentService.unsaveCareer(id!)
        : studentService.saveCareer(id!),
    onSuccess: () => {
      for (const key of [
        "saved-careers",
        "explore-saved-careers",
        "dashboard",
        "dash",
      ])
        void client.invalidateQueries({ queryKey: [key] });
    },
  });
  if (!id)
    return (
      <Link className="cg-text-link" to={careerPathLink(title)}>
        Find in Career Explorer <ArrowRight size={13} />
      </Link>
    );
  return (
    <div>
      <button
        className="cg-save"
        aria-label={`${isSaved ? "Unsave" : "Save"} ${title}`}
        aria-pressed={isSaved}
        disabled={saved.isPending || saved.isError || save.isPending}
        onClick={() => save.mutate()}
      >
        <Bookmark size={16} fill={isSaved ? "currentColor" : "none"} />
        {save.isPending
          ? "Saving..."
          : isSaved
            ? "Saved career"
            : "Save career"}
      </button>
      {saved.isError && (
        <p role="alert">
          Saved careers unavailable.{" "}
          <button className="cg-text-button" onClick={() => saved.refetch()}>
            Retry
          </button>
        </p>
      )}
      {save.isError && (
        <p role="alert">Could not update saved career. Please retry.</p>
      )}
    </div>
  );
}

const tabs = [
  "Overview",
  "Requirements",
  "Universities",
  "Career Path",
  "Skills",
  "Opportunities",
] as const;
type Tab = (typeof tabs)[number];
const requirementFields = [
  ["Mathematics", "mathematicsRequirement"],
  ["Mathematical Literacy", "mathematicalLiteracyRequirement"],
  ["English", "englishRequirement"],
  ["Physical Sciences", "physicalSciencesRequirement"],
  ["Life Sciences", "lifeSciencesRequirement"],
  ["Accounting", "accountingRequirement"],
] as const;

export function CareerSubjectTags({ title }: { title: string }) {
  const { user } = useAuth();
  const requirements = useAppQuery({
    queryKey: ["guidance-requirements", user?.id, title],
    queryFn: () => featureModulesService.careerRequirementMatches(title),
    enabled: Boolean(user?.id),
  });
  const subjects = requirementFields
    .filter(([, field]) =>
      requirements.data?.some((item) => item.verified && item[field]),
    )
    .map(([name]) => name);
  return (
    <div className="cg-card-subjects">
      <h4>Programme subjects</h4>
      {subjects.length ? (
        <>
          <div className="cg-skill-chips">
            {subjects.map((subject) => (
              <span className="cg-pill" key={subject}>
                {subject}
              </span>
            ))}
          </div>
          <p className="cg-muted">
            Requirements vary by programme; review alternatives below.
          </p>
        </>
      ) : (
        <p className="cg-muted">
          {requirements.isPending
            ? "Loading verified subject requirements..."
            : requirements.isError
              ? "Subject requirements are temporarily unavailable."
              : "No verified subject requirements recorded yet."}
        </p>
      )}
    </div>
  );
}

export function AcademicRequirements({
  programmes,
  profile,
}: {
  programmes: UniversityRequirementMatch[];
  profile?: StudentProfile;
}) {
  const [programmeId, setProgrammeId] = useState("");
  const verified = programmes.filter((item) => item.verified);
  const programme =
    verified.find(
      (item, index) => (item.id || String(index)) === programmeId,
    ) ?? verified[0];
  const rows = requirementFields.filter(([, field]) => programme?.[field]);
  return (
    <section className="cg-requirements">
      <h3>Academic Requirements</h3>
      <p>
        Compare your recorded results with a verified programme. Subject
        comparisons alone do not establish admission eligibility.
      </p>
      {verified.length > 0 && (
        <label className="cg-programme-select">
          Programme to compare
          <select
            value={programmeId || programme?.id || "0"}
            onChange={(event) => setProgrammeId(event.target.value)}
          >
            {verified.map((item, index) => (
              <option key={item.id || index} value={item.id || String(index)}>
                {item.institutionName} · {item.qualificationName}
              </option>
            ))}
          </select>
        </label>
      )}
      {programme ? (
        <>
          <p>
            <strong>{programme.qualificationName}</strong> ·{" "}
            {programme.institutionName}
            {programme.admissionYear
              ? ` · ${programme.admissionYear} requirements`
              : ""}
          </p>
          <div className="cg-table-scroll">
            <table>
              <caption className="sr-only">
                Recorded subject results compared with verified programme
                requirements
              </caption>
              <thead>
                <tr>
                  <th>Subject</th>
                  <th>Student result</th>
                  <th>Requirement</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {rows.map(([subject, field]) => {
                  const result = profile?.subjectAchievements?.find(
                    (item) =>
                      item.subjectName.trim().toLowerCase() ===
                      subject.toLowerCase(),
                  );
                  const status = compareRequirement(programme[field]!, result);
                  return (
                    <tr key={field}>
                      <th scope="row">{subject}</th>
                      <td>
                        {subjectResult(result)}
                        {result && (
                          <small>
                            {result.verified
                              ? "Reviewed result"
                              : "Unverified result"}
                          </small>
                        )}
                      </td>
                      <td>{programme[field]}</td>
                      <td>
                        <span
                          className={`cg-pill ${status === "Met" && result?.verified ? "cg-met" : status === "Gap" ? "cg-gap" : ""}`}
                        >
                          {status === "Met"
                            ? result?.verified
                              ? "Requirement met"
                              : "Meets recorded threshold · unverified"
                            : status === "Gap"
                              ? "Gap to address"
                              : "Cannot determine"}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {!rows.length && (
            <p>
              No comparable subject thresholds are recorded for this programme.
            </p>
          )}
          {programme.apsRequired != null && (
            <p>
              Required APS: {programme.apsRequired}.{" "}
              {programme.apsRule ||
                "Institution APS scoring rule unavailable; eligibility cannot be determined."}
            </p>
          )}
          {[
            programme.generalAdmissionRequirements,
            programme.programmeAdmissionRequirements,
            programme.additionalSelectionRequirements,
          ]
            .filter(Boolean)
            .map((text, index) => (
              <p key={index}>{text}</p>
            ))}
          <p className="cg-note">
            Check the institution’s current admission criteria before applying.
          </p>
        </>
      ) : (
        <div className="cg-empty">
          <GraduationCap size={25} />
          <p>
            No verified programme requirements are available for this career
            yet.
          </p>
          <Link
            className="cg-text-link"
            to="/student/study-options?section=universities"
          >
            Explore university requirements <ArrowRight size={14} />
          </Link>
        </div>
      )}
    </section>
  );
}

function UniversityCards({
  programmes,
  title,
}: {
  programmes: UniversityRequirementMatch[];
  title: string;
}) {
  if (!programmes.length)
    return (
      <div className="cg-empty">
        <GraduationCap size={27} />
        <p>No linked university programmes are recorded for {title} yet.</p>
        <Link
          className="cg-text-link"
          to={`/student/study-options?section=programmes&q=${encodeURIComponent(title)}`}
        >
          Explore university programmes <ArrowRight size={14} />
        </Link>
      </div>
    );
  return (
    <div className="cg-universities">
      {programmes.map((item, index) => {
        const institution = resolveInstitutionDisplay({
          name: item.institutionName,
          province: item.province,
          applicationUrl: item.applicationUrl,
          institutionType: item.institutionType,
        });
        return (
          <article
            className="cg-university"
            key={item.id || `${item.institutionName}-${index}`}
          >
            <InstitutionLogo
              src={institution.logoUrl}
              institutionName={institution.displayName}
              abbreviation={institution.abbreviation}
              size={56}
            />
            <div>
              <h4>{item.institutionName}</h4>
              <p>{item.qualificationName}</p>
              <span className="cg-pill">
                {item.verified
                  ? "Verified programme record"
                  : "Requirements need verification"}
              </span>
              <p>
                {item.apsRequired != null
                  ? `Required APS: ${item.apsRequired}`
                  : "APS requirement not recorded"}
              </p>
              <p>Eligibility: review current requirements</p>
              {item.programmeAdmissionRequirements && (
                <p>{item.programmeAdmissionRequirements}</p>
              )}
              <Link
                className="cg-text-link"
                to={
                  careerPathLink(title) +
                  (item.id ? `&programme=${encodeURIComponent(item.id)}` : "")
                }
              >
                View Programme <ArrowRight size={14} />
              </Link>
            </div>
          </article>
        );
      })}
    </div>
  );
}

function CareerOpportunities({ career }: { career: GuidanceCareer }) {
  const { user } = useAuth();
  const funding = useAppQuery({
    queryKey: ["guidance-bursaries", user?.id],
    queryFn: bursaryService.recommended,
    enabled: Boolean(user?.id),
  });
  // Existing endpoint personalises to the student, not to a selected career.
  // Only show entries whose published text actually references this career.
  const terms = career.title
    .toLowerCase()
    .split(/\W+/)
    .filter((term) => term.length > 3);
  const items =
    funding.data?.filter((item) =>
      terms.some((term) =>
        `${item.title} ${item.eligibility ?? ""}`.toLowerCase().includes(term),
      ),
    ) ?? [];
  return (
    <>
      <h3>Relevant Bursaries & Opportunities</h3>
      {funding.isPending && (
        <p role="status">Loading your funding recommendations...</p>
      )}
      {funding.isError && (
        <p role="alert">
          Opportunities could not load.{" "}
          <button className="cg-text-button" onClick={() => funding.refetch()}>
            Retry
          </button>
        </p>
      )}
      <div className="cg-universities">
        {items.map((item) => (
          <article className="cg-university" key={item.externalId}>
            <Wallet size={28} />
            <div>
              <h4>{item.title}</h4>
              <p>{item.provider}</p>
              {item.deadline && <p>Deadline: {item.deadline}</p>}
              <p>{item.eligibility}</p>
              <Link
                className="cg-text-link"
                to={`/student/funding?section=bursaries&q=${encodeURIComponent(item.title)}`}
              >
                View funding opportunity <ArrowRight size={14} />
              </Link>
            </div>
          </article>
        ))}
      </div>
      {!funding.isPending && !funding.isError && !items.length && (
        <p className="cg-empty">
          No current funding recommendations explicitly reference this career.
          Explore the full directory for other bursaries, scholarships and
          internships.
        </p>
      )}
      <Link
        className="cg-text-link"
        to={`/student/funding?section=discover&q=${encodeURIComponent(career.title)}`}
      >
        Explore relevant opportunities <ArrowRight size={14} />
      </Link>
    </>
  );
}

export function CareerGuidanceDetails({
  career,
  profile,
  onCompare,
}: {
  career: GuidanceCareer;
  profile?: StudentProfile;
  onCompare?: () => void;
}) {
  const { user } = useAuth();
  const access = useSubscriptionAccess();
  const canReadRoadmaps = Boolean(
    access.data?.entitlements.includes("CAREER_ROADMAP_PERSONALISED"),
  );
  const [tab, setTab] = useState<Tab>("Overview");
  const id = useId();
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const requirements = useAppQuery({
    queryKey: ["guidance-requirements", user?.id, career.title],
    queryFn: () => featureModulesService.careerRequirementMatches(career.title),
    enabled: Boolean(user?.id),
  });
  const roadmaps = useAppQuery({
    queryKey: ["student-career-roadmaps-saved", user?.id, access.data?.plan],
    queryFn: featureModulesService.savedCareerRoadmaps,
    enabled: Boolean(user?.id && canReadRoadmaps),
  });
  const saved = (canReadRoadmaps ? roadmaps.data : undefined)
    ?.filter(
      (item) =>
        item.careerName.trim().toLowerCase() ===
        career.title.trim().toLowerCase(),
    )
    .sort((a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt))[0];
  const roadmap = saved?.roadmap;
  const programmes = requirements.data ?? [];
  const steps = roadmap?.roadmapTimeline ?? [];
  const nextSteps = [
    [
      "Explore University Programmes",
      `/student/study-options?section=programmes&q=${encodeURIComponent(career.title)}`,
      GraduationCap,
    ],
    ["Compare Similar Careers", "#career-recommendations", BriefcaseBusiness],
    [
      "Create My AI Study Plan",
      careerPathLink(career.title, "study-plan"),
      BookOpen,
    ],
    [
      "Explore Relevant Bursaries",
      `/student/funding?section=discover&q=${encodeURIComponent(career.title)}`,
      Wallet,
    ],
    ["View My Career Roadmap", careerPathLink(career.title), Route],
  ] as const;
  return (
    <section
      className="cg-panel cg-details"
      aria-label={`${career.title} career details`}
    >
      <div className="cg-detail-hero">
        <CareerVisual category={career.category} />
        <div>
          <span className="cg-eyebrow">YOUR CAREER, IN FOCUS</span>
          <h2>{career.title}</h2>
          <span className="cg-category">{career.category}</span>
          <span className="cg-match">
            <Sparkles size={12} />
            {career.ai ? "AI suggested" : "Evidence-backed suggestion"}
          </span>
          <p>
            {career.description ||
              "Explore the recommendation evidence and available pathways for this career."}
          </p>
          <CareerSaveButton id={career.id} title={career.title} />
        </div>
      </div>
      <div className="cg-tabs" role="tablist" aria-label="Career details">
        {tabs.map((item, index) => (
          <button
            key={item}
            ref={(node) => {
              tabRefs.current[index] = node;
            }}
            role="tab"
            id={`${id}-tab-${index}`}
            aria-controls={`${id}-panel`}
            aria-selected={tab === item}
            tabIndex={tab === item ? 0 : -1}
            onClick={() => setTab(item)}
            onKeyDown={(event) => {
              const next =
                event.key === "ArrowRight"
                  ? (index + 1) % tabs.length
                  : event.key === "ArrowLeft"
                    ? (index + tabs.length - 1) % tabs.length
                    : event.key === "Home"
                      ? 0
                      : event.key === "End"
                        ? tabs.length - 1
                        : null;
              if (next != null) {
                event.preventDefault();
                setTab(tabs[next]);
                tabRefs.current[next]?.focus();
              }
            }}
          >
            {item}
          </button>
        ))}
      </div>
      <div
        className="cg-tab-panel"
        role="tabpanel"
        id={`${id}-panel`}
        aria-labelledby={`${id}-tab-${tabs.indexOf(tab)}`}
        tabIndex={0}
      >
        {tab === "Overview" && (
          <div className="cg-two-columns">
            <div>
              <h3>
                Why {career.ai ? "Gemini" : "EduRite"} recommended this career
              </h3>
              <ul className="cg-list">
                {career.reasons.map((text, index) => (
                  <li key={index}>
                    <Sparkles size={15} />
                    <span>{text}</span>
                  </li>
                ))}
              </ul>
              {roadmap?.overview.description && (
                <>
                  <h3>Saved career overview</h3>
                  <p>{roadmap.overview.description}</p>
                </>
              )}
            </div>
            <div>
              <h3>Career Responsibilities</h3>
              {roadmap?.overview.dailyResponsibilities?.length ? (
                <ul>
                  {roadmap.overview.dailyResponsibilities.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              ) : (
                <p>
                  Generate a personalised roadmap in Career Explorer to review
                  responsibilities and professional pathways.
                </p>
              )}
              <Link className="cg-text-link" to={careerPathLink(career.title)}>
                Explore this career <ArrowRight size={14} />
              </Link>
            </div>
          </div>
        )}
        {(tab === "Requirements" || tab === "Universities") && (
          <>
            {requirements.isPending && (
              <p role="status">Loading programme requirements...</p>
            )}
            {requirements.isError && (
              <p role="alert">
                Programme requirements could not load.{" "}
                <button
                  className="cg-text-button"
                  onClick={() => requirements.refetch()}
                >
                  Retry
                </button>
              </p>
            )}
            {!requirements.isPending &&
              !requirements.isError &&
              (tab === "Requirements" ? (
                <AcademicRequirements
                  programmes={programmes}
                  profile={profile}
                />
              ) : (
                <>
                  <h3>Suitable Universities & Programmes</h3>
                  <UniversityCards
                    programmes={programmes}
                    title={career.title}
                  />
                </>
              ))}
          </>
        )}
        {tab === "Career Path" && (
          <>
            <h3>Your Career Progression Roadmap</h3>
            {roadmaps.isFetching && (
              <p role="status">Loading saved pathways...</p>
            )}
            {roadmaps.isError && (
              <p role="alert">
                Your saved roadmap could not load.{" "}
                <button
                  className="cg-text-button"
                  onClick={() => roadmaps.refetch()}
                >
                  Retry
                </button>
              </p>
            )}
            {steps.length ? (
              <ol className="cg-roadmap">
                {steps.map((step, index) => (
                  <li key={`${step.title}-${index}`}>
                    <span>{index + 1}</span>
                    <div>
                      <h4>{step.title}</h4>
                      <p>{step.description}</p>
                    </div>
                  </li>
                ))}
              </ol>
            ) : (
              <div className="cg-empty">
                <Route size={30} />
                <p>No personalised roadmap has been saved for this career.</p>
                <p>
                  {profile?.selectedGrade
                    ? `Build a pathway from ${profile.selectedGrade} using your saved academic results.`
                    : "Add your current grade and academic results to personalise your pathway."}
                </p>
              </div>
            )}
            <Link className="cg-button" to={careerPathLink(career.title)}>
              Create / View Career Roadmap <ArrowRight size={14} />
            </Link>
          </>
        )}
        {tab === "Skills" && (
          <>
            <h3>Skills to Develop</h3>
            {roadmap?.overview.skillsNeeded?.length ? (
              <div className="cg-skill-chips">
                {roadmap.overview.skillsNeeded.map((skill) => (
                  <span className="cg-pill" key={skill}>
                    {skill}
                  </span>
                ))}
              </div>
            ) : (
              <p>
                A saved roadmap can identify technical and professional skills
                for this career.
              </p>
            )}
            {career.improvements.length > 0 && (
              <>
                <h3>Personalised learning recommendations</h3>
                <ul>
                  {career.improvements.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </>
            )}
            {roadmap?.studyPlan?.map((item) => (
              <article className="cg-learning-step" key={item.title}>
                <h4>{item.title}</h4>
                <p>{item.focus}</p>
                <ul>
                  {item.actions.map((action) => (
                    <li key={action}>{action}</li>
                  ))}
                </ul>
              </article>
            ))}
            <Link
              className="cg-text-link"
              to={careerPathLink(career.title, "learning-path")}
            >
              Explore your learning path <ArrowRight size={14} />
            </Link>
          </>
        )}
        {tab === "Opportunities" && <CareerOpportunities career={career} />}
        {saved && (
          <p className="cg-note">
            Roadmap information is a saved snapshot from{" "}
            {new Date(saved.updatedAt).toLocaleDateString()}. Refresh it in
            Career Explorer after changing your profile. Programme requirements
            load separately from current catalogue records.
          </p>
        )}
      </div>
      <section className="cg-next-steps">
        <span className="cg-eyebrow">TURN DIRECTION INTO ACTION</span>
        <h3>Your Next Steps</h3>
        <div>
          {nextSteps.map(([label, to, Icon]) =>
            to.startsWith("#") && onCompare ? (
              <button key={label} onClick={onCompare}>
                <Icon size={19} />
                <span>{label}</span>
                <ArrowRight size={14} />
              </button>
            ) : to.startsWith("#") ? (
              <a href={to} key={label}>
                <Icon size={19} />
                <span>{label}</span>
                <ArrowRight size={14} />
              </a>
            ) : (
              <Link to={to} key={label}>
                <Icon size={19} />
                <span>{label}</span>
                <ArrowRight size={14} />
              </Link>
            ),
          )}
          <div className="cg-next-save">
            <Bookmark size={19} />
            <CareerSaveButton id={career.id} title={career.title} />
          </div>
        </div>
      </section>
    </section>
  );
}
