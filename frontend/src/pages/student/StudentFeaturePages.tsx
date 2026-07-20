import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { Input } from '@/components/ui/Input';
import { InstitutionLogo } from '@/components/institutions/InstitutionLogo';
import { resolveInstitutionDisplay } from '@/lib/institutionRegistry';
import { useAppQuery } from '@/hooks/useAppQuery';
import { StudentCareerRoadmapsExplorerPage } from '@/pages/student/StudentCareerRoadmapsExplorerPage';
import { featureModulesService } from '@/services/featureModulesService';
import type {
  ScholarshipApplication,
  SchoolProfile,
  StudentCv,
  TutorMessage,
  UniversityApplication,
} from '@/types';

const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <section className="student-page-section">
    <header>
      <h1 className="student-page-title">{title}</h1>
    </header>
    <div className="space-y-5">{children}</div>
  </section>
);

const TextArea = ({ className = '', ...props }: React.TextareaHTMLAttributes<HTMLTextAreaElement>) => (
  <textarea
    className={`w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary-500 ${className}`}
    {...props}
  />
);

const fieldClass = 'space-y-1 text-sm';
const statusColor = (status?: string): 'slate' | 'emerald' | 'amber' | 'blue' => {
  const value = (status ?? '').toUpperCase();
  if (['APPROVED', 'ACCEPTED', 'APPLIED', 'SUBMITTED'].includes(value)) return 'emerald';
  if (['IN_PROGRESS', 'READY', 'WAITLISTED', 'INTERESTED', 'REQUESTED'].includes(value)) return 'amber';
  if (['DRAFT', 'SAVED', 'NOT_STARTED'].includes(value)) return 'blue';
  return 'slate';
};

const formatDate = (value?: string) => value ? new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(value)) : 'No deadline';

const emptyCv: StudentCv = {
  personalSummary: '',
  education: '',
  skills: '',
  experience: '',
  projects: '',
  certifications: '',
  references: '',
  careerObjective: '',
  readinessPercentage: 0,
};

export const StudentCvBuilderPage = () => {
  const qc = useQueryClient();
  const cv = useAppQuery({ queryKey: ['student-cv'], queryFn: featureModulesService.getCv });
  const suggestions = useAppQuery({ queryKey: ['student-cv-suggestions'], queryFn: featureModulesService.cvSuggestions });
  const [form, setForm] = useState<StudentCv>(emptyCv);
  const [preview, setPreview] = useState(false);
  const save = useMutation({
    mutationFn: () => featureModulesService.saveCv(form),
    onSuccess: (data) => {
      setForm(data);
      qc.invalidateQueries({ queryKey: ['student-cv'] });
      qc.invalidateQueries({ queryKey: ['progress-score'] });
    },
  });

  useEffect(() => {
    if (cv.data) setForm({ ...emptyCv, ...cv.data });
  }, [cv.data]);

  if (cv.isLoading) return <LoadingState message="Loading CV builder..." />;
  if (cv.isError) return <ErrorState message="Could not load your CV builder." />;

  const update = (key: keyof StudentCv, value: string) => setForm((state) => ({ ...state, [key]: value }));
  const cvFields: Array<[keyof StudentCv, string, number]> = [
    ['careerObjective', 'Career objective', 3],
    ['personalSummary', 'Personal summary', 4],
    ['education', 'Education', 4],
    ['skills', 'Skills', 4],
    ['experience', 'Experience', 5],
    ['projects', 'Projects', 4],
    ['certifications', 'Certifications', 3],
    ['references', 'References', 3],
  ];

  return <Section title="CV Builder">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <Badge color={form.readinessPercentage >= 70 ? 'emerald' : form.readinessPercentage >= 35 ? 'amber' : 'slate'}>{form.readinessPercentage}% ready</Badge>
      <div className="flex gap-2">
        <Button type="button" className="bg-primary-600 hover:bg-primary-700" onClick={() => setPreview((value) => !value)}>{preview ? 'Edit' : 'Preview'}</Button>
        <Button type="button" onClick={() => save.mutate()} disabled={save.isPending}>{save.isPending ? 'Saving...' : 'Save CV'}</Button>
      </div>
    </div>
    {preview ? (
      <div className="space-y-4 rounded border bg-white p-5">
        <div>
          <h2 className="text-lg font-semibold">{form.careerObjective || 'Career Objective'}</h2>
          <p className="mt-2 whitespace-pre-wrap text-sm text-slate-700">{form.personalSummary || 'Add a personal summary.'}</p>
        </div>
        {cvFields.slice(2).map(([key, label]) => <div key={key}>
          <h3 className="font-semibold">{label}</h3>
          <p className="whitespace-pre-wrap text-sm text-slate-700">{String(form[key] || 'Not added yet.')}</p>
        </div>)}
      </div>
    ) : (
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
          {cvFields.map(([key, label, rows]) => (
            <label key={key} className={`${fieldClass} ${rows >= 4 ? 'sm:col-span-2' : ''}`}>
              <span>{label}</span>
              <TextArea rows={rows} value={String(form[key] ?? '')} onChange={(event) => update(key, event.target.value)} />
            </label>
          ))}
        </div>
        <aside className="space-y-3 rounded border bg-slate-50 p-4">
          <h2 className="font-semibold">AI Suggestions</h2>
          {suggestions.isLoading ? <p className="text-sm text-slate-500">Generating suggestions...</p> : null}
          {suggestions.isError ? <p className="text-sm text-amber-700">Suggestions are unavailable right now.</p> : null}
          {suggestions.data ? <div className="space-y-3 text-sm text-slate-700">
            <div><p className="font-medium">Summary</p><p>{suggestions.data.summarySuggestion}</p></div>
            <div><p className="font-medium">Skills improvement</p><p>{suggestions.data.skillsImprovement}</p></div>
            <div><p className="font-medium">Cover letter draft</p><p className="whitespace-pre-wrap">{suggestions.data.coverLetterDraft}</p></div>
            <div><p className="font-medium">Job readiness tips</p><p>{suggestions.data.jobReadinessTips}</p></div>
          </div> : null}
        </aside>
      </div>
    )}
    {save.isSuccess ? <p className="text-sm text-emerald-700">CV saved.</p> : null}
    {save.isError ? <p className="text-sm text-red-600">Could not save CV.</p> : null}
  </Section>;
};

