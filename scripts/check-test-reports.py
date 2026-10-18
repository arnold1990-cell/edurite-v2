"""Fail CI on missing reports, failures, or unexpected skips (including DB suites)."""
import pathlib
import sys
import xml.etree.ElementTree as ET

OPTIONAL = {"com.edurite.ai.service.TutorProviderLiveIntegrationTest"}
REQUIRED = {
    "TrialMigrationTest", "ComplimentaryProGrantIntegrationTest",
    "RoadmapMigrationIntegrationTest", "StudyPlanPersistenceIntegrationTest",
    "AcademicProfilePersistenceIntegrationTest", "DevProfileDatasourceIntegrationTest",
    "AiUsageConcurrencyTest", "EmailRecoveryPostgresTest",
}


def check(directory):
    reports = list(pathlib.Path(directory).glob("TEST-*.xml"))
    seen, errors = set(), []
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    for report in reports:
        root = ET.parse(report).getroot()
        name = root.attrib["name"]
        seen.add(name.rsplit(".", 1)[-1])
        counts = {key: int(root.get(key, "0")) for key in totals}
        for key, count in counts.items():
            totals[key] += count
        if counts["skipped"]:
            print(f"SKIPPED {name}: {counts['skipped']}" + (" (optional live provider)" if name in OPTIONAL else ""))
        if counts["failures"] or counts["errors"] or (counts["skipped"] and name not in OPTIONAL):
            errors.append(name)
        if name.rsplit(".", 1)[-1] in REQUIRED and not counts["tests"]:
            errors.append(f"Empty required suite: {name}")
    errors.extend(f"Missing required suite: {name}" for name in sorted(REQUIRED - seen))
    print(totals)
    if not reports or errors:
        raise SystemExit("Required verification failed: " + ", ".join(errors))


if __name__ == "__main__":
    check(sys.argv[1] if len(sys.argv) > 1 else "backend/target/surefire-reports")
