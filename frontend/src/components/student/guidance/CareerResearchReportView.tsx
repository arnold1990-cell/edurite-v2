import { useState } from "react";
import { Link } from "react-router-dom";
import type {
  CareerResearchReport,
  ResearchProgramme,
} from "@/types/careerResearch";
import { CareerComparison } from "./CareerComparison";
import { CareerSaveButton } from "./CareerGuidanceDetails";
import { careerPathLink, subjectResult } from "./careerGuidanceModel";
import { UniversityCoveragePanel } from "./UniversityCoveragePanel";

const href = (url?: string) => {
  try {
    const value = new URL(url ?? "");
    return ["https:", "http:"].includes(value.protocol)
      ? value.href
      : undefined;
  } catch {
    return undefined;
  }
};
const sourceLink = (url?: string, label = "Official source") =>
  href(url) ? (
    <a
      className="cg-text-link"
      href={href(url)}
      target="_blank"
      rel="noopener noreferrer"
    >
      {label} ↗
    </a>
  ) : null;
const unique = (values: (string | undefined)[]) =>
  [...new Set(values.filter((v): v is string => Boolean(v)))].sort();

export function ResearchProgrammeCollection({
  programmes,
  initialCareer,
}: {
  programmes: ResearchProgramme[];
  initialCareer?: string;
}) {
  const [country, setCountry] = useState("");
  const [institution, setInstitution] = useState("");
  const [qualification, setQualification] = useState("");
  const [eligibility, setEligibility] = useState("");
  const [mode, setMode] = useState("");
  const [search, setSearch] = useState("");
  const [count, setCount] = useState(12);
  const [selected, setSelected] = useState("");
  const rows = programmes.filter(
    (p) =>
      (!initialCareer || p.careerIds.includes(initialCareer)) &&
      (!country || p.country === country) &&
      (!institution || p.institution === institution) &&
      (!qualification || p.qualification === qualification) &&
      (!eligibility || p.eligibility.status === eligibility) &&
      (!mode || p.studyMode === mode) &&
      `${p.name} ${p.institution}`.toLowerCase().includes(search.toLowerCase()),
  );
  const selectors = [
    {
      label: "Country",
      value: country,
      change: setCountry,
      values: unique(programmes.map((p) => p.country)),
    },
    {
      label: "University",
      value: institution,
      change: setInstitution,
      values: unique(programmes.map((p) => p.institution)),
    },
    {
      label: "Qualification",
      value: qualification,
      change: setQualification,
      values: unique(programmes.map((p) => p.qualification)),
    },
    {
      label: "Admission Eligibility",
      value: eligibility,
      change: setEligibility,
      values: unique(programmes.map((p) => p.eligibility.status)),
    },
    {
      label: "Study Mode",
      value: mode,
      change: setMode,
      values: unique(programmes.map((p) => p.studyMode)),
    },
  ];
  return (
    <section className="cg-panel ed-panel">
      <div className="cg-section-heading">
        <div>
          <h2>University and Programme Recommendations</h2>
          <p>
            {rows.length} matching programme records. Eligibility checks are
            separate from career suitability and do not guarantee admission.
          </p>
        </div>
      </div>
      <div className="cg-research-filters">
        {selectors.map((s) => (
          <label key={s.label}>
            {s.label}
            <select
              value={s.value}
              onChange={(e) => {
                s.change(e.target.value);
                setCount(12);
              }}
            >
              <option value="">All</option>
              {s.values.map((v) => (
                <option key={v}>{v}</option>
              ))}
            </select>
          </label>
        ))}
        <label>
          Search programmes
          <input
            type="search"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setCount(12);
            }}
            placeholder="Programme or institution"
          />
        </label>
      </div>
      {!rows.length && (
        <p className="cg-empty">
          No supported programme records match these filters.{" "}
          <Link
            className="cg-text-link"
            to="/student/study-options?section=universities"
          >
            Explore institutions
          </Link>
        </p>
      )}
      <div className="cg-universities">
        {rows.slice(0, count).map((p) => (
          <article className="cg-university cg-research-programme" key={p.id}>
            <div>
              <span className="cg-category">
                {p.country || "Country not recorded"}
                {p.campus ? ` · ${p.campus}` : ""}
              </span>
              <h3>{p.name}</h3>
              <p>
                <strong>{p.institution}</strong>
              </p>
              <p>
                {[p.qualification, p.duration, p.studyMode]
                  .filter(Boolean)
                  .join(" · ") || "Qualification details not recorded"}
              </p>
              <p className="cg-muted">
                {p.verificationStatus}
                {p.verifiedAt
                  ? ` · checked ${new Date(p.verifiedAt).toLocaleDateString()}`
                  : ""}
              </p>
              <span
                className={`cg-pill ${p.eligibility.status === "Meets published requirements" ? "cg-met" : p.eligibility.status === "Insufficient information" ? "" : "cg-gap"}`}
              >
                {p.eligibility.category || p.eligibility.status}
              </span>
              <div className="cg-inline-links">
                {sourceLink(p.programmeUrl || p.sourceUrl, "Programme source")}
                {sourceLink(p.applicationUrl, "Application details")}
                <button
                  className="cg-text-button"
                  onClick={() => setSelected(selected === p.id ? "" : p.id)}
                  aria-expanded={selected === p.id}
                >
                  Check Admission Eligibility
                </button>
              </div>
              {selected === p.id && (
                <div className="cg-research-eligibility">
                  <h4>Checked academic requirements</h4>
                  <p>Admission is never guaranteed.</p>
                  {p.publishedEvidence && <dl>
                    <dt>Published intake</dt><dd>{p.publishedEvidence.intakeYear || "Information not yet verified."}</dd>
                    <dt>Applications open</dt><dd>{p.publishedEvidence.applicationOpening || "Information not yet verified."}</dd>
                    <dt>Applications close</dt><dd>{p.publishedEvidence.applicationClosing || "Information not yet verified."}</dd>
                    <dt>Published tuition fees</dt><dd>{p.publishedEvidence.tuitionFees || "Information not yet verified."}</dd>
                    <dt>Published subject requirements</dt><dd>{Object.entries(p.publishedEvidence.subjectRequirements).map(([subject, threshold]) => `${subject}: ${threshold}`).join("; ") || "Information not yet verified."}</dd>
                    <dt>Programme evidence</dt><dd>{sourceLink(p.publishedEvidence.evidenceUrl, "Official published evidence")}</dd>
                    <dt>Official funding information</dt><dd>{Object.entries(p.publishedEvidence.fundingLinks).map(([url,label]) => <span key={url}>{sourceLink(url,label)}{" "}</span>)}</dd>
                  </dl>}
                  {p.eligibility.subjects.length > 0 && (
                    <div className="cg-table-scroll">
                      <table>
                        <thead>
                          <tr>
                            <th>Subject</th>
                            <th>Your result</th>
                            <th>Published requirement</th>
                            <th>Checked status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {p.eligibility.subjects.map((s, i) => (
                            <tr key={i}>
                              <th scope="row">{s.subject}</th>
                              <td>
                                {s.studentResult}
                                {!s.reviewed && " · not reviewed"}
                              </td>
                              <td>{s.requirement}</td>
                              <td>{s.status}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                  {p.eligibility.requiredAps != null && (
                    <p>
                      Institution APS:{" "}
                      {p.eligibility.studentAps ?? "Cannot calculate"} ·
                      Required: {p.eligibility.requiredAps} · Rule:{" "}
                      {p.eligibility.apsRule || "Not verified"}
                    </p>
                  )}
                  {p.requirements && (
                    <p className="cg-preserve-lines">{p.requirements}</p>
                  )}
                  <ul>
                    {[
                      ...p.eligibility.gaps,
                      ...p.eligibility.uncheckedConditions,
                    ].map((text, i) => (
                      <li key={i}>{text}</li>
                    ))}
                  </ul>
                  <p className="cg-note">
                    This assessment checks available published conditions only.
                    Selection, qualification completion, equivalence and
                    available places may require further verification.
                  </p>
                </div>
              )}
            </div>
          </article>
        ))}
      </div>
      {count < rows.length && (
        <button className="cg-button" onClick={() => setCount(count + 12)}>
          Load more programmes ({rows.length - count} remaining)
        </button>
      )}
    </section>
  );
}

export function CareerResearchReportView({
  report,
}: {
  report: CareerResearchReport;
}) {
  const [comparison, setComparison] = useState<string | null>(null);
  const comparable = report.careers.map((c) => ({
    key: c.id,
    id: c.id,
    title: c.title,
    category: c.category,
    description: c.description,
    reasons: [c.explanation],
    improvements: c.improvementAreas,
    ai: true,
  }));
  const [category, setCategory] = useState("");
  const [search, setSearch] = useState("");
  const [country, setCountry] = useState("");
  const [qualification, setQualification] = useState("");
  const [eligibility, setEligibility] = useState("");
  const [count, setCount] = useState(12);
  const [career, setCareer] = useState("");
  const eligibleProgrammeIds = new Set(
    report.programmes
      .filter(
        (p) =>
          (!country || p.country === country) &&
          (!qualification || p.qualification === qualification) &&
          (!eligibility || p.eligibility.status === eligibility),
      )
      .map((p) => p.id),
  );
  const careers = report.careers.filter(
    (c) =>
      (!category || c.category === category) &&
      `${c.title} ${c.explanation} ${c.relevantSubjects.join(" ")}`
        .toLowerCase()
        .includes(search.toLowerCase()) &&
      (!(country || qualification || eligibility) ||
        c.programmeIds.some((id) => eligibleProgrammeIds.has(id))),
  );
  const categories = report.careers.reduce<Record<string, number>>(
    (counts, c) => ({ ...counts, [c.category]: (counts[c.category] ?? 0) + 1 }),
    {},
  );
  const funding = report.funding.filter(
    (f) => !f.deadline || f.deadline >= new Date().toISOString().slice(0, 10),
  );
  return (
    <div className="cg-generated" aria-label="Generated AI guidance">
      <UniversityCoveragePanel />
      <section className="cg-panel ed-panel">
        <h2>Student Profile Summary</h2>
        <p>
          Research snapshot: {new Date(report.generatedAt).toLocaleString()}.{" "}
          {report.academic.grade || "Academic level not recorded"}
          {report.academic.percentageAverage != null
            ? ` · ${report.academic.percentageAverage.toFixed(1)}% average across comparable percentage results`
            : ""}
        </p>
        <p>
          Curriculum: {report.academic.curricula.join(", ") || "Not recorded"}
        </p>
        <p>
          Interests: {report.academic.interests.join(", ") || "Not recorded"} ·
          Skills: {report.academic.skills.join(", ") || "Not recorded"}
        </p>
        {report.academic.goals && <p>Career goals: {report.academic.goals}</p>}
        <div className="cg-research-metrics">
          <span>
            <strong>{report.careers.length}</strong> supported careers
          </span>
          <span>
            <strong>{report.programmes.length}</strong> programme matches
          </span>
          <span>
            <strong>
              {
                new Set(
                  report.programmes
                    .filter(
                      (p) =>
                        p.verificationStatus.startsWith("Verified") ||
                        p.verificationStatus.startsWith(
                          "Programme title verified",
                        ),
                    )
                    .map((p) => p.institution),
                ).size
              }
            </strong>{" "}
            institutions with verified records
          </span>
        </div>
        <p>
          Career categories:{" "}
          {Object.entries(categories)
            .sort((a, b) => b[1] - a[1])
            .map(([name, n]) => `${name} (${n})`)
            .join(" · ")}
        </p>
      </section>
      <section className="cg-panel ed-panel">
        <h2>AI Career Assessment</h2>
        <p className="cg-preserve-lines">{report.interpretation.assessment}</p>
        <p className="cg-note">
          AI interpretation of supplied evidence. Programme facts and
          eligibility checks are listed separately below.
        </p>
      </section>
      <section id="career-recommendations">
        <div className="cg-section-heading">
          <div>
            <h2>Your AI Career Recommendations</h2>
            <p>
              All {report.careers.length} supported catalogue results within
              your plan. Internal priority weights are not probabilities or
              admission decisions.
            </p>
          </div>
        </div>
        <div className="cg-research-filters">
          <label>
            Search Careers
            <input
              type="search"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setCount(12);
              }}
            />
          </label>
          <label>
            Career Category
            <select
              value={category}
              onChange={(e) => {
                setCategory(e.target.value);
                setCount(12);
              }}
            >
              <option value="">All Careers</option>
              {unique(report.careers.map((c) => c.category)).map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </label>
          {[
            {
              label: "Country",
              value: country,
              change: setCountry,
              values: unique(report.programmes.map((p) => p.country)),
            },
            {
              label: "Qualification",
              value: qualification,
              change: setQualification,
              values: unique(report.programmes.map((p) => p.qualification)),
            },
            {
              label: "Admission Eligibility",
              value: eligibility,
              change: setEligibility,
              values: unique(
                report.programmes.map((p) => p.eligibility.status),
              ),
            },
          ].map((s) => (
            <label key={s.label}>
              {s.label}
              <select
                value={s.value}
                onChange={(e) => {
                  s.change(e.target.value);
                  setCount(12);
                }}
              >
                <option value="">All</option>
                {s.values.map((v) => (
                  <option key={v}>{v}</option>
                ))}
              </select>
            </label>
          ))}
        </div>
        <div className="cg-career-grid">
          {careers.slice(0, count).map((c) => (
            <article className="cg-panel cg-career-card ed-panel" key={c.id}>
              <span className="cg-category">{c.category}</span>
              <h3>{c.title}</h3>
              <p>{c.description}</p>
              <span className="cg-pill">{c.suitability}</span>
              <h4>Why EduRite recommends this career</h4>
              <p>{c.explanation}</p>
              {c.relevantSubjects.length > 0 && (
                <p>
                  <strong>Relevant subjects:</strong>{" "}
                  {c.relevantSubjects.join(", ")}
                </p>
              )}
              {c.strengths.length > 0 && (
                <p>
                  <strong>Academic strengths:</strong> {c.strengths.join("; ")}
                </p>
              )}
              {c.improvementAreas.length > 0 && (
                <p>
                  <strong>Programme gaps to address:</strong>{" "}
                  {c.improvementAreas.join("; ")}
                </p>
              )}
              {c.qualifications.length > 0 && (
                <p>
                  <strong>Catalogue qualification guidance:</strong>{" "}
                  {c.qualifications.join(", ")}. Confirm programme requirements.
                </p>
              )}
              <p>
                {c.programmeIds.length} programme records ·{" "}
                {
                  new Set(
                    report.programmes
                      .filter((p) => p.careerIds.includes(c.id))
                      .map((p) => p.institution),
                  ).size
                }{" "}
                institutions
              </p>
              <p className="cg-muted">
                Priority weight {c.rankingWeight} · internal ordering, not a
                match percentage.
              </p>
              <div className="cg-career-actions">
                <CareerSaveButton id={c.id} title={c.title} />
                {report.careers.length > 1 && (
                  <button
                    className="cg-text-button"
                    onClick={() => setComparison(c.id)}
                  >
                    Compare Similar Careers
                  </button>
                )}
                <Link className="cg-button" to={careerPathLink(c.title)}>
                  View Career Pathway
                </Link>
                <a
                  className="cg-text-link"
                  href="#research-programmes"
                  onClick={() => setCareer(c.id)}
                >
                  Explore Universities / Check Admission Eligibility
                </a>
                <Link
                  className="cg-text-link"
                  to={`/student/study-options?section=research&career=${encodeURIComponent(c.id)}`}
                >
                  View Study Options
                </Link>
              </div>
            </article>
          ))}
        </div>
        {!careers.length && (
          <p className="cg-empty">
            No careers match these filters. Change the category, country,
            qualification or search.
          </p>
        )}
        {count < careers.length && (
          <button className="cg-button" onClick={() => setCount(count + 12)}>
            Load more careers ({careers.length - count} remaining)
          </button>
        )}
        <details className="cg-panel ed-panel">
          <summary>How career priorities are calculated</summary>
          <p>{report.rankingMethod}</p>
        </details>
      </section>
      {comparison && comparable.find((c) => c.key === comparison) && (
        <CareerComparison
          careers={comparable}
          initialCareer={comparable.find((c) => c.key === comparison)!}
          onClose={() => setComparison(null)}
        />
      )}
      {career && (
        <div className="cg-inline-links">
          <p>
            Programme filter:{" "}
            {report.careers.find((c) => c.id === career)?.title}
          </p>
          <button className="cg-text-button" onClick={() => setCareer("")}>
            Show programmes for all careers
          </button>
        </div>
      )}
      <div id="research-programmes">
        <ResearchProgrammeCollection
          key={career}
          programmes={report.programmes}
          initialCareer={career}
        />
      </div>
      {report.academic.subjects.length > 0 && (
        <section className="cg-panel ed-panel">
          <h2>Your Subject Analysis</h2>
          <div className="cg-two-columns">
            {report.academic.subjects.map((s, i) => (
              <div key={i}>
                <div className="cg-section-heading">
                  <strong>{s.subjectName}</strong>
                  <span>{subjectResult(s)}</span>
                </div>
                {s.markPercentage != null &&
                  s.markPercentage >= 0 &&
                  s.markPercentage <= 100 && (
                    <progress
                      aria-label={`${s.subjectName} recorded percentage`}
                      value={s.markPercentage}
                      max={100}
                    />
                  )}
                <p className="cg-muted">
                  {[s.curriculum, s.academicYear, s.examination]
                    .filter(Boolean)
                    .join(" ? ")}
                  {s.verified
                    ? " ? Reviewed result"
                    : " ? Awaiting review / self-reported"}
                </p>
              </div>
            ))}
          </div>
        </section>
      )}
      <section className="cg-panel ed-panel">
        <h2>My Academic Strengths and Areas for Improvement</h2>
        {Boolean(report.academic.progression?.length) && (
          <div>
            <h3>Recorded Academic Progression</h3>
            <ul>
              {report.academic.progression?.map((p, i) => (
                <li key={i}>
                  {p.subject} ({p.curriculum}): {p.previousYear}{" "}
                  {p.previousMark}% ? {p.currentYear} {p.currentMark}% (
                  {p.percentagePointChange > 0 ? "+" : ""}
                  {p.percentagePointChange} percentage points)
                </li>
              ))}
            </ul>
          </div>
        )}
        <div className="cg-two-columns">
          <div>
            <h3>Relative Academic Strengths</h3>
            {report.academic.relativeStrengths.length ? (
              <ul>
                {report.academic.relativeStrengths.map((s, i) => (
                  <li key={i}>
                    <strong>
                      {s.subjectName}: {subjectResult(s)}
                    </strong>
                    <p>
                      Related careers:{" "}
                      {report.careers
                        .filter((c) =>
                          c.relevantSubjects.some((t) =>
                            t.startsWith(s.subjectName),
                          ),
                        )
                        .map((c) => c.title)
                        .join(", ") || "No catalogue correlation verified yet"}
                    </p>
                  </li>
                ))}
              </ul>
            ) : (
              <p>
                No comparable percentage results are available for a strength
                ranking.
              </p>
            )}
          </div>
          <div>
            <h3>Areas for Improvement</h3>
            <p>
              These are relative study priorities, not limits on your career
              potential.
            </p>
            <ul>
              {report.academic.improvementPriorities.map((s, i) => (
                <li key={i}>
                  <strong>
                    {s.subjectName}: {subjectResult(s)}
                  </strong>
                  <p>
                    Prioritise practice in this recorded subject and review the
                    specific programme gaps below.
                  </p>
                  <Link
                    className="cg-text-link"
                    to="/student/learning?section=tutor"
                  >
                    Practise with the EduRite AI Tutor
                  </Link>
                </li>
              ))}
            </ul>
            {[
              ...new Set(report.programmes.flatMap((p) => p.eligibility.gaps)),
            ].map((gap) => (
              <p key={gap}>{gap}</p>
            ))}
          </div>
        </div>
        {report.academic.limitations.map((l) => (
          <p className="cg-note" key={l}>
            {l}
          </p>
        ))}
      </section>
      <section className="cg-panel ed-panel">
        <h2>Alternative Study Pathways</h2>
        {report.alternativePathways.length ? (
          <ul>
            {report.alternativePathways.map((p) => (
              <li key={p.id}>
                <strong>{p.name}</strong> · {p.institution} · {p.qualification}{" "}
                {sourceLink(p.sourceUrl)}
              </li>
            ))}
          </ul>
        ) : (
          <p>
            No specific diploma, certificate, bridging or vocational alternative
            was verified within this report.{" "}
            <Link
              className="cg-text-link"
              to="/student/study-options?section=colleges-tvets"
            >
              Explore vocational institutions
            </Link>
          </p>
        )}
      </section>
      <section className="cg-panel ed-panel">
        <h2>Bursaries and Funding</h2>
        {funding.length ? (
          <div className="cg-universities">
            {funding.map((f) => (
              <article className="cg-university" key={f.externalId}>
                <div>
                  <h3>{f.title}</h3>
                  <p>{f.provider}</p>
                  <p>{f.eligibility || f.description}</p>
                  <p>
                    {f.deadline
                      ? `Published deadline: ${f.deadline}`
                      : "Deadline and open status not verified"}
                  </p>
                  <p className="cg-muted">{f.dataFreshnessNote}</p>
                  {sourceLink(f.applicationLink, "Funding source")}
                  <Link
                    className="cg-text-link"
                    to={`/student/funding?section=bursaries&q=${encodeURIComponent(f.title)}`}
                  >
                    View in Bursaries & Funding
                  </Link>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <p>
            No current relevant funding opportunity was verified from available
            records. Expired dated opportunities are excluded.
          </p>
        )}
      </section>
      <section className="cg-panel ed-panel">
        <h2>Recommended Next Steps</h2>
        <ol>
          {report.interpretation.nextSteps.map((text, i) => (
            <li key={i}>{text}</li>
          ))}
        </ol>
        <div className="cg-inline-links">
          <Link className="cg-text-link" to="/student/profile?section=academic">
            Review Academic Profile
          </Link>
          <Link
            className="cg-text-link"
            to="/student/career-match?step=assessment"
          >
            Assessment and saved results
          </Link>
          <Link className="cg-text-link" to="/student/learning?section=tutor">
            Open AI Tutor
          </Link>
        </div>
      </section>
      <section className="cg-panel ed-panel">
        <h2>Your AI Career Research Summary</h2>
        <p className="cg-preserve-lines">
          {report.interpretation.researchSummary}
        </p>
        <details>
          <summary>Profile factors and research limitations</summary>
          <ul>
            {[...report.inputsUsed, ...report.limitations].map((text, i) => (
              <li key={i}>{text}</li>
            ))}
          </ul>
        </details>
        <h3>Source List</h3>
        {report.sources.length ? (
          <ul>
            {report.sources.map((s, i) => (
              <li key={i}>
                {sourceLink(s.url, s.institution)}
                <p>
                  {s.status}
                  {s.verifiedAt
                    ? ` · verified ${new Date(s.verifiedAt).toLocaleDateString()}`
                    : ""}
                </p>
              </li>
            ))}
          </ul>
        ) : (
          <p>
            No programme source was verified in this research run. AI
            interpretations must not be treated as verified university facts.
          </p>
        )}
      </section>
    </div>
  );
}
