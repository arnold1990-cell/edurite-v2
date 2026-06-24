export const PlaceholderChart = ({ title }: { title: string }) => (
  <div className="card min-w-0 p-4">
    <h3 className="truncate text-sm font-semibold sm:text-base">{title}</h3>
    <div className="mt-3 h-36 w-full rounded-lg bg-gradient-to-r from-blue-100 to-emerald-100 sm:h-40 lg:h-48" />
  </div>
);