const scholarshipDefaults: ScholarshipApplication = {
  scholarshipTitle: '',
  provider: '',
  applicationDeadline: '',
  status: 'NOT_STARTED',
  checklist: '',
  requiredDocuments: '',
  reminderNotes: '',
  motivationLetterDraft: '',
  saved: true,
  notes: '',
};

export const StudentScholarshipAssistantPage = () => {
  const qc = useQueryClient();
  const applications = useAppQuery({ queryKey: ['scholarship-applications'], queryFn: featureModulesService.scholarships });
  const upcoming = useAppQuery({ queryKey: ['scholarship-applications', 'upcoming'], queryFn: featureModulesService.upcomingScholarships });
  const [form, setForm] = useState<ScholarshipApplication>(scholarshipDefaults);
  const [editingId, setEditingId] = useState<string | null>(null);
  const save = useMutation({
    mutationFn: () => editingId ? featureModulesService.updateScholarship(editingId, form) : featureModulesService.createScholarship(form),
    onSuccess: () => {
      setForm(scholarshipDefaults);
      setEditingId(null);
      qc.invalidateQueries({ queryKey: ['scholarship-applications'] });
      qc.invalidateQueries({ queryKey: ['progress-score'] });
    },
  });
  const motivation = useMutation({
    mutationFn: (id: string) => featureModulesService.motivationLetter(id),
    onSuccess: (data) => setForm((state) => ({ ...state, motivationLetterDraft: data.motivationLetterDraft })),
  });

  if (applications.isLoading) return <LoadingState message="Loading scholarship assistant..." />;
  if (applications.isError) return <ErrorState message="Could not load scholarship applications." />;

  return <Section title="Scholarship Application Assistant">
    <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
      <form className="card space-y-3 p-4 shadow-none" onSubmit={(event) => { event.preventDefault(); save.mutate(); }}>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
          <label className={fieldClass}>Scholarship title<Input value={form.scholarshipTitle} onChange={(event) => setForm((s) => ({ ...s, scholarshipTitle: event.target.value }))} required /></label>
          <label className={fieldClass}>Provider<Input value={form.provider ?? ''} onChange={(event) => setForm((s) => ({ ...s, provider: event.target.value }))} /></label>
          <label className={fieldClass}>Deadline<Input type="date" value={form.applicationDeadline ?? ''} onChange={(event) => setForm((s) => ({ ...s, applicationDeadline: event.target.value }))} /></label>
          <label className={fieldClass}>Status<select className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" value={form.status} onChange={(event) => setForm((s) => ({ ...s, status: event.target.value as ScholarshipApplication['status'] }))}>
            {['NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED', 'APPROVED', 'REJECTED'].map((status) => <option key={status}>{status}</option>)}
          </select></label>
        </div>
        <label className={fieldClass}>Application checklist<TextArea rows={3} value={form.checklist ?? ''} onChange={(event) => setForm((s) => ({ ...s, checklist: event.target.value }))} /></label>
        <label className={fieldClass}>Required documents<TextArea rows={3} value={form.requiredDocuments ?? ''} onChange={(event) => setForm((s) => ({ ...s, requiredDocuments: event.target.value }))} /></label>
        <label className={fieldClass}>Reminder notes<TextArea rows={2} value={form.reminderNotes ?? ''} onChange={(event) => setForm((s) => ({ ...s, reminderNotes: event.target.value }))} /></label>
        <label className={fieldClass}>Motivation letter draft<TextArea rows={5} value={form.motivationLetterDraft ?? ''} onChange={(event) => setForm((s) => ({ ...s, motivationLetterDraft: event.target.value }))} /></label>
        <div className="flex flex-wrap gap-2">
          <Button disabled={save.isPending}>{editingId ? 'Update application' : 'Save application'}</Button>
          {editingId ? <Button type="button" className="bg-primary-600 hover:bg-primary-700" disabled={motivation.isPending} onClick={() => motivation.mutate(editingId)}>Generate motivation letter</Button> : null}
        </div>
      </form>
      <aside className="space-y-3">
        <div className="rounded border bg-amber-50 p-3">
          <h2 className="font-semibold text-amber-900">Upcoming deadlines</h2>
          {upcoming.data?.length ? upcoming.data.map((item) => <p key={item.id} className="mt-2 text-sm text-amber-900">{item.scholarshipTitle}: {formatDate(item.applicationDeadline)}</p>) : <p className="mt-2 text-sm text-amber-800">No deadlines in the next 45 days.</p>}
        </div>
      </aside>
    </div>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
      {(applications.data ?? []).map((item) => <article key={item.id} className="rounded border bg-white p-4">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div><h3 className="font-semibold">{item.scholarshipTitle}</h3><p className="text-sm text-slate-500">{item.provider || 'Provider not set'} · {formatDate(item.applicationDeadline)}</p></div>
          <Badge color={statusColor(item.status)}>{item.status}</Badge>
        </div>
        <p className="mt-3 whitespace-pre-wrap text-sm text-slate-600">{item.requiredDocuments || 'No documents listed yet.'}</p>
        <Button type="button" className="mt-3 bg-primary-600 hover:bg-primary-700" onClick={() => { setEditingId(item.id ?? null); setForm({ ...scholarshipDefaults, ...item }); }}>Edit</Button>
      </article>)}
    </div>
  </Section>;
};

