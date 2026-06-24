interface Column<T> {
  key: keyof T;
  header: string;
  render?: (item: T) => React.ReactNode;
  hideOnMobile?: boolean;
}

export const DataTable = <T extends { id: string }>({ columns, data }: { columns: Column<T>[]; data?: T[] }) => (
  <div className="card min-w-0">
    <div className="grid gap-3 p-3 md:hidden">
      {(data ?? []).map((row) => (
        <article key={row.id} className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
          <dl className="space-y-2">
            {columns.filter((col) => !col.hideOnMobile).map((col) => (
              <div key={String(col.key)} className="grid grid-cols-[110px_1fr] items-start gap-2">
                <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">{col.header}</dt>
                <dd className="min-w-0 break-words text-sm text-slate-700">{col.render ? col.render(row) : String(row[col.key] ?? '')}</dd>
              </div>
            ))}
          </dl>
        </article>
      ))}
    </div>
    <div className="hidden overflow-x-auto md:block">
      <table className="min-w-full text-sm">
        <thead className="bg-slate-100">
          <tr>{columns.map((col) => <th key={String(col.key)} className="px-4 py-3 text-left font-semibold text-slate-600">{col.header}</th>)}</tr>
        </thead>
        <tbody>
          {(data ?? []).map((row) => (
            <tr key={row.id} className="border-t border-slate-100">
              {columns.map((col) => <td key={String(col.key)} className="px-4 py-3">{col.render ? col.render(row) : String(row[col.key] ?? '')}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </div>
);
