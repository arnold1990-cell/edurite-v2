import { useEffect, useRef, useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { DataTable } from '@/components/tables/DataTable';
import { getErrorMessage } from '@/lib/errors';
import { adminSubscriptionService as service, type ManagedPlan, type ManagedSubscription } from '@/services/adminSubscriptionService';

function Modal({ title, children, onClose, busy = false }: { title: string; children: ReactNode; onClose: () => void; busy?: boolean }) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => { dialog.current?.showModal(); }, []);
  return <dialog ref={dialog} aria-label={title} onCancel={event => { event.preventDefault(); if (!busy) onClose(); }} className="w-full max-w-2xl rounded-2xl border border-slate-200 p-6 shadow-xl backdrop:bg-slate-900/50">
    <h2 className="mb-4 text-xl font-bold">{title}</h2>{children}
    <Button type="button" disabled={busy} onClick={onClose} className="mt-4">Close</Button>
  </dialog>;
}

export function ManageSubscriptionsPage() {
  const client = useQueryClient();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<ManagedSubscription | null>(null);
  const [mode, setMode] = useState<'change' | 'detail' | 'history' | null>(null);
  const [plan, setPlan] = useState<ManagedPlan>('BASIC');
  const [reason, setReason] = useState('');
  const [notice, setNotice] = useState('');
  const [historyPage, setHistoryPage] = useState(0);
  const users = useQuery({ queryKey: ['admin', 'subscriptions', search, page], queryFn: () => service.list(search, page) });
  const detail = useQuery({ queryKey: ['admin', 'subscription', selected?.userId], enabled: mode === 'detail' && Boolean(selected), queryFn: () => service.detail(selected!.userId) });
  const history = useQuery({ queryKey: ['admin', 'subscription-history', selected?.userId, historyPage], enabled: mode === 'history' && Boolean(selected), queryFn: () => service.history(selected!.userId, historyPage) });
  const change = useMutation({
    mutationFn: () => service.change(selected!.userId, plan, reason.trim()),
    onSuccess: async () => {
      setMode(null); setNotice('Subscription updated successfully.');
      await Promise.all([
        client.invalidateQueries({ queryKey: ['admin'] }),
        client.invalidateQueries({ queryKey: ['subscription-access'] }),
        client.invalidateQueries({ queryKey: ['subscription'] }),
      ]);
    },
  });
  const open = (user: ManagedSubscription, action: typeof mode) => {
    setSelected(user); setPlan(user.currentPlan); setReason(''); setMode(action); setHistoryPage(0); change.reset(); setNotice('');
  };
  return <section className="space-y-6">
    <header><h1 className="text-2xl font-bold">Manage User Subscriptions</h1><p className="text-sm text-slate-600">Assign platform access independently of billing. Changes are recorded in subscription history.</p></header>
    {notice && <p role="status" className="rounded-lg bg-emerald-50 p-3 text-emerald-800">{notice}</p>}
    <label className="block">Search users by name, username or email<Input value={search} onChange={event => { setSearch(event.target.value); setPage(0); }} /></label>
    {users.isPending ? <p role="status">Loading subscriptions...</p> : users.isError ? <p role="alert">{getErrorMessage(users.error)} <Button onClick={() => void users.refetch()}>Retry</Button></p> : <>
      {users.data.content.length === 0 && <p>No matching users.</p>}
      <DataTable data={users.data.content.map(user => ({ ...user, id: user.userId }))} columns={[
        { key: 'fullName', header: 'User Name' }, { key: 'email', header: 'Email' },
        { key: 'currentPlan', header: 'Current Plan' }, { key: 'source', header: 'Subscription Source' },
        { key: 'status', header: 'Subscription Status' }, { key: 'id', header: 'Actions', render: user => <div className="flex flex-wrap gap-2">
          <Button onClick={() => open(user, 'change')}>Change Plan</Button><Button onClick={() => open(user, 'detail')}>View Subscription</Button><Button onClick={() => open(user, 'history')}>View History</Button>
        </div> },
      ]} />
      <div className="flex items-center gap-3"><Button disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button><span>{users.data.totalElements} users · Page {page + 1}</span><Button disabled={page + 1 >= users.data.totalPages} onClick={() => setPage(page + 1)}>Next</Button></div>
    </>}
    {mode && selected && <Modal title={mode === 'change' ? 'Change Plan' : mode === 'history' ? 'Subscription History' : 'View Subscription'} onClose={() => setMode(null)} busy={change.isPending}>
      <p className="mb-3">{selected.fullName} · {selected.email}</p>
      {mode === 'change' && <form className="space-y-4" onSubmit={event => { event.preventDefault(); if (reason.trim() && !change.isPending) change.mutate(); }}>
        <label className="block">Selected User<Input readOnly value={selected.email} /></label>
        <p>Current Plan: <strong>{selected.currentPlan}</strong></p>
        <label className="block">New Plan<select className="ml-3 rounded-lg border border-slate-300 p-2" value={plan} onChange={event => setPlan(event.target.value as ManagedPlan)} disabled={change.isPending}>{(['BASIC', 'PREMIUM', 'PRO'] as const).map(value => <option key={value}>{value}</option>)}</select></label>
        <label className="block">Reason for Change<textarea className="mt-2 w-full rounded-lg border border-slate-300 p-3" value={reason} onChange={event => setReason(event.target.value)} required maxLength={1000} disabled={change.isPending} /></label>
        <p className="text-sm text-slate-600">This changes application access until another administrator changes it. Existing billing agreements continue separately.</p>
        {change.isError && <p role="alert">{getErrorMessage(change.error)}</p>}
        <Button type="submit" disabled={!reason.trim() || change.isPending}>{change.isPending ? 'Saving...' : 'Confirm Change'}</Button>
      </form>}
      {mode === 'detail' && (detail.isPending ? <p role="status">Loading subscription...</p> : detail.isError ? <p role="alert">{getErrorMessage(detail.error)} <Button onClick={() => void detail.refetch()}>Retry</Button></p> : <dl className="space-y-2"><dt>Current Plan</dt><dd>{detail.data.currentPlan}</dd><dt>Subscription Source</dt><dd>{detail.data.source}</dd><dt>Status</dt><dd>{detail.data.status}</dd><dt>Start Date</dt><dd>{detail.data.subscription?.startDate ?? 'Not applicable'}</dd><dt>End Date</dt><dd>{detail.data.subscription?.endDate ?? 'No scheduled expiry'}</dd></dl>)}
      {mode === 'history' && (history.isPending ? <p role="status">Loading history...</p> : history.isError ? <p role="alert">{getErrorMessage(history.error)} <Button onClick={() => void history.refetch()}>Retry</Button></p> : <>
        {history.data.content.length === 0 ? <p>No administrator subscription changes recorded.</p> : <ol className="space-y-4">{history.data.content.map(item => <li key={item.id} className="rounded-lg border p-3"><p>{item.previousPlan} → {item.newPlan}</p><p>{item.reason}</p><p className="text-sm text-slate-600">{item.administratorEmail} · {new Date(item.createdAt).toLocaleString()}</p></li>)}</ol>}
        <div className="mt-3 flex gap-3"><Button disabled={historyPage === 0} onClick={() => setHistoryPage(historyPage - 1)}>Previous history</Button><Button disabled={historyPage + 1 >= history.data.totalPages} onClick={() => setHistoryPage(historyPage + 1)}>Next history</Button></div>
      </>)}
    </Modal>}
  </section>;
}
