import { useState } from "react";
import { apiClient } from "@/services/apiClient";

interface Coverage {
  publicInstitutions: number;
  directoryStatus: string;
  limitation: string;
  institutions: {
    institution: { universityName: string; institutionType: string; province?: string; registrationStatus: string; lastVerificationDate?: string };
    indexedProgrammes: number;
    retrievedPages: number;
    queuedPages: number;
    status: string;
    unavailableSources: { url: string; status: string; lastChecked?: string; reason?: string }[];
  }[];
}

export function UniversityCoveragePanel() {
  const [coverage, setCoverage] = useState<Coverage>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const load = async () => {
    if (loading) return;
    setLoading(true); setError("");
    try { setCoverage((await apiClient.get<Coverage>("/ai/university-intelligence/coverage")).data); }
    catch { setError("Information not yet verified. University coverage could not be retrieved."); }
    finally { setLoading(false); }
  };
  return <section className="cg-panel ed-panel" aria-label="University research coverage">
    <h2>South African university research coverage</h2>
    <p>A university appearing in the registry does not mean all its programmes have been verified.</p>
    <button className="cg-button" onClick={() => void load()} disabled={loading}>
      {loading ? "Loading coverage…" : coverage ? "Refresh coverage" : "View university coverage"}
    </button>
    {error && <p role="status">{error}</p>}
    {coverage && <>
      <p>{coverage.publicInstitutions} public universities in the registry. {coverage.directoryStatus}</p>
      <p>{coverage.limitation}</p>
      <div className="cg-table-scroll"><table><thead><tr><th>Institution</th><th>Category / province</th><th>Registration evidence</th><th>Indexed programmes</th><th>Source coverage</th></tr></thead>
        <tbody>{coverage.institutions.map(row => <tr key={row.institution.universityName}>
          <th scope="row">{row.institution.universityName}</th>
          <td>{row.institution.institutionType.replace(/_/g, " ")}<br />{row.institution.province || "Information not yet verified."}</td>
          <td>{row.institution.registrationStatus}<br />{row.institution.lastVerificationDate || "Information not yet verified."}</td>
          <td>{row.indexedProgrammes}</td>
          <td>{row.retrievedPages} retrieved; {row.queuedPages} queued. {row.status}
            {row.unavailableSources.length > 0 && <details><summary>{row.unavailableSources.length} unavailable sources</summary><ul>
              {row.unavailableSources.map(source => <li key={source.url}>{source.url}: {source.status}. {source.reason} {source.lastChecked && `Checked ${new Date(source.lastChecked).toLocaleDateString()}`}</li>)}
            </ul></details>}
          </td>
        </tr>)}</tbody></table></div>
    </>}
  </section>;
}
