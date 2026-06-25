import { type ComponentType, useEffect, useMemo, useState } from 'react';
import {
  BookOpen,
  CheckCircle2,
  ClipboardCheck,
  Clock3,
  FileDown,
  FileText,
  GraduationCap,
  LayoutDashboard,
  LineChart,
  LogOut,
  Search,
  Send,
  Target,
  Upload,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { DashboardLogo } from '@/components/common/DashboardLogo';
import { Input } from '@/components/ui/Input';
import { DataTable } from '@/components/tables/DataTable';
import { EmptyState } from '@/components/feedback/States';
import type { LearnerAssessmentView, LearnerProgressSummary, LearnerSubjectView, LearnerTaskView, SubmissionView } from '@/services/schoolService';
import { useAuth } from '@/hooks/useAuth';

type LearnerSidebarItem =
  | 'Dashboard'
  | 'My Subjects'
  | 'Assignments & SBA'
  | 'Notes & Resources'
  | 'Exams & Quizzes'
  | 'My Submissions'
  | 'Marks & Feedback'
  | 'Progress'
  | 'Logout';

const sidebarItems: Array<{ label: LearnerSidebarItem; icon: ComponentType<{ className?: string }> }> = [
  { label: 'Dashboard', icon: LayoutDashboard },
  { label: 'My Subjects', icon: BookOpen },
  { label: 'Assignments & SBA', icon: ClipboardCheck },
  { label: 'Notes & Resources', icon: FileText },
  { label: 'Exams & Quizzes', icon: GraduationCap },
  { label: 'My Submissions', icon: Send },
  { label: 'Marks & Feedback', icon: CheckCircle2 },
  { label: 'Progress', icon: LineChart },
  { label: 'Logout', icon: LogOut },
];

type NoteItem = { id?: string; title?: string; noteText?: string; pdfUrl?: string };
type TaskOrAssessment = LearnerTaskView | LearnerAssessmentView;

const getTaskLikeId = (item: TaskOrAssessment): string => item.taskId;

type Props = {
  initialSection?: LearnerSidebarItem;
  subjects: LearnerSubjectView[];
  tasks: LearnerTaskView[];
  assessments: LearnerAssessmentView[];
  notes: NoteItem[];
  submissions: SubmissionView[];
  marks: SubmissionView[];
  progress: LearnerProgressSummary;
  onSubmitTask: (payload: { taskId: string; submissionText?: string; fileUrl?: string }) => Promise<void>;
  submitting: boolean;
  onLogout: () => void;
  successMessage?: string | null;
  onSectionChange?: (section: LearnerSidebarItem) => void;
};

export const SchoolStudentWorkspace = ({
  initialSection = 'Dashboard',
  subjects,
  tasks,
  assessments,
  notes,
  submissions,
  marks,
  progress,
  onSubmitTask,
  submitting,
  onLogout,
  successMessage,
  onSectionChange,
}: Props) => {
  const { user } = useAuth();
  const learnerName = user?.fullName || 'Learner';
  const [active, setActive] = useState<LearnerSidebarItem>(initialSection);
  const [search, setSearch] = useState('');
  const [taskId, setTaskId] = useState('');
  const [submissionText, setSubmissionText] = useState('');
  const [fileUrl, setFileUrl] = useState('');
  const [statusFilter, setStatusFilter] = useState<'Pending' | 'Submitted' | 'Marked' | 'Overdue'>('Pending');
  const [showSubmitModal, setShowSubmitModal] = useState(false);

  const submittedIds = useMemo(() => new Set(submissions.map((item) => item.taskId)), [submissions]);
  const now = Date.now();
  const pendingTasks = tasks.filter((task) => !submittedIds.has(task.taskId) && new Date(task.dueAt).getTime() >= now);
  const overdueTasks = tasks.filter((task) => !submittedIds.has(task.taskId) && new Date(task.dueAt).getTime() < now);
  const markedSubmissions = marks.filter((item) => item.released && item.marks !== null && item.marks !== undefined);
  const avgMark = markedSubmissions.length
    ? Math.round(markedSubmissions.reduce((acc, item) => acc + Number(item.marks ?? 0), 0) / markedSubmissions.length)
    : 0;
  const progressScore = progress.totalTasks ? Math.round((progress.submitted / progress.totalTasks) * 100) : 0;
  const activateSection = (section: LearnerSidebarItem) => {
    setActive(section);
    onSectionChange?.(section);
  };

  useEffect(() => {
    setActive(initialSection);
  }, [initialSection]);

  const visibleTasks = useMemo(() => {
    if (statusFilter === 'Pending') return pendingTasks;
    if (statusFilter === 'Overdue') return overdueTasks;
    if (statusFilter === 'Submitted') return tasks.filter((task) => submittedIds.has(task.taskId));
    return tasks.filter((task) => marks.some((submission) => submission.taskId === task.taskId && submission.released));
  }, [statusFilter, pendingTasks, overdueTasks, tasks, submittedIds, marks]);

  const todayItems: TaskOrAssessment[] = [...pendingTasks, ...assessments]
    .filter((item) => {
      const due = new Date(item.dueAt);
      const today = new Date();
      return due.getFullYear() === today.getFullYear() && due.getMonth() === today.getMonth() && due.getDate() === today.getDate();
    })
    .slice(0, 8);

  const filteredSubjects = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return subjects;
    return subjects.filter((item) =>
      item.subjectName.toLowerCase().includes(keyword)
      || item.teacherName.toLowerCase().includes(keyword),
    );
  }, [search, subjects]);

  const cards = [
    { label: 'Active Subjects', value: subjects.length, icon: <BookOpen className="h-4 w-4" />, section: 'My Subjects' as LearnerSidebarItem },
    { label: 'Pending Tasks', value: pendingTasks.length, icon: <Clock3 className="h-4 w-4" />, section: 'Assignments & SBA' as LearnerSidebarItem },
    { label: 'Submitted Work', value: progress.submitted, icon: <Send className="h-4 w-4" />, section: 'My Submissions' as LearnerSidebarItem },
    { label: 'Upcoming Exams', value: assessments.length, icon: <GraduationCap className="h-4 w-4" />, section: 'Exams & Quizzes' as LearnerSidebarItem },
    { label: 'Average Mark', value: `${avgMark}%`, icon: <Target className="h-4 w-4" />, section: 'Marks & Feedback' as LearnerSidebarItem },
    { label: 'Missing Submissions', value: progress.missing, icon: <ClipboardCheck className="h-4 w-4" />, section: 'Assignments & SBA' as LearnerSidebarItem },
    { label: 'Notes Available', value: notes.length, icon: <FileText className="h-4 w-4" />, section: 'Notes & Resources' as LearnerSidebarItem },
    { label: 'Progress Score', value: `${progressScore}%`, icon: <LineChart className="h-4 w-4" />, section: 'Progress' as LearnerSidebarItem },
  ];

  const submit = async () => {
    if (!taskId) return;
    await onSubmitTask({ taskId, submissionText: submissionText.trim() || undefined, fileUrl: fileUrl.trim() || undefined });
    setSubmissionText('');
    setFileUrl('');
  };

  return (
    <section className="space-y-4">
      <div className="flex gap-4">
        <aside className="hidden w-[250px] shrink-0 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm lg:block">
          <div className="mb-4 flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5">
            <DashboardLogo className="block h-12 w-auto shrink-0 object-contain" />
            <div>
              <p className="text-sm font-bold text-slate-900">Learner Portal</p>
            </div>
          </div>
          {sidebarItems.map((item) => {
            const Icon = item.icon;
            const isActive = active === item.label;
            return (
              <button
                key={item.label}
                type="button"
                onClick={() => {
                  if (item.label === 'Logout') {
                    onLogout();
                    return;
                  }
                  activateSection(item.label);
                }}
                className={`mb-1.5 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm ${isActive ? 'bg-primary-600 text-white' : 'text-slate-700 hover:bg-slate-100'}`}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </button>
            );
          })}
        </aside>
        <div className="min-w-0 flex-1 space-y-4">
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-3">
              <DashboardLogo className="block h-10 w-auto shrink-0 object-contain" />
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Learner Portal</p>
                <h2 className="mt-1 text-xl font-bold text-slate-900">{learnerName}</h2>
              </div>
            </div>
            <p className="text-sm text-slate-600">Your school learning workspace with real-time teacher updates.</p>
          </div>
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex flex-wrap items-center gap-3">
              <div className="relative min-w-[220px] flex-1">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search subjects, tasks, notes..." className="h-10 rounded-xl bg-slate-50 pl-9 text-sm" />
              </div>
            </div>
          </div>

          {successMessage ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{successMessage}</div> : null}

          {(active === 'Dashboard' || active === 'Progress') ? (
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {cards.slice(0, 6).map((card) => (
                <button
                  key={card.label}
                  type="button"
                  onClick={() => activateSection(card.section)}
                  className="rounded-3xl border border-slate-200 bg-white p-4 text-left shadow-sm transition hover:border-primary-300"
                >
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{card.label}</p>
                    <span className="inline-flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 text-white">{card.icon}</span>
                  </div>
                  <p className="mt-2 text-2xl font-bold text-slate-900">{card.value}</p>
                </button>
              ))}
            </div>
          ) : null}

          {(active === 'Dashboard' || active === 'Assignments & SBA' || active === 'Exams & Quizzes') ? (
            <div className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
              <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
                <h3 className="text-sm font-semibold text-slate-900">Today&apos;s Tasks</h3>
                <div className="mt-3 space-y-2">
                  {todayItems.map((item) => (
                    <div key={getTaskLikeId(item)} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                    <p className="text-xs text-slate-500">{item.taskType} | Due {new Date(item.dueAt).toLocaleTimeString()}</p>
                    </div>
                  ))}
                  {!todayItems.length ? <EmptyState title="No tasks due today" message="You are up to date for today." /> : null}
                </div>
              </div>
              <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
                <h3 className="text-sm font-semibold text-slate-900">Recent Submission Updates</h3>
                <div className="mt-3 space-y-2">
                  {[...submissions.slice(0, 4), ...marks.slice(0, 2)].slice(0, 6).map((item) => (
                    <div key={item.submissionId} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-sm font-medium text-slate-800">{item.status}</p>
                      <p className="text-xs text-slate-500">{item.feedback || 'Feedback pending release'}</p>
                    </div>
                  ))}
                  {!submissions.length && !marks.length ? <p className="text-sm text-slate-500">No activity yet.</p> : null}
                </div>
              </div>
            </div>
          ) : null}

          {(active === 'Dashboard' || active === 'My Subjects') ? (
            <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
              <h3 className="text-sm font-semibold text-slate-900">Subject Cards</h3>
              <div className="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {filteredSubjects.map((subject) => (
                  <div key={subject.subjectId} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                    <p className="text-sm font-semibold text-slate-900">{subject.subjectName}</p>
                    <p className="text-xs text-slate-500">Teacher: {subject.teacherName}</p>
                    <div className="mt-2 h-2 rounded-full bg-slate-200">
                      <div className="h-2 rounded-full bg-primary-600" style={{ width: `${subject.progress}%` }} />
                    </div>
                    <p className="mt-2 text-xs text-slate-600">Latest task: {subject.latestTaskTitle || 'No task yet'}</p>
                    <p className="text-xs text-slate-600">Latest note: {subject.latestNoteTitle || 'No note yet'}</p>
                  </div>
                ))}
                {!filteredSubjects.length ? <EmptyState title="No assigned subjects" message="Subjects will appear once your school enrolls you." /> : null}
              </div>
            </div>
          ) : null}

          {(active === 'Dashboard' || active === 'Assignments & SBA') ? (
            <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="text-sm font-semibold text-slate-900">Assignments & SBA Center</h3>
                <div className="flex gap-2">
                  {(['Pending', 'Submitted', 'Marked', 'Overdue'] as const).map((item) => (
                    <button key={item} type="button" className={`rounded-xl px-3 py-1.5 text-xs font-semibold ${statusFilter === item ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-700'}`} onClick={() => setStatusFilter(item)}>
                      {item}
                    </button>
                  ))}
                </div>
              </div>
              <div className="mt-3 grid gap-3 lg:grid-cols-[1fr_0.9fr]">
                <div className="space-y-2">
                  {visibleTasks.map((task) => (
                    <div key={task.taskId} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-sm font-semibold text-slate-900">{task.title}</p>
                      <p className="text-xs text-slate-500">{task.taskType} - Due {new Date(task.dueAt).toLocaleString()}</p>
                    </div>
                  ))}
                  {!visibleTasks.length ? <p className="text-sm text-slate-500">No tasks in this state.</p> : null}
                </div>
                <div className="rounded-xl border border-slate-200 bg-white p-3">
                  <p className="text-sm font-semibold text-slate-900">Upload Submission</p>
                  <Button className="mt-2 h-10 rounded-xl px-4 text-sm" onClick={() => setShowSubmitModal(true)} disabled={!tasks.length}>
                    <Upload className="mr-2 h-4 w-4" />
                    Upload Submission
                  </Button>
                </div>
              </div>
            </div>
          ) : null}

          {(active === 'Dashboard' || active === 'Notes & Resources') ? (
            <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
              <h3 className="text-sm font-semibold text-slate-900">Notes & Resources</h3>
              <div className="mt-3 space-y-2">
                {notes.map((note, index) => (
                  <div key={note.id ?? `note-${index}`} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                    <p className="text-sm font-semibold text-slate-900">{note.title || 'Untitled note'}</p>
                    <p className="mt-1 text-xs text-slate-600">{note.noteText || 'No note text provided.'}</p>
                    <div className="mt-2 flex gap-2">
                      {note.pdfUrl ? <a className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-700" href={note.pdfUrl} target="_blank" rel="noreferrer"><FileDown className="mr-1 h-3.5 w-3.5" />View PDF</a> : null}
                    </div>
                  </div>
                ))}
                {!notes.length ? <EmptyState title="No resources yet" message="Teacher notes and PDFs will appear here." /> : null}
              </div>
            </div>
          ) : null}

          {(active === 'Dashboard' || active === 'My Submissions') ? (
            <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
              <h3 className="text-sm font-semibold text-slate-900">My Submissions</h3>
              <div className="mt-3">
                <DataTable
                  columns={[
                    { key: 'taskId', header: 'Task', render: (row) => tasks.find((task) => task.taskId === row.taskId)?.title || row.taskId },
                    { key: 'status', header: 'Status' },
                    { key: 'late', header: 'Late', render: (row) => row.late ? 'Yes' : 'No' },
                    { key: 'submissionText', header: 'Submission', render: (row) => row.submissionText || (row.fileUrl ? 'File submitted' : 'No text provided') },
                    { key: 'fileUrl', header: 'Attachment', render: (row) => row.fileUrl ? <a href={row.fileUrl} target="_blank" rel="noreferrer" className="text-primary-700 underline">Open file</a> : 'None' },
                  ]}
                  data={submissions.map((row) => ({ ...row, id: row.submissionId }))}
                />
                {!submissions.length ? <div className="mt-3"><EmptyState title="No submissions yet" message="Submit an assignment to see it here." /></div> : null}
              </div>
            </div>
          ) : null}

          {(active === 'Dashboard' || active === 'Marks & Feedback') ? (
            <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
              <h3 className="text-sm font-semibold text-slate-900">Marks & Feedback</h3>
              <div className="mt-3">
                <DataTable
                  columns={[
                    { key: 'taskId', header: 'Task', render: (row) => tasks.find((task) => task.taskId === row.taskId)?.title || row.taskId },
                    { key: 'status', header: 'Status' },
                    { key: 'marks', header: 'Marks' },
                    { key: 'feedback', header: 'Teacher Feedback' },
                  ]}
                  data={marks.map((row) => ({ ...row, id: row.submissionId }))}
                />
                {!marks.length ? <div className="mt-3"><EmptyState title="No marks released yet" message="Teacher feedback will appear after marking." /></div> : null}
              </div>
            </div>
          ) : null}

          {(active === 'Dashboard' || active === 'Progress') ? (
            <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
              <h3 className="text-sm font-semibold text-slate-900">Progress Analytics</h3>
              <div className="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">Completed tasks: <span className="font-semibold">{progress.submitted}</span></div>
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">Missing tasks: <span className="font-semibold">{progress.missing}</span></div>
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">Late submissions: <span className="font-semibold">{progress.late}</span></div>
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">Completion rate: <span className="font-semibold">{progressScore}%</span></div>
              </div>
            </div>
          ) : null}

        </div>
      </div>

      {showSubmitModal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4">
          <div className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-4 shadow-xl">
            <h3 className="text-base font-semibold text-slate-900">Upload Task Submission</h3>
            <select className="mt-3 h-10 w-full rounded-xl border border-slate-200 px-3 text-sm" value={taskId} onChange={(e) => setTaskId(e.target.value)}>
              <option value="">Select task</option>
              {tasks.map((task) => <option key={task.taskId} value={task.taskId}>{task.title}</option>)}
            </select>
            <textarea className="mt-2 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" rows={5} value={submissionText} onChange={(e) => setSubmissionText(e.target.value)} placeholder="Write your answer..." />
            <Input className="mt-2 h-10 rounded-xl text-sm" value={fileUrl} onChange={(e) => setFileUrl(e.target.value)} placeholder="Optional file URL" />
            <div className="mt-3 flex justify-end gap-2">
              <Button className="h-10 rounded-xl bg-slate-600 px-4 text-sm hover:bg-slate-700" onClick={() => setShowSubmitModal(false)}>Cancel</Button>
              <Button
                className="h-10 rounded-xl px-4 text-sm"
                onClick={async () => {
                  await submit();
                  setShowSubmitModal(false);
                }}
                disabled={submitting || !taskId}
              >
                {submitting ? 'Submitting...' : 'Submit Work'}
              </Button>
            </div>
          </div>
        </div>
      ) : null}

    </section>
  );
};