const tutorSubjects = ['MATHEMATICS', 'ENGLISH', 'SCIENCE', 'BIOLOGY', 'CHEMISTRY', 'PHYSICS', 'ACCOUNTING', 'COMPUTER_STUDIES', 'GENERAL_STUDY_HELP'];

export const StudentAiTutorPage = () => {
  const qc = useQueryClient();
  const sessions = useAppQuery({ queryKey: ['tutor-sessions'], queryFn: featureModulesService.tutorSessions });
  const [sessionId, setSessionId] = useState<string | undefined>();
  const activeSession = useAppQuery({ queryKey: ['tutor-session', sessionId], enabled: Boolean(sessionId), queryFn: () => featureModulesService.tutorSession(String(sessionId)) });
  const [subject, setSubject] = useState('MATHEMATICS');
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState<TutorMessage[]>([]);
  const ask = useMutation({
    mutationFn: () => featureModulesService.askTutor({ sessionId, subject, question }),
    onSuccess: (data) => {
      setSessionId(data.sessionId);
      setMessages(data.messages);
      setQuestion('');
      qc.invalidateQueries({ queryKey: ['tutor-sessions'] });
      qc.invalidateQueries({ queryKey: ['progress-score'] });
    },
  });

  useEffect(() => {
    if (activeSession.data) {
      setSubject(activeSession.data.subject);
      setMessages(activeSession.data.messages);
    }
  }, [activeSession.data]);

  if (sessions.isLoading) return <LoadingState message="Loading AI tutor..." />;
  if (sessions.isError) return <ErrorState message="Could not load AI tutor sessions." />;

  return <Section title="AI Tutor">
    <div className="grid gap-4 lg:grid-cols-[260px_minmax(0,1fr)]">
      <aside className="space-y-2 rounded border bg-slate-50 p-3">
        <h2 className="font-semibold">Previous sessions</h2>
        {sessions.data?.length ? sessions.data.map((session) => <button key={session.id} type="button" className="block w-full rounded border bg-white px-3 py-2 text-left text-sm hover:bg-slate-100" onClick={() => setSessionId(session.id)}>
          <span className="font-medium">{session.title}</span><br /><span className="text-xs text-slate-500">{session.subject}</span>
        </button>) : <p className="text-sm text-slate-500">No sessions yet.</p>}
      </aside>
      <div className="space-y-3">
        <div className="min-h-[320px] space-y-3 rounded border bg-white p-4">
          {messages.length ? messages.map((message, index) => <div key={message.id ?? index} className={`rounded-lg p-3 text-sm ${message.sender === 'STUDENT' ? 'ml-auto bg-primary-50 text-primary-900' : 'mr-auto bg-slate-100 text-slate-800'} max-w-[85%]`}>
            <p className="text-xs font-semibold">{message.sender === 'STUDENT' ? 'You' : 'EduRite Tutor'}</p>
            <p className="whitespace-pre-wrap">{message.message}</p>
          </div>) : <EmptyState title="Ask a question" message="Choose a subject and ask for help with a concept, problem, or study plan." />}
        </div>
        <form className="grid gap-2 sm:grid-cols-2 lg:grid-cols-[220px_minmax(0,1fr)_auto]" onSubmit={(event) => { event.preventDefault(); if (question.trim()) ask.mutate(); }}>
          <select className="rounded-lg border border-slate-300 px-3 py-2 text-sm" value={subject} onChange={(event) => setSubject(event.target.value)}>
            {tutorSubjects.map((item) => <option key={item}>{item}</option>)}
          </select>
          <Input value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Ask an academic question" />
          <Button disabled={ask.isPending || !question.trim()}>{ask.isPending ? 'Asking...' : 'Ask'}</Button>
        </form>
      </div>
    </div>
  </Section>;
};

