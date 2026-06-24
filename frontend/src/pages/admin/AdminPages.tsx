import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/tables/DataTable';
import { MetricCard } from '@/components/cards/MetricCard';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { useAppQuery } from '@/hooks/useAppQuery';
import { adminService, type AdminAnalytics, type AdminBulkUploadResult, type AdminBursary, type AdminCompany, type AdminDistrictManagementResponse, type AdminPlatformSettings, type AdminUser } from '@/services/adminService';
import { notificationService } from '@/services/notificationService';
import type { ApiError } from '@/types';

const Header = ({ title, subtitle }: { title: string; subtitle: string }) => (
  <div>
    <h1 className="text-2xl font-bold">{title}</h1>
    <p className="text-sm text-slate-600">{subtitle}</p>
  </div>
);

const fmtDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '-');
const normalizeStatusColor = (status?: string) => {
  const key = (status ?? '').toUpperCase();
  if (['ACTIVE', 'APPROVED'].includes(key)) return 'emerald' as const;
  if (['PENDING', 'PENDING_APPROVAL', 'MORE_INFO_REQUIRED'].includes(key)) return 'amber' as const;
  if (['SUSPENDED', 'REJECTED', 'DELETED', 'CLOSED', 'ARCHIVED'].includes(key)) return 'slate' as const;
  return 'blue' as const;
};

const MiniBarChart = ({ title, data }: { title: string; data: Array<{ month: string; total: number }> }) => {
  const max = Math.max(1, ...data.map((item) => item.total));
  return (
    <div className="card p-4">
      <h3 className="font-semibold">{title}</h3>
      {data.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">No data available.</p>
      ) : (
        <div className="mt-4 flex h-44 items-end gap-2">
          {data.map((item) => (
            <div key={item.month} className="flex min-w-0 flex-1 flex-col items-center justify-end gap-2">
              <div className="w-full rounded-t bg-primary-500/80" style={{ height: `${Math.max(6, (item.total / max) * 120)}px` }} title={`${item.month}: ${item.total}`} />
              <span className="text-[10px] text-slate-500">{item.month.slice(5)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const useAdminAnalytics = () => useAppQuery<AdminAnalytics>({ queryKey: ['admin', 'analytics'], queryFn: () => adminService.getAnalytics() });

export const AdminDashboardPage = () => {
  const analytics = useAdminAnalytics();
  if (analytics.isLoading) return <LoadingState />;
  if (analytics.error || !analytics.data) return <ErrorState message="Unable to load admin dashboard analytics." />;

  const data = analytics.data;
  return (
    <section className="space-y-6">
      <Header title="Admin Dashboard" subtitle="Live platform overview sourced from production admin APIs." />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-2 xl:grid-cols-4">
        <MetricCard title="Total users" value={data.totalUsers} />
        <MetricCard title="Students" value={data.totalStudents} />
        <MetricCard title="Companies" value={data.totalCompanies} subtitle={`${data.pendingCompanyApprovals} pending approvals`} />
        <MetricCard title="Applications" value={data.totalApplicationsSubmitted} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <MiniBarChart title="Registrations by month" data={data.registrationsByMonth} />
        <MiniBarChart title="Applications by month" data={data.applicationsByMonth} />
      </div>
      <DataTable
        columns={[
          { key: 'fullName', header: 'Recent user' },
          { key: 'email', header: 'Email' },
          { key: 'roles', header: 'Roles', render: (row) => row.roles.join(', ') },
          { key: 'createdAt', header: 'Joined', render: (row) => fmtDate(row.createdAt) },
        ]}
        data={data.recentRegistrations.map((row) => ({ ...row, id: row.id }))}
      />
    </section>
  );
};

export const AdminUsersPage = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [accountType, setAccountType] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [bulkResult, setBulkResult] = useState<AdminBulkUploadResult | null>(null);

  const users = useAppQuery<AdminUser[]>({
    queryKey: ['admin', 'users', search, status, accountType],
    queryFn: () => adminService.getUsers({ search, status, accountType }),
  });
  const rows = Array.isArray(users.data) ? users.data : [];

  const suspendUser = useMutation({ mutationFn: (id: string) => adminService.suspendUser(id), onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }) });
  const unsuspendUser = useMutation({ mutationFn: (id: string) => adminService.unsuspendUser(id), onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }) });
  const deleteUser = useMutation({ mutationFn: ({ id, reason }: { id: string; reason?: string }) => adminService.deleteUser(id, reason), onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }) });

  const uploadCsv = useMutation({
    mutationFn: (csv: File) => adminService.bulkUploadUsers(csv),
    onSuccess: (result) => {
      setBulkResult(result);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });

  return (
    <section className="space-y-6">
      <Header title="User Management" subtitle="Search, filter, suspend, unsuspend, delete, and bulk import users." />
      <div className="card grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Search name/email" value={search} onChange={(event) => setSearch(event.target.value)} />
        <select className="rounded-lg border border-slate-300 px-3 py-2 text-sm" value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">All statuses</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="SUSPENDED">SUSPENDED</option>
          <option value="PENDING">PENDING</option>
          <option value="DELETED">DELETED</option>
        </select>
        <select className="rounded-lg border border-slate-300 px-3 py-2 text-sm" value={accountType} onChange={(event) => setAccountType(event.target.value)}>
          <option value="">All roles</option>
          <option value="STUDENT">STUDENT</option>
          <option value="COMPANY">COMPANY</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <Button type="button" onClick={() => void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })}>Refresh</Button>
      </div>

      <div className="card space-y-3 p-4">
        <p className="text-sm font-semibold">Bulk Upload CSV</p>
        <div className="flex flex-wrap items-center gap-3">
          <input type="file" accept=".csv,text/csv" onChange={(event) => setFile(event.target.files?.[0] ?? null)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <Button type="button" onClick={() => file && uploadCsv.mutate(file)} disabled={!file || uploadCsv.isPending}>Upload CSV</Button>
          <Button type="button" className="bg-slate-700 hover:bg-slate-600" onClick={async () => {
            const template = await adminService.getBulkUploadTemplate();
            const blob = new Blob([template], { type: 'text/csv;charset=utf-8' });
            const href = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = href;
            a.download = 'admin-user-bulk-upload-template.csv';
            a.click();
            URL.revokeObjectURL(href);
          }}>Download Template</Button>
        </div>
        {bulkResult ? <p className="text-sm text-emerald-700">Processed {bulkResult.totalRows} rows: {bulkResult.successfulRows} successful, {bulkResult.failedRows} failed.</p> : null}
        {bulkResult && bulkResult.errors.length > 0 ? (
          <div className="max-h-40 overflow-auto rounded border border-red-100 bg-red-50 p-2 text-xs text-red-700">
            {bulkResult.errors.map((error) => <div key={`${error.rowNumber}-${error.message}`}>Row {error.rowNumber}: {error.message}</div>)}
          </div>
        ) : null}
      </div>

      {users.isLoading ? <LoadingState /> : null}
      {users.error ? <ErrorState message="Unable to load users." /> : null}
      {!users.isLoading && rows.length === 0 ? <EmptyState title="No users found" message="No users match the current filters." /> : null}
      {rows.length > 0 ? (
        <DataTable
          columns={[
            { key: 'fullName', header: 'User' },
            { key: 'email', header: 'Email' },
            { key: 'roles', header: 'Roles', render: (row) => row.roles.join(', ') },
            { key: 'companyApprovalStatus', header: 'Company status', render: (row) => row.companyApprovalStatus ?? '-' },
            { key: 'status', header: 'Status', render: (row) => <Badge color={normalizeStatusColor(row.status)}>{row.status}</Badge> },
            { key: 'createdAt', header: 'Created', render: (row) => fmtDate(row.createdAt) },
            {
              key: 'id',
              header: 'Actions',
              render: (row) => (
                <div className="flex flex-wrap gap-2">
                  {row.active
                    ? <button className="text-amber-700" onClick={() => suspendUser.mutate(row.id)}>Suspend</button>
                    : <button className="text-emerald-700" onClick={() => unsuspendUser.mutate(row.id)}>Unsuspend</button>}
                  <button className="text-red-600" onClick={() => {
                    if (!window.confirm(`Delete account for ${row.email}?`)) return;
                    deleteUser.mutate({ id: row.id, reason: 'Deleted from admin user management' });
                  }}>Delete</button>
                </div>
              ),
            },
          ]}
          data={rows}
        />
      ) : null}
    </section>
  );
};

