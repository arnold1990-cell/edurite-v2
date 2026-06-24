export const EmptyState = ({ title, message }: { title: string; message: string }) => (
  <div className="card p-8 text-center">
    <h3 className="text-lg font-semibold">{title}</h3>
    <p className="mt-2 text-sm text-slate-500">{message}</p>
  </div>
);

export const LoadingState = ({ message = 'Loading data...', detail }: { message?: string; detail?: string }) => (
  <div className="card flex flex-col items-center justify-center gap-4 p-8 text-center" role="status" aria-live="polite">
    <div className="h-12 w-12 animate-spin rounded-full border-4 border-slate-200 border-t-primary-600" aria-hidden="true" />
    <div className="space-y-1">
      <p className="text-sm font-medium text-slate-700">{message}</p>
      {detail ? <p className="text-xs text-slate-500">{detail}</p> : null}
    </div>
  </div>
);
export const ErrorState = ({ message }: { message: string }) => <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">{message}</div>;
