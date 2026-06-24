export const PageShell = ({ title, description }: { title: string; description: string }) => (
  <section className="space-y-4">
    <div>
      <h1 className="text-2xl font-bold">{title}</h1>
      <p className="text-sm text-slate-600">{description}</p>
    </div>
    <div className="card p-6 text-sm text-slate-600">This section is ready for feature-specific components and live API data.</div>
  </section>
);