export const AdminRolesPage = () => {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [permissions, setPermissions] = useState('');
  const roles = useAppQuery({ queryKey: ['admin', 'roles'], queryFn: () => adminService.getRoles() });

  const createRole = useMutation({
    mutationFn: () => adminService.createRole({ name, permissions: permissions.split(',').map((item) => item.trim()).filter(Boolean) }),
    onSuccess: () => {
      setName('');
      setPermissions('');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'roles'] });
    },
  });

  return (
    <section className="space-y-6">
      <Header title="Roles Management" subtitle="Maintain admin role templates and permissions." />
      <div className="card grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-[1fr_2fr_auto]">
        <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="ROLE_CONTENT_REVIEWER" value={name} onChange={(event) => setName(event.target.value)} />
        <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="USER_MANAGE,ANALYTICS_VIEW" value={permissions} onChange={(event) => setPermissions(event.target.value)} />
        <Button type="button" onClick={() => createRole.mutate()} disabled={!name.trim() || createRole.isPending}>Add role</Button>
      </div>
      <DataTable columns={[{ key: 'name', header: 'Role' }, { key: 'permissions', header: 'Permissions', render: (row) => Array.isArray(row.permissions) ? row.permissions.join(', ') : '' }]} data={Array.isArray(roles.data) ? roles.data : []} />
    </section>
  );
};

