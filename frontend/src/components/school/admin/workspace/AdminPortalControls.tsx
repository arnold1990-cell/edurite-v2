export const AdminPortalControls = () => (
  <div className="grid gap-4 xl:grid-cols-2">
    <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="text-sm font-semibold text-slate-900">Teacher Portal Controls</h2>
      <ul className="mt-3 space-y-2 text-sm text-slate-700">
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">View teachers</li>
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">Add / edit / deactivate teachers</li>
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">Assign teachers to subjects and classes</li>
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">View teacher workload and submissions</li>
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">Manage teacher dashboard access</li>
      </ul>
    </article>
    <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="text-sm font-semibold text-slate-900">Learner Portal Controls</h2>
      <ul className="mt-3 space-y-2 text-sm text-slate-700">
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">View learners</li>
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">Add / edit / deactivate learners</li>
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">Assign learners to classes and subjects</li>
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">View learner progress, submissions and results</li>
        <li className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">Manage learner dashboard access</li>
      </ul>
    </article>
  </div>
);
