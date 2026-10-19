import { useState } from "react";
import type { GuidanceCareer } from "./careerGuidanceModel";

export function CareerComparison({
  careers,
  initialCareer,
  onClose,
}: {
  careers: GuidanceCareer[];
  initialCareer: GuidanceCareer;
  onClose: () => void;
}) {
  const [firstKey, setFirstKey] = useState(initialCareer.key);
  const [secondKey, setSecondKey] = useState(
    careers.find(
      (item) =>
        item.key !== initialCareer.key &&
        item.category === initialCareer.category,
    )?.key ??
      careers.find((item) => item.key !== initialCareer.key)?.key ??
      "",
  );
  const first = careers.find((item) => item.key === firstKey);
  const second = careers.find((item) => item.key === secondKey);
  return (
    <section
      className="cg-panel cg-comparison"
      aria-label="Compare recommended careers"
    >
      <div className="cg-section-heading">
        <div>
          <h3>Compare Similar Careers</h3>
          <p>
            Compare the descriptions and evidence behind your actual
            recommendations.
          </p>
        </div>
        <button className="cg-text-button" onClick={onClose}>
          Close comparison
        </button>
      </div>
      <div className="cg-two-columns">
        {[
          { value: firstKey, change: setFirstKey, label: "First career" },
          { value: secondKey, change: setSecondKey, label: "Second career" },
        ].map(({ value, change, label }) => (
          <label className="cg-programme-select" key={label}>
            {label}
            <select
              value={value}
              onChange={(event) => change(event.target.value)}
            >
              <option value="">Choose a recommended career</option>
              {careers.map((item) => (
                <option key={item.key} value={item.key}>
                  {item.title}
                </option>
              ))}
            </select>
          </label>
        ))}
      </div>
      <div className="cg-table-scroll">
        <table>
          <caption className="sr-only">
            Comparison of recommendation evidence
          </caption>
          <thead>
            <tr>
              <th>Career information</th>
              <th>{first?.title || "Choose first career"}</th>
              <th>{second?.title || "Choose second career"}</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <th scope="row">Category</th>
              <td>{first?.category || "Not selected"}</td>
              <td>{second?.category || "Not selected"}</td>
            </tr>
            <tr>
              <th scope="row">Description</th>
              <td>
                {first?.description || "No catalogue description available"}
              </td>
              <td>
                {second?.description || "No catalogue description available"}
              </td>
            </tr>
            <tr>
              <th scope="row">Personalised evidence</th>
              <td>{first?.reasons.join(" ") || "Not available"}</td>
              <td>{second?.reasons.join(" ") || "Not available"}</td>
            </tr>
            <tr>
              <th scope="row">Recommended improvements</th>
              <td>
                {first?.improvements.join(" ") ||
                  "Review programme requirements"}
              </td>
              <td>
                {second?.improvements.join(" ") ||
                  "Review programme requirements"}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  );
}