export const AdminPendingApprovalsPage = () => {
  const [status, setStatus] = useState('PENDING');
  const [search, setSearch] = useState('');
  const companies = useAppQuery<AdminCompany[]>({
    queryKey: ['admin', 'companies', status, search],
    queryFn: () => adminService.listCompanies({ status, search }),
  });
  if (companies.isLoading) return <LoadingState />;
  if (companies.error) return <ErrorState message="Unable to load company approvals." />;
  const rows = Array.isArray(companies.data) ? companies.data : [];
  return (
    <section className="space-y-6">
      <Header title="Company Moderation" subtitle="Review company registrations and execute approval/moderation actions." />
      <div className="card grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-3">
        <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Search company/email/registration" value={search} onChange={(event) => setSearch(event.target.value)} />
        <select className="rounded-lg border border-slate-300 px-3 py-2 text-sm" value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="PENDING">PENDING</option>
          <option value="APPROVED">APPROVED</option>
          <option value="MORE_INFO_REQUIRED">MORE_INFO_REQUIRED</option>
          <option value="SUSPENDED">SUSPENDED</option>
          <option value="REJECTED">REJECTED</option>
        </select>
      </div>
      {rows.length === 0 ? <EmptyState title="No companies found" message="No company records match the current filters." /> : (
        <DataTable columns={[
          { key: 'companyName', header: 'Company' },
          { key: 'officialEmail', header: 'Email' },
          { key: 'registrationNumber', header: 'Registration Number' },
          { key: 'status', header: 'Status', render: (row) => <Badge color={normalizeStatusColor(row.status)}>{row.status}</Badge> },
          { key: 'id', header: 'Actions', render: (row) => <Link className="text-primary-700" to={`/admin/companies/${row.id}`}>Review</Link> },
        ]} data={rows} />
      )}
    </section>
  );
};

export const AdminCompanyReviewPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [notes, setNotes] = useState('');
  const detail = useAppQuery<AdminCompany>({ queryKey: ['admin', 'company', id], enabled: Boolean(id), queryFn: () => adminService.getCompanyDetail(String(id)) });

  const run = async (fn: () => Promise<unknown>) => {
    await fn();
    await queryClient.invalidateQueries({ queryKey: ['admin', 'companies'] });
    navigate('/admin/pending-approvals');
  };

  if (detail.isLoading) return <LoadingState />;
  if (detail.error || !detail.data) return <ErrorState message="Unable to load company details." />;
  const company = detail.data;
  return (
    <section className="space-y-6">
      <Header title="Review Company" subtitle="Approve, reject, suspend, reactivate, or delete this company." />
      <div className="card space-y-2 p-4 text-sm">
        <p><span className="font-semibold">Company:</span> {company.companyName}</p>
        <p><span className="font-semibold">Registration:</span> {company.registrationNumber}</p>
        <p><span className="font-semibold">Email:</span> {company.officialEmail}</p>
        <p><span className="font-semibold">Status:</span> <Badge color={normalizeStatusColor(company.status)}>{company.status}</Badge></p>
        {company.reviewNotes ? <p><span className="font-semibold">Current notes:</span> {company.reviewNotes}</p> : null}
      </div>
      <div className="card space-y-3 p-4">
        <label className="text-sm">
          Admin notes
          <textarea className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" rows={3} value={notes} onChange={(event) => setNotes(event.target.value)} />
        </label>
        <div className="flex flex-wrap gap-2">
          <Button type="button" onClick={() => id && void run(() => adminService.approveCompany(id, notes))}>Approve</Button>
          <Button type="button" className="bg-amber-600 hover:bg-amber-500" onClick={() => id && void run(() => adminService.requestCompanyMoreInfo(id, notes))}>Request More Info</Button>
          <Button type="button" className="bg-slate-700 hover:bg-slate-600" onClick={() => id && void run(() => adminService.reactivateCompany(id, notes))}>Reactivate</Button>
          <Button type="button" className="bg-red-600 hover:bg-red-500" onClick={() => id && void run(() => adminService.rejectCompany(id, notes))}>Reject</Button>
          <Button type="button" className="bg-red-800 hover:bg-red-700" onClick={() => id && void run(() => adminService.suspendCompany(id, notes))}>Suspend</Button>
          <Button type="button" className="bg-black hover:bg-slate-800" onClick={() => {
            if (!window.confirm('Delete this company and disable account access?')) return;
            if (id) void run(() => adminService.deleteCompany(id, notes || 'Deleted by admin'));
          }}>Delete Company</Button>
        </div>
      </div>
    </section>
  );
};