const universityDefaults: UniversityApplication = {
  universityName: '',
  programmeName: '',
  country: '',
  intakeYear: new Date().getFullYear() + 1,
  applicationDeadline: '',
  applicationStatus: 'DRAFT',
  notes: '',
  documentReferences: '',
};

export const StudentUniversityApplicationsPage = () => {
  const qc = useQueryClient();
  const apps = useAppQuery({ queryKey: ['university-applications'], queryFn: featureModulesService.universityApplications });
  const [form, setForm] = useState<UniversityApplication>(universityDefaults);
  const [editingId, setEditingId] = useState<string | null>(null);
  const save = useMutation({
    mutationFn: () => editingId ? featureModulesService.updateUniversityApplication(editingId, form) : featureModulesService.createUniversityApplication(form),
    onSuccess: () => {
      setForm(universityDefaults);
      setEditingId(null);
      qc.invalidateQueries({ queryKey: ['university-applications'] });
      qc.invalidateQueries({ queryKey: ['progress-score'] });
    },
  });
  const remove = useMutation({ mutationFn: featureModulesService.deleteUniversityApplication, onSuccess: () => qc.invalidateQueries({ queryKey: ['university-applications'] }) });
  if (apps.isLoading) return <LoadingState message="Loading university applications..." />;
  if (apps.isError) return <ErrorState message="Could not load university applications." />;
  return <Section title="University Applications">
    <form className="grid gap-3 rounded border bg-white p-4 sm:grid-cols-2 lg:grid-cols-2" onSubmit={(event) => { event.preventDefault(); save.mutate(); }}>
      <label className={fieldClass}>University name<Input value={form.universityName} onChange={(event) => setForm((s) => ({ ...s, universityName: event.target.value }))} required /></label>
      <label className={fieldClass}>Programme name<Input value={form.programmeName} onChange={(event) => setForm((s) => ({ ...s, programmeName: event.target.value }))} required /></label>
      <label className={fieldClass}>Country<Input value={form.country ?? ''} onChange={(event) => setForm((s) => ({ ...s, country: event.target.value }))} /></label>
      <label className={fieldClass}>Intake year<Input type="number" value={form.intakeYear ?? ''} onChange={(event) => setForm((s) => ({ ...s, intakeYear: Number(event.target.value) }))} /></label>
      <label className={fieldClass}>Deadline<Input type="date" value={form.applicationDeadline ?? ''} onChange={(event) => setForm((s) => ({ ...s, applicationDeadline: event.target.value }))} /></label>
      <label className={fieldClass}>Status<select className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" value={form.applicationStatus} onChange={(event) => setForm((s) => ({ ...s, applicationStatus: event.target.value as UniversityApplication['applicationStatus'] }))}>{['DRAFT', 'READY', 'SUBMITTED', 'ACCEPTED', 'REJECTED', 'WAITLISTED'].map((status) => <option key={status}>{status}</option>)}</select></label>
      <label className={`${fieldClass} sm:col-span-2`}>Document references<TextArea rows={3} value={form.documentReferences ?? ''} onChange={(event) => setForm((s) => ({ ...s, documentReferences: event.target.value }))} /></label>
      <label className={`${fieldClass} sm:col-span-2`}>Notes<TextArea rows={3} value={form.notes ?? ''} onChange={(event) => setForm((s) => ({ ...s, notes: event.target.value }))} /></label>
      <Button className="w-full sm:w-fit" disabled={save.isPending}>{editingId ? 'Update application' : 'Add application'}</Button>
    </form>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
      {(apps.data ?? []).map((app) => {
        const institution = resolveInstitutionDisplay({ name: app.universityName, country: app.country });
        return <article key={app.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-3"><div className="flex items-start gap-3"><InstitutionLogo src={institution.logoUrl} institutionName={institution.displayName} abbreviation={institution.abbreviation} size={56} className="rounded-2xl" /><div><h3 className="font-semibold text-slate-900">{app.universityName}</h3><p className="text-sm text-slate-500">{app.programmeName} · {app.country || institution.country || 'Country not set'}</p></div></div><Badge color={statusColor(app.applicationStatus)}>{app.applicationStatus}</Badge></div>
          <p className="mt-3 text-sm text-slate-600">Deadline: {formatDate(app.applicationDeadline)}</p>
          <div className="mt-3 flex flex-col gap-2 sm:flex-row"><Button type="button" className="bg-primary-600 hover:bg-primary-700" onClick={() => { setEditingId(app.id ?? null); setForm({ ...universityDefaults, ...app }); }}>Edit</Button><Button type="button" className="bg-red-700 hover:bg-red-600" onClick={() => app.id && remove.mutate(app.id)}>Delete</Button></div>
        </article>;
      })}
    </div>
  </Section>;
};

export const StudentCareerRoadmapsPage = StudentCareerRoadmapsExplorerPage;

const schoolDefaults: SchoolProfile = { schoolName: '', country: '', city: '', contactPerson: '', contactEmail: '', notes: '' };

export const AdminSchoolPortalPage = () => {
  const qc = useQueryClient();
  const schools = useAppQuery({ queryKey: ['admin-schools'], queryFn: featureModulesService.adminSchools });
  const [form, setForm] = useState<SchoolProfile>(schoolDefaults);
  const [selectedId, setSelectedId] = useState<string>('');
  const [studentId, setStudentId] = useState('');
  const summary = useAppQuery({ queryKey: ['admin-schools', selectedId, 'summary'], enabled: Boolean(selectedId), queryFn: () => featureModulesService.schoolSummary(selectedId) });
  const save = useMutation({
    mutationFn: () => selectedId && form.id ? featureModulesService.updateSchool(selectedId, form) : featureModulesService.createSchool(form),
    onSuccess: (school) => {
      setSelectedId(school.id ?? '');
      qc.invalidateQueries({ queryKey: ['admin-schools'] });
    },
  });
  const link = useMutation({
    mutationFn: () => featureModulesService.linkSchoolStudent(selectedId, studentId),
    onSuccess: () => {
      setStudentId('');
      qc.invalidateQueries({ queryKey: ['admin-schools', selectedId, 'summary'] });
    },
  });
  const selectedSchool = useMemo(() => schools.data?.find((school) => school.id === selectedId), [schools.data, selectedId]);
  useEffect(() => {
    if (selectedSchool) setForm({ ...schoolDefaults, ...selectedSchool });
  }, [selectedSchool]);
  if (schools.isLoading) return <LoadingState message="Loading school portal..." />;
  if (schools.isError) return <ErrorState message="Could not load school portal." />;
  return <Section title="School Portal">
    <div className="grid gap-4 lg:grid-cols-[300px_minmax(0,1fr)]">
      <aside className="space-y-2">
        <Button type="button" className="w-full bg-primary-600 hover:bg-primary-700" onClick={() => { setSelectedId(''); setForm(schoolDefaults); }}>New school</Button>
        {schools.data?.map((school) => <button key={school.id} type="button" className={`block w-full rounded border px-3 py-2 text-left text-sm ${selectedId === school.id ? 'border-primary-300 bg-primary-50' : 'bg-white'}`} onClick={() => setSelectedId(school.id ?? '')}>{school.schoolName}</button>)}
      </aside>
      <div className="space-y-4">
        <form className="grid gap-3 rounded border bg-white p-4 sm:grid-cols-2 lg:grid-cols-2" onSubmit={(event) => { event.preventDefault(); save.mutate(); }}>
          <label className={fieldClass}>School name<Input value={form.schoolName} onChange={(event) => setForm((s) => ({ ...s, schoolName: event.target.value }))} required /></label>
          <label className={fieldClass}>Country<Input value={form.country ?? ''} onChange={(event) => setForm((s) => ({ ...s, country: event.target.value }))} /></label>
          <label className={fieldClass}>City<Input value={form.city ?? ''} onChange={(event) => setForm((s) => ({ ...s, city: event.target.value }))} /></label>
          <label className={fieldClass}>Contact email<Input value={form.contactEmail ?? ''} onChange={(event) => setForm((s) => ({ ...s, contactEmail: event.target.value }))} /></label>
          <label className={`${fieldClass} sm:col-span-2`}>Notes<TextArea rows={3} value={form.notes ?? ''} onChange={(event) => setForm((s) => ({ ...s, notes: event.target.value }))} /></label>
          <Button className="w-full sm:w-fit" disabled={save.isPending}>Save school</Button>
        </form>
        {selectedId ? <div className="rounded border bg-white p-4">
          <h2 className="font-semibold">Linked students</h2>
          <div className="mt-3 flex flex-col gap-2 sm:flex-row"><Input placeholder="Student UUID" value={studentId} onChange={(event) => setStudentId(event.target.value)} /><Button type="button" disabled={!studentId || link.isPending} onClick={() => link.mutate()}>Link</Button></div>
          {summary.data ? <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            <div className="rounded border p-3"><p className="text-xs text-slate-500">Students</p><p className="text-2xl font-semibold">{summary.data.linkedStudents}</p></div>
            <div className="rounded border p-3"><p className="text-xs text-slate-500">Psychometric done</p><p className="text-2xl font-semibold">{summary.data.psychometricCompleted}</p></div>
            <div className="rounded border p-3"><p className="text-xs text-slate-500">Complete profiles</p><p className="text-2xl font-semibold">{summary.data.completeProfiles}</p></div>
            <div className="rounded border p-3"><p className="text-xs text-slate-500">Tracked apps</p><p className="text-2xl font-semibold">{summary.data.trackedApplications}</p></div>
          </div> : null}
          <div className="mt-3 space-y-2">{summary.data?.students.map((student) => <p key={student.studentId} className="rounded border bg-slate-50 p-2 text-sm">{student.name || student.studentId} · {student.profileCompleteness}% profile</p>)}</div>
        </div> : null}
      </div>
    </div>
  </Section>;
};








