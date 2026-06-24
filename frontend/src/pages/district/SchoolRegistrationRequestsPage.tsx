import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { ErrorState, LoadingState } from '@/components/feedback/States';
import { useAppQuery } from '@/hooks/useAppQuery';
import { districtService, type SchoolRegistrationRequestItem, type SchoolRegistrationRequestStatusFilter } from '@/services/districtService';

const statusTabs: Array<{ key: SchoolRegistrationRequestStatusFilter; label: string; helper: string }> = [
  { key: 'PENDING', label: 'Pending Requests', helper: 'Awaiting district approval and school activation.' },
  { key: 'APPROVED', label: 'Approved Schools', helper: 'District-approved registrations now active in the school portal.' },
  { key: 'REJECTED', label: 'Rejected Requests', helper: 'Requests returned to schools with a rejection reason.' },
];

const statusLabel = (status: SchoolRegistrationRequestItem['status']) => {
  if (status === 'ACTIVE') return 'Approved';
  if (status === 'PENDING_DISTRICT_APPROVAL') return 'Pending';
  if (status === 'REJECTED') return 'Rejected';
  if (status === 'SUSPENDED') return 'Suspended';
  return status;
};

const statusBadgeClass = (status: SchoolRegistrationRequestItem['status']) => {
  if (status === 'ACTIVE') return 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200';
  if (status === 'REJECTED') return 'bg-rose-50 text-rose-700 ring-1 ring-rose-200';
  if (status === 'SUSPENDED') return 'bg-slate-100 text-slate-700 ring-1 ring-slate-200';
  return 'bg-amber-50 text-amber-700 ring-1 ring-amber-200';
};