export const AdminBursaryModerationPage = () => {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState('');
  const [companyId, setCompanyId] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  const bursaries = useAppQuery<AdminBursary[]>({
    queryKey: ['admin', 'bursaries', status, companyId, fromDate, toDate],
    queryFn: () => adminService.listBursaries({ status, companyId, fromDate, toDate }),
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['admin', 'bursaries'] });

  if (bursaries.isLoading) return <LoadingState />;
  if (bursaries.error) return <ErrorState message="Unable to load bursaries." />;
  const rows = Array.isArray(bursaries.data) ? bursaries.data : [];

  return (
    <section className="space-y-6">
      <Header title="Bursary Management" subtitle="Filter and moderate bursaries across all companies." />
      <div className="card grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <select className="rounded-lg border border-slate-300 px-3 py-2 text-sm" value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">All statuses</option>
          <option value="PENDING_APPROVAL">PENDING_APPROVAL</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="SUSPENDED">SUSPENDED</option>
          <option value="CLOSED">CLOSED</option>
        </select>
        <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Company ID" value={companyId} onChange={(event) => setCompanyId(event.target.value)} />
        <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} />
        <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} />
        <Button type="button" onClick={refresh}>Refresh</Button>
      </div>
      {rows.length === 0 ? <EmptyState title="No bursaries found" message="No bursaries match the current filters." /> : (
        <DataTable columns={[
          { key: 'title', header: 'Bursary' },
          { key: 'companyName', header: 'Company' },
          { key: 'status', header: 'Status', render: (row) => <Badge color={normalizeStatusColor(row.status)}>{row.status}</Badge> },
          { key: 'applicantCount', header: 'Applicants' },
          { key: 'applicationEndDate', header: 'Application End', render: (row) => row.applicationEndDate ?? '-' },
          {
            key: 'id',
            header: 'Actions',
            render: (row) => (
              <div className="flex flex-wrap gap-2">
                <button className="text-emerald-700" onClick={async () => { await adminService.reviewBursary(row.id, 'APPROVED', 'Approved by admin'); refresh(); }}>Approve</button>
                <button className="text-amber-700" onClick={async () => { await adminService.suspendBursary(row.id, 'Suspended by admin'); refresh(); }}>Suspend</button>
                <button className="text-slate-700" onClick={async () => { await adminService.reactivateBursary(row.id, 'Reactivated by admin'); refresh(); }}>Reactivate</button>
                <button className="text-red-600" onClick={async () => {
                  if (!window.confirm('Delete this bursary?')) return;
                  await adminService.deleteBursary(row.id, 'Deleted by admin');
                  refresh();
                }}>Delete</button>
              </div>
            ),
          },
        ]} data={rows} />
      )}
    </section>
  );
};

export const AdminSubscriptionsPage = () => {
  const analytics = useAdminAnalytics();
  if (analytics.isLoading) return <LoadingState />;
  if (analytics.error || !analytics.data) return <ErrorState message="Unable to load company overview." />;
  return (
    <section className="space-y-6">
      <Header title="Company Oversight" subtitle="Company moderation and approval metrics." />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <MetricCard title="Total companies" value={analytics.data.totalCompanies} />
        <MetricCard title="Pending approvals" value={analytics.data.pendingCompanyApprovals} />
        <MetricCard title="Suspended companies" value={analytics.data.suspendedCompanies} />
      </div>
    </section>
  );
};

export const AdminPaymentsPage = () => {
  const analytics = useAdminAnalytics();
  if (analytics.isLoading) return <LoadingState />;
  if (analytics.error || !analytics.data) return <ErrorState message="Unable to load applications overview." />;
  return (
    <section className="space-y-6">
      <Header title="Applications Oversight" subtitle="Application and bursary operational metrics." />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <MetricCard title="Applications submitted" value={analytics.data.totalApplicationsSubmitted} />
        <MetricCard title="Active bursaries" value={analytics.data.activeBursaries} />
        <MetricCard title="Closed/expired bursaries" value={analytics.data.closedOrExpiredBursaries} />
      </div>
    </section>
  );
};

export const AdminNotificationTemplatesPage = () => {
  const settings = useAppQuery<AdminPlatformSettings>({ queryKey: ['admin', 'settings-notices'], queryFn: () => adminService.getSettings() });
  if (settings.isLoading) return <LoadingState />;
  if (settings.error || !settings.data) return <ErrorState message="Unable to load support and notice settings." />;
  return (
    <section className="space-y-6">
      <Header title="Support & Contact" subtitle="Live support contact information from system settings." />
      <div className="card space-y-2 p-4 text-sm">
        <p><span className="font-semibold">Support email:</span> {settings.data.supportEmail ?? '-'}</p>
        <p><span className="font-semibold">Platform contact info:</span> {settings.data.platformContactInfo ?? '-'}</p>
        <p><span className="font-semibold">Maintenance mode:</span> {settings.data.maintenanceModeEnabled ? 'Enabled' : 'Disabled'}</p>
      </div>
    </section>
  );
};

export const AdminAnalyticsPage = () => {
  const analytics = useAdminAnalytics();
  if (analytics.isLoading) return <LoadingState />;
  if (analytics.error || !analytics.data) return <ErrorState message="Unable to load analytics." />;
  const data = analytics.data;
  return (
    <section className="space-y-6">
      <Header title="Analytics" subtitle="Detailed registrations, applications, and bursary performance." />
      <div className="grid gap-4 lg:grid-cols-2">
        <MiniBarChart title="Registrations by month" data={data.registrationsByMonth} />
        <MiniBarChart title="Bursaries by month" data={data.bursariesByMonth} />
      </div>
      <DataTable columns={[{ key: 'bursaryTitle', header: 'Bursary' }, { key: 'totalApplications', header: 'Applications' }]} data={data.applicationsPerBursary.map((row) => ({ ...row, id: row.bursaryId }))} />
    </section>
  );
};

