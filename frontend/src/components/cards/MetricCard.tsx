export const MetricCard = ({ title, value, subtitle }: { title: string; value: string | number; subtitle?: string }) => (
  <article className="card p-4">
    <p className="text-sm text-slate-500">{title}</p>
    <h3 className="mt-1 text-2xl font-bold text-slate-900">{value}</h3>
    {subtitle ? <p className="mt-2 text-xs text-slate-500">{subtitle}</p> : null}
  </article>
);