export const SchoolRegistrationRequestsPage = () => {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState('');
  const [selectedRequestId, setSelectedRequestId] = useState<string | null>(null);
  const [detailRequestId, setDetailRequestId] = useState<string | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const selectedStatus = (searchParams.get('status')?.toUpperCase() as SchoolRegistrationRequestStatusFilter | null) || 'PENDING';
  const requestsQuery = useAppQuery({
    queryKey: ['district', 'school-registration-requests', selectedStatus, search],
    queryFn: () => districtService.schoolRegistrationRequests({ search, status: selectedStatus }),
  });

  const decisionMutation = useMutation({
    mutationFn: ({ requestId, decision, rejectionReason: reason }: { requestId: string; decision: 'APPROVE' | 'REJECT'; rejectionReason?: string }) =>
      decision === 'APPROVE'
        ? districtService.approveSchoolRegistrationRequest(requestId)
        : districtService.rejectSchoolRegistrationRequest(requestId, { rejectionReason: reason ?? '' }),
    onSuccess: () => {
      setSelectedRequestId(null);
      setDetailRequestId(null);
      setRejectionReason('');
      queryClient.invalidateQueries({ queryKey: ['district', 'school-registration-requests'] });
      queryClient.invalidateQueries({ queryKey: ['district', 'school-registration-requests', 'pending-count'] });
      queryClient.invalidateQueries({ queryKey: ['district-schools'] });
      queryClient.invalidateQueries({ queryKey: ['district-dashboard'] });
    },
  });

  const selectedRequest = useMemo(
    () => (requestsQuery.data?.items ?? []).find((item) => item.requestId === selectedRequestId) ?? null,
    [requestsQuery.data?.items, selectedRequestId],
  );
  const detailRequest = useMemo(
    () => (requestsQuery.data?.items ?? []).find((item) => item.requestId === detailRequestId) ?? null,
    [detailRequestId, requestsQuery.data?.items],
  );
  const activeTab = statusTabs.find((tab) => tab.key === selectedStatus) ?? statusTabs[0];
  const loadErrorMessage = requestsQuery.error instanceof Error
    ? requestsQuery.error.message
    : 'Unable to load school registration requests.';

  if (requestsQuery.isLoading) return <LoadingState />;
  if (requestsQuery.isError || !requestsQuery.data) return <ErrorState message={loadErrorMessage} />;

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">School Registration Requests</h1>
        <p className="text-sm text-slate-600">Review, approve, or reject self-registered schools before linking them into the district workspace.</p>
      </div>

      <div className="grid gap-3 lg:grid-cols-3">
        {statusTabs.map((tab) => {
          const active = tab.key === selectedStatus;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => setSearchParams({ status: tab.key })}
              className={`rounded-[24px] border px-5 py-4 text-left transition ${active ? 'border-blue-200 bg-blue-50 shadow-sm' : 'border-slate-200 bg-white hover:border-blue-200 hover:bg-slate-50'}`}
            >
              <p className="text-sm font-semibold text-slate-900">{tab.label}</p>
              <p className="mt-1 text-sm text-slate-600">{tab.helper}</p>
            </button>
          );
        })}
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        {requestsQuery.data.metrics.map((metric) => (
          <div key={metric.label} className="rounded-2xl border border-slate-200 bg-white p-5">
            <p className="text-sm text-slate-500">{metric.label}</p>
            <p className="mt-2 text-2xl font-bold text-slate-900">{metric.value}</p>
            <p className="mt-1 text-sm text-slate-500">{metric.helperText}</p>
          </div>
        ))}
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-5">
        <div className="flex flex-col gap-3 md:flex-row md:items-center">
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold text-slate-900">{activeTab.label}</p>
            <p className="text-sm text-slate-600">{activeTab.helper}</p>
          </div>
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search by school name or EMIS number"
            className="h-11 flex-1 rounded-xl border border-slate-200 px-3 text-sm"
          />
          <Link to="/district/schools" className="inline-flex h-11 items-center justify-center rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50">
            Open Schools List
          </Link>
        </div>
      </div>

      <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-500">
              <tr>
                <th className="px-4 py-3 font-medium">School Name</th>
                <th className="px-4 py-3 font-medium">EMIS Number</th>
                <th className="px-4 py-3 font-medium">Province</th>
                <th className="px-4 py-3 font-medium">District</th>
                <th className="px-4 py-3 font-medium">Circuit</th>
                <th className="px-4 py-3 font-medium">Principal Name</th>
                <th className="px-4 py-3 font-medium">Principal Email</th>
                <th className="px-4 py-3 font-medium">School Email</th>
                <th className="px-4 py-3 font-medium">Phone Number</th>
                <th className="px-4 py-3 font-medium">Submitted Date</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {requestsQuery.data.items.length ? requestsQuery.data.items.map((item) => {
                const isPending = item.status === 'PENDING_DISTRICT_APPROVAL';
                return (
                  <tr key={item.requestId} className="border-t border-slate-100 align-top">
                    <td className="px-4 py-3 font-semibold text-slate-900">{item.schoolName}</td>
                    <td className="px-4 py-3">{item.emisNumber}</td>
                    <td className="px-4 py-3">{item.province}</td>
                    <td className="px-4 py-3">{item.district}</td>
                    <td className="px-4 py-3">{item.circuit}</td>
                    <td className="px-4 py-3">{item.principalName}</td>
                    <td className="px-4 py-3">{item.principalEmail}</td>
                    <td className="px-4 py-3">{item.schoolEmail}</td>
                    <td className="px-4 py-3">{item.phoneNumber}</td>
                    <td className="px-4 py-3">{new Date(item.submittedAt).toLocaleString()}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${statusBadgeClass(item.status)}`}>
                        {statusLabel(item.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        <Button type="button" className="bg-slate-700 hover:bg-slate-600" onClick={() => setDetailRequestId(item.requestId)}>
                          View Details
                        </Button>
                        <Button
                          type="button"
                          disabled={decisionMutation.isPending || !isPending}
                          onClick={() => decisionMutation.mutate({ requestId: item.requestId, decision: 'APPROVE' })}
                        >
                          Approve
                        </Button>
                        <Button
                          type="button"
                          className="bg-rose-600 hover:bg-rose-500"
                          disabled={decisionMutation.isPending || !isPending}
                          onClick={() => {
                            setSelectedRequestId(item.requestId);
                            setRejectionReason(item.rejectionReason ?? '');
                          }}
                        >
                          Reject
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              }) : (
                <tr>
                  <td colSpan={12} className="px-4 py-8 text-sm text-slate-500">
                    No school registration requests found for this status.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {selectedRequest ? (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/40 p-4">
          <div className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-2xl">
            <h3 className="text-lg font-semibold text-slate-900">Reject Registration Request</h3>
            <p className="mt-2 text-sm text-slate-600">Provide a reason for rejecting {selectedRequest.schoolName}.</p>
            <textarea
              value={rejectionReason}
              onChange={(event) => setRejectionReason(event.target.value)}
              rows={5}
              className="mt-4 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm"
              placeholder="Explain what the school must correct before resubmitting."
            />
            {decisionMutation.isError ? <p className="mt-3 text-sm text-rose-600">{(decisionMutation.error as Error).message}</p> : null}
            <div className="mt-4 flex justify-end gap-3">
              <Button className="bg-slate-700 hover:bg-slate-600" onClick={() => setSelectedRequestId(null)}>Cancel</Button>
              <Button
                disabled={decisionMutation.isPending || !rejectionReason.trim()}
                onClick={() => decisionMutation.mutate({ requestId: selectedRequest.requestId, decision: 'REJECT', rejectionReason })}
              >
                {decisionMutation.isPending ? 'Saving...' : 'Confirm Rejection'}
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {detailRequest ? (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/40 p-4">
          <div className="w-full max-w-3xl rounded-2xl bg-white p-6 shadow-2xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="text-lg font-semibold text-slate-900">{detailRequest.schoolName}</h3>
                <p className="mt-1 text-sm text-slate-600">EMIS {detailRequest.emisNumber} · {statusLabel(detailRequest.status)}</p>
              </div>
              <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${statusBadgeClass(detailRequest.status)}`}>
                {statusLabel(detailRequest.status)}
              </span>
            </div>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Province</p><p className="mt-1 text-sm text-slate-900">{detailRequest.province}</p></div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">District</p><p className="mt-1 text-sm text-slate-900">{detailRequest.district}</p></div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Circuit</p><p className="mt-1 text-sm text-slate-900">{detailRequest.circuit}</p></div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">School Type</p><p className="mt-1 text-sm text-slate-900">{detailRequest.schoolType}</p></div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Principal Name</p><p className="mt-1 text-sm text-slate-900">{detailRequest.principalName}</p></div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Principal Email</p><p className="mt-1 text-sm text-slate-900">{detailRequest.principalEmail}</p></div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">School Email</p><p className="mt-1 text-sm text-slate-900">{detailRequest.schoolEmail}</p></div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Phone Number</p><p className="mt-1 text-sm text-slate-900">{detailRequest.phoneNumber}</p></div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 sm:col-span-2"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Physical Address</p><p className="mt-1 text-sm text-slate-900">{detailRequest.physicalAddress}</p></div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 sm:col-span-2"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Submitted Date</p><p className="mt-1 text-sm text-slate-900">{new Date(detailRequest.submittedAt).toLocaleString()}</p></div>
            </div>
            {detailRequest.rejectionReason ? (
              <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
                <p className="font-semibold">Rejection reason</p>
                <p className="mt-1">{detailRequest.rejectionReason}</p>
              </div>
            ) : null}
            {decisionMutation.isError ? <p className="mt-4 text-sm text-rose-600">{(decisionMutation.error as Error).message}</p> : null}
            <div className="mt-5 flex flex-wrap justify-end gap-3">
              <Button type="button" className="bg-slate-700 hover:bg-slate-600" onClick={() => setDetailRequestId(null)}>Close</Button>
              <Button
                type="button"
                disabled={decisionMutation.isPending || detailRequest.status !== 'PENDING_DISTRICT_APPROVAL'}
                onClick={() => decisionMutation.mutate({ requestId: detailRequest.requestId, decision: 'APPROVE' })}
              >
                Approve
              </Button>
              <Button
                type="button"
                className="bg-rose-600 hover:bg-rose-500"
                disabled={decisionMutation.isPending || detailRequest.status !== 'PENDING_DISTRICT_APPROVAL'}
                onClick={() => {
                  setSelectedRequestId(detailRequest.requestId);
                  setRejectionReason(detailRequest.rejectionReason ?? '');
                }}
              >
                Reject
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
};