export const AdminAuditLogsPage = () => {
  const logs = useAppQuery({ queryKey: ['admin', 'audit-logs'], queryFn: () => adminService.getAuditLogs() });
  return (
    <section className="space-y-6">
      <Header title="Audit Logs" subtitle="Traceable admin actions for moderation and settings changes." />
      {logs.isLoading ? <LoadingState /> : null}
      {logs.error ? <ErrorState message="Unable to load audit logs." /> : null}
      <DataTable
        columns={[
          { key: 'action', header: 'Action' },
          { key: 'entityType', header: 'Entity' },
          { key: 'entityId', header: 'Entity ID' },
          { key: 'details', header: 'Details', render: (row) => <span className="text-xs">{JSON.stringify(row.details)}</span> },
          { key: 'createdAt', header: 'Created', render: (row) => fmtDate(row.createdAt) },
        ]}
        data={Array.isArray(logs.data) ? logs.data : []}
      />
    </section>
  );
};

export const AdminSettingsPage = () => {
  const queryClient = useQueryClient();
  const settings = useAppQuery<AdminPlatformSettings>({ queryKey: ['admin', 'settings'], queryFn: () => adminService.getSettings() });
  const { register, handleSubmit, reset } = useForm<AdminPlatformSettings>();
  const [savedMessage, setSavedMessage] = useState('');

  useEffect(() => {
    if (settings.data) {
      reset(settings.data);
    }
  }, [settings.data, reset]);

  const saveSettings = useMutation({
    mutationFn: (payload: Partial<AdminPlatformSettings>) => adminService.updateSettings(payload),
    onSuccess: async (updated) => {
      setSavedMessage(`Settings saved at ${fmtDate(updated.updatedAt)}`);
      await queryClient.invalidateQueries({ queryKey: ['admin', 'settings'] });
    },
  });

  if (settings.isLoading) return <LoadingState />;
  if (settings.error || !settings.data) return <ErrorState message="Unable to load system settings." />;

  return (
    <section className="space-y-6">
      <Header title="System Settings" subtitle="Manage global platform policies and moderation controls." />
      <form className="card grid gap-4 p-4 sm:grid-cols-2 lg:grid-cols-2" onSubmit={handleSubmit((values) => saveSettings.mutate(values))}>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register('companySelfRegistrationEnabled')} /> Enable company self-registration</label>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register('manualCompanyApprovalRequired')} /> Require manual company approval</label>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register('bursaryPostingEnabled')} /> Enable bursary posting</label>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register('studentRegistrationEnabled')} /> Enable student registration</label>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register('bursaryModerationRequired')} /> Require bursary moderation before publish</label>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register('aiGuidanceEnabled')} /> Enable AI guidance features</label>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register('maintenanceModeEnabled')} /> Enable maintenance mode</label>
        <label className="text-sm">Max CSV bulk upload rows
          <input type="number" min={1} max={10000} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('maxCsvBulkUploadRows', { valueAsNumber: true })} />
        </label>
        <label className="text-sm sm:col-span-2">Support email
          <input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('supportEmail')} />
        </label>
        <label className="text-sm sm:col-span-2">Platform contact info
          <textarea rows={3} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('platformContactInfo')} />
        </label>
        <div className="sm:col-span-2 flex items-center gap-3">
          <Button type="submit" disabled={saveSettings.isPending}>Save Settings</Button>
          {savedMessage ? <span className="text-sm text-emerald-700">{savedMessage}</span> : null}
        </div>
      </form>
    </section>
  );
};

