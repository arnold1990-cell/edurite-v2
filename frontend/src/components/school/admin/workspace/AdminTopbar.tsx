import { Menu, Search } from 'lucide-react';
import { Input } from '@/components/ui/Input';

export const AdminTopbar = ({
  fullName,
  search,
  setSearch,
  onOpenMenu,
}: {
  fullName?: string;
  search: string;
  setSearch: (value: string) => void;
  onOpenMenu: () => void;
}) => (
  <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
    <div className="flex flex-wrap items-center gap-3">
      <button
        type="button"
        onClick={onOpenMenu}
        aria-label="Open admin menu"
        className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 text-slate-600 lg:hidden"
      >
        <Menu className="h-4 w-4" />
      </button>
      <div className="relative min-w-[220px] flex-1">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search teachers, learners, classes, subjects..." className="h-10 rounded-xl bg-slate-50 pl-9 text-sm" />
      </div>
      <div className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2">
        <span className="h-8 w-8 rounded-full bg-gradient-to-br from-primary-600 to-indigo-600" />
        <div className="hidden text-left sm:block">
          <p className="text-xs font-semibold text-slate-800">{fullName || 'School Admin'}</p>
          <p className="text-[11px] text-slate-500">School Admin</p>
        </div>
      </div>
    </div>
  </div>
);