export const AdminNotificationsPage = () => {
  const queryClient = useQueryClient();
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [type, setType] = useState<'INFO' | 'WARNING' | 'SUCCESS' | 'PAYMENT' | 'SYSTEM' | 'ANNOUNCEMENT'>('ANNOUNCEMENT');
  const [priority, setPriority] = useState<'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'>('NORMAL');
  const [targetAudience, setTargetAudience] = useState<'ALL' | 'FILTERED' | 'SELECTED'>('ALL');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('ACTIVE');
  const [subscriptionPlan, setSubscriptionPlan] = useState('');
  const [grade, setGrade] = useState('');
  const [school, setSchool] = useState('');
  const [search, setSearch] = useState('');
  const [selectedUserIdsRaw, setSelectedUserIdsRaw] = useState('');

  const preview = useAppQuery({
    queryKey: ['admin', 'notifications', 'preview', role, status, subscriptionPlan, grade, school, search],
    queryFn: () => notificationService.filterUsers({ role, status, subscriptionPlan, grade, school, search, activeOnly: true, size: 200 }),
    enabled: targetAudience === 'FILTERED',
  });
  const sent = useAppQuery({ queryKey: ['admin', 'notifications', 'list'], queryFn: () => notificationService.listAdmin({ page: 0, size: 30 }) });

  const send = useMutation({
    mutationFn: () => notificationService.sendAdmin({
      title: title.trim(),
      message: message.trim(),
      type,
      priority,
      targetAudience,
      filters: targetAudience === 'FILTERED' ? { role, status, subscriptionPlan, grade, schoolId: school, search, activeOnly: true } : undefined,
      selectedUserIds: targetAudience === 'SELECTED' ? selectedUserIdsRaw.split(',').map((item) => item.trim()).filter(Boolean) : undefined,
    }),
    onSuccess: async () => {
      setTitle('');
      setMessage('');
      await queryClient.invalidateQueries({ queryKey: ['admin', 'notifications', 'list'] });
    },
  });

  const deleteSent = useMutation({
    mutationFn: (id: string) => notificationService.deleteAdmin(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'notifications', 'list'] }),
  });

  const hasValidationError = !title.trim() || !message.trim();

  return (
    <section className="space-y-6">
      <Header title="Notifications" subtitle="Send platform notifications to all users, filtered user groups, or selected users." />
      <div className="card space-y-4 p-4">
        <div className="grid gap-3 md:grid-cols-2">
          <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Title" value={title} onChange={(event) => setTitle(event.target.value)} />
          <select className="rounded-lg border border-slate-300 px-3 py-2 text-sm" value={type} onChange={(event) => setType(event.target.value as typeof type)}>
            <option value="INFO">INFO</option><option value="WARNING">WARNING</option><option value="SUCCESS">SUCCESS</option><option value="PAYMENT">PAYMENT</option><option value="SYSTEM">SYSTEM</option><option value="ANNOUNCEMENT">ANNOUNCEMENT</option>
          </select>
          <textarea className="rounded-lg border border-slate-300 px-3 py-2 text-sm md:col-span-2" rows={4} placeholder="Message" value={message} onChange={(event) => setMessage(event.target.value)} />
          <select className="rounded-lg border border-slate-300 px-3 py-2 text-sm" value={priority} onChange={(event) => setPriority(event.target.value as typeof priority)}>
            <option value="LOW">LOW</option><option value="NORMAL">NORMAL</option><option value="HIGH">HIGH</option><option value="URGENT">URGENT</option>
          </select>
          <select className="rounded-lg border border-slate-300 px-3 py-2 text-sm" value={targetAudience} onChange={(event) => setTargetAudience(event.target.value as typeof targetAudience)}>
            <option value="ALL">All Users</option><option value="FILTERED">Filter Users</option><option value="SELECTED">Select Users</option>
          </select>
        </div>
        {targetAudience === 'FILTERED' ? (
          <div className="grid gap-3 md:grid-cols-3">
            <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Role (ROLE_STUDENT)" value={role} onChange={(event) => setRole(event.target.value)} />
            <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="User Status (ACTIVE)" value={status} onChange={(event) => setStatus(event.target.value)} />
            <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Plan (PREMIUM)" value={subscriptionPlan} onChange={(event) => setSubscriptionPlan(event.target.value)} />
            <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Grade" value={grade} onChange={(event) => setGrade(event.target.value)} />
            <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="School ID" value={school} onChange={(event) => setSchool(event.target.value)} />
            <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Search name/email" value={search} onChange={(event) => setSearch(event.target.value)} />
            <p className="text-sm text-slate-600 md:col-span-3">Matching users: {preview.data?.totalMatchedUsers ?? 0}</p>
          </div>
        ) : null}
        {targetAudience === 'SELECTED' ? <input className="rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Comma-separated user IDs" value={selectedUserIdsRaw} onChange={(event) => setSelectedUserIdsRaw(event.target.value)} /> : null}
        <div className="flex items-center gap-3">
          <Button type="button" onClick={() => send.mutate()} disabled={send.isPending || hasValidationError}>Send notification</Button>
          {hasValidationError ? <span className="text-sm text-red-600">Title and message are required.</span> : null}
          {send.isSuccess ? <span className="text-sm text-emerald-700">Notification sent successfully.</span> : null}
          {send.isError ? <span className="text-sm text-red-600">{(send.error as ApiError | null)?.message ?? 'Unable to send notification.'}</span> : null}
        </div>
      </div>
      <div className="card p-4">
        <h3 className="mb-3 font-semibold">Sent notifications</h3>
        <DataTable
          columns={[
            { key: 'title', header: 'Title' },
            { key: 'type', header: 'Type' },
            { key: 'priority', header: 'Priority' },
            { key: 'targetAudience', header: 'Target' },
            { key: 'recipients', header: 'Recipients' },
            { key: 'createdAt', header: 'Created', render: (row) => fmtDate(row.createdAt) },
            { key: 'id', header: 'Actions', render: (row) => <button className="text-red-600" onClick={() => deleteSent.mutate(row.id)}>Delete</button> },
          ]}
          data={sent.data?.content ?? []}
        />
      </div>
    </section>
  );
};

export const AdminDistrictManagementPage = () => {
  const queryClient = useQueryClient();
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingDistrictId, setEditingDistrictId] = useState<string | null>(null);
  const [viewDistrictId, setViewDistrictId] = useState<string | null>(null);
  const [createdCredentials, setCreatedCredentials] = useState<{ username?: string | null; temporaryPassword?: string | null } | null>(null);
  const { register, handleSubmit, reset, formState: { errors } } = useForm<{
    districtName: string;
    districtCode: string;
    directorName: string;
    adminName: string;
    adminEmail: string;
    phoneNumber: string;
    physicalAddress: string;
    status: 'ACTIVE' | 'INACTIVE';
  }>({
    defaultValues: {
      districtName: '',
      districtCode: '',
      directorName: '',
      adminName: '',
      adminEmail: '',
      phoneNumber: '',
      physicalAddress: '',
      status: 'ACTIVE',
    },
  });
  const editForm = useForm<{
    directorName: string;
    adminName: string;
    adminEmail: string;
    phoneNumber: string;
    status: 'ACTIVE' | 'INACTIVE';
  }>({
    defaultValues: {
      directorName: '',
      adminName: '',
      adminEmail: '',
      phoneNumber: '',
      status: 'ACTIVE',
    },
  });

  const districts = useAppQuery<AdminDistrictManagementResponse>({
    queryKey: ['admin', 'districts'],
    queryFn: () => adminService.getDistricts(),
  });

  const createDistrict = useMutation({
    mutationFn: adminService.createDistrict,
    onSuccess: async (created) => {
      setCreatedCredentials({ username: created.username, temporaryPassword: created.temporaryPassword });
      setShowCreateForm(false);
      reset();
      await queryClient.invalidateQueries({ queryKey: ['admin', 'districts'] });
    },
  });
  const updateDistrict = useMutation({
    mutationFn: ({ districtId, payload }: { districtId: string; payload: { directorName: string; adminName: string; adminEmail: string; phoneNumber: string; status: 'ACTIVE' | 'INACTIVE' } }) =>
      adminService.updateDistrict(districtId, payload),
    onSuccess: async () => {
      setEditingDistrictId(null);
      await queryClient.invalidateQueries({ queryKey: ['admin', 'districts'] });
    },
  });
  const createDistrictAdmin = useMutation({
    mutationFn: (districtId: string) => adminService.createDistrictAdmin(districtId),
    onSuccess: async (created) => {
      setCreatedCredentials({ username: created.username, temporaryPassword: created.temporaryPassword });
      await queryClient.invalidateQueries({ queryKey: ['admin', 'districts'] });
    },
  });

  if (districts.isLoading) return <LoadingState />;
  if (districts.error || !districts.data) return <ErrorState message="Unable to load district management." />;
  const selectedDistrict = districts.data.items.find((item) => item.id === viewDistrictId) ?? null;
  const editingDistrict = districts.data.items.find((item) => item.id === editingDistrictId) ?? null;

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <Header title="District Management" subtitle="Create and manage districts directly without province setup." />
        <Button type="button" onClick={() => setShowCreateForm((current) => !current)}>
          {showCreateForm ? 'Close Form' : '+ Create District'}
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {districts.data.metrics.map((metric) => (
          <MetricCard key={metric.label} title={metric.label} value={metric.value} subtitle={metric.helperText} />
        ))}
      </div>

      {createdCredentials?.username && createdCredentials.temporaryPassword ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
          District admin account created. Username: <span className="font-semibold">{createdCredentials.username}</span>. Temporary password: <span className="font-semibold">{createdCredentials.temporaryPassword}</span>.
        </div>
      ) : null}

      {districts.data.items.some((item) => !item.hasAssignedAdmin) ? (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          No District Admin assigned. School registrations cannot be reviewed for one or more districts.
        </div>
      ) : null}

      {showCreateForm ? (
        <form
          className="card grid gap-4 p-5 md:grid-cols-2"
          onSubmit={handleSubmit((values) => {
            setCreatedCredentials(null);
            createDistrict.mutate(values);
          })}
        >
          <label className="text-sm font-medium text-slate-700">District Name<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('districtName', { required: 'District name is required' })} /></label>
          <label className="text-sm font-medium text-slate-700">District Code<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('districtCode', { required: 'District code is required' })} /></label>
          <label className="text-sm font-medium text-slate-700">District Director Name<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('directorName', { required: 'Director name is required' })} /></label>
          <label className="text-sm font-medium text-slate-700">District Admin Name<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('adminName', { required: 'District admin name is required' })} /></label>
          <label className="text-sm font-medium text-slate-700">District Admin Email<input type="email" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('adminEmail', { required: 'District admin email is required' })} /></label>
          <label className="text-sm font-medium text-slate-700">Phone Number<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('phoneNumber', { required: 'Phone number is required' })} /></label>
          <label className="text-sm font-medium text-slate-700 md:col-span-2">Physical Address<textarea rows={3} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('physicalAddress', { required: 'Physical address is required' })} /></label>
          <label className="text-sm font-medium text-slate-700">Status
            <select className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...register('status', { required: true })}>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </label>
          <div className="md:col-span-2 flex items-center gap-3">
            <Button type="submit" disabled={createDistrict.isPending}>{createDistrict.isPending ? 'Creating District...' : 'Create District'}</Button>
            {createDistrict.isError ? <span className="text-sm text-red-600">{createDistrict.error instanceof Error ? createDistrict.error.message : 'Unable to create district.'}</span> : null}
            {Object.values(errors)[0]?.message ? <span className="text-sm text-red-600">{String(Object.values(errors)[0]?.message ?? '')}</span> : null}
          </div>
        </form>
      ) : null}

      <DataTable
        columns={[
          { key: 'districtName', header: 'District' },
          { key: 'districtCode', header: 'Code' },
          { key: 'directorName', header: 'Director' },
          { key: 'adminName', header: 'District Admin' },
          { key: 'adminEmail', header: 'Admin Email' },
          { key: 'pendingRegistrations', header: 'Pending Requests' },
          { key: 'phoneNumber', header: 'Phone' },
          { key: 'status', header: 'Status', render: (row) => <Badge color={normalizeStatusColor(row.status)}>{row.status}</Badge> },
          { key: 'username', header: 'Username', render: (row) => row.username ?? '-' },
          {
            key: 'id',
            header: 'Actions',
            render: (row) => (
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  className="text-primary-700"
                  onClick={() => setViewDistrictId(row.id)}
                >
                  View District
                </button>
                <button
                  type="button"
                  className="text-slate-700"
                  onClick={() => {
                    setEditingDistrictId(row.id);
                    editForm.reset({
                      directorName: row.directorName || '',
                      adminName: row.adminName || '',
                      adminEmail: row.adminEmail || '',
                      phoneNumber: row.phoneNumber || '',
                      status: (row.status?.toUpperCase() === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE'),
                    });
                  }}
                >
                  Edit District
                </button>
                {!row.hasAssignedAdmin ? (
                  <button
                    type="button"
                    className="text-emerald-700"
                    onClick={() => {
                      setCreatedCredentials(null);
                      createDistrictAdmin.mutate(row.id);
                    }}
                    disabled={createDistrictAdmin.isPending}
                  >
                    Create District Admin
                  </button>
                ) : null}
                <button
                  type="button"
                  className="text-amber-700"
                  onClick={() => setViewDistrictId(row.id)}
                >
                  View Requests
                </button>
              </div>
            ),
          },
          { key: 'createdAt', header: 'Created', render: (row) => fmtDate(row.createdAt) },
        ]}
        data={districts.data.items.map((row) => ({ ...row, id: row.id }))}
      />

      {editingDistrict ? (
        <div className="card space-y-4 p-5">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-lg font-semibold text-slate-900">Edit District: {editingDistrict.districtName}</p>
              <p className="text-sm text-slate-600">Assign district leadership and contact details.</p>
            </div>
            <Button type="button" className="bg-slate-700 hover:bg-slate-600" onClick={() => setEditingDistrictId(null)}>Close</Button>
          </div>
          <form
            className="grid gap-4 md:grid-cols-2"
            onSubmit={editForm.handleSubmit((values) => updateDistrict.mutate({ districtId: editingDistrict.id, payload: values }))}
          >
            <label className="text-sm font-medium text-slate-700">District Director Name<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...editForm.register('directorName', { required: true })} /></label>
            <label className="text-sm font-medium text-slate-700">District Admin Name<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...editForm.register('adminName', { required: true })} /></label>
            <label className="text-sm font-medium text-slate-700">District Admin Email<input type="email" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...editForm.register('adminEmail', { required: true })} /></label>
            <label className="text-sm font-medium text-slate-700">Phone Number<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...editForm.register('phoneNumber', { required: true })} /></label>
            <label className="text-sm font-medium text-slate-700">Status
              <select className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" {...editForm.register('status', { required: true })}>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </label>
            <div className="md:col-span-2 flex items-center gap-3">
              <Button type="submit" disabled={updateDistrict.isPending}>{updateDistrict.isPending ? 'Saving...' : 'Save District'}</Button>
              {updateDistrict.isError ? <span className="text-sm text-red-600">{updateDistrict.error instanceof Error ? updateDistrict.error.message : 'Unable to update district.'}</span> : null}
            </div>
          </form>
        </div>
      ) : null}

      {selectedDistrict ? (
        <div className="card space-y-4 p-5">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-lg font-semibold text-slate-900">{selectedDistrict.districtName}</p>
              <p className="text-sm text-slate-600">{selectedDistrict.districtCode}</p>
            </div>
            <Button type="button" className="bg-slate-700 hover:bg-slate-600" onClick={() => setViewDistrictId(null)}>Close</Button>
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm"><span className="font-semibold text-slate-900">District Director:</span> {selectedDistrict.directorName || 'Not assigned'}</div>
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm"><span className="font-semibold text-slate-900">Assigned Admin:</span> {selectedDistrict.adminName || 'Not assigned'}</div>
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm"><span className="font-semibold text-slate-900">Admin Email:</span> {selectedDistrict.adminEmail || 'Not assigned'}</div>
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm"><span className="font-semibold text-slate-900">Username:</span> {selectedDistrict.username || '-'}</div>
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm"><span className="font-semibold text-slate-900">Number of Schools:</span> {selectedDistrict.schoolCount}</div>
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm"><span className="font-semibold text-slate-900">Pending Registrations:</span> {selectedDistrict.pendingRegistrations}</div>
          </div>
          {selectedDistrict.warningMessage ? (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">{selectedDistrict.warningMessage}</div>
          ) : (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
              District admin is assigned. School requests for this district are routed by `districtId` and can be reviewed in the district portal.
            </div>
          )}
        </div>
      ) : null}
    </section>
  );
};

