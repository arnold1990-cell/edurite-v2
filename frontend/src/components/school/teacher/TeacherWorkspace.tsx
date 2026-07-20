import { useMemo, useState, type ComponentType } from 'react';
import {
  BookOpen,
  Calendar,
  ClipboardCheck,
  GraduationCap,
  LayoutDashboard,
  LineChart,
  LogOut,
  Settings,
  Users,
} from 'lucide-react';
import { DashboardLogo } from '@/components/common/DashboardLogo';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { DataTable } from '@/components/tables/DataTable';
import { EmptyState } from '@/components/feedback/States';
import type {
  SchoolDashboard,
  SchoolTask,
  SubmissionView,
  TeacherAnalytics,
  TeacherCalendarItem,
  TeacherClassView,
  TeacherSubjectView,
} from '@/services/schoolService';

type SidebarLabel =
  | 'Dashboard'
  | 'My Classes'
  | 'Subjects'
  | 'Assignments & SBA'
  | 'Notes & Resources'
  | 'Exams & Assessments'
  | 'Learner Progress'
  | 'Analytics'
  | 'Calendar'
  | 'Settings'
  | 'Logout';

const sidebarItems: Array<{ label: SidebarLabel; icon: ComponentType<{ className?: string }> }> = [
  { label: 'Dashboard', icon: LayoutDashboard },
  { label: 'My Classes', icon: Users },
  { label: 'Subjects', icon: BookOpen },
  { label: 'Assignments & SBA', icon: ClipboardCheck },
  { label: 'Notes & Resources', icon: BookOpen },
  { label: 'Exams & Assessments', icon: GraduationCap },
  { label: 'Learner Progress', icon: LineChart },
  { label: 'Analytics', icon: LineChart },
  { label: 'Calendar', icon: Calendar },
  { label: 'Settings', icon: Settings },
  { label: 'Logout', icon: LogOut },
];

export type TeacherWorkspaceProps = {
  activeSection: SidebarLabel;
  setActiveSection: (section: SidebarLabel) => void;
  dashboard: SchoolDashboard;
  analytics?: TeacherAnalytics | null;
  tasks: SchoolTask[];
  submissions: SubmissionView[];
  classes: TeacherClassView[];
  subjects: TeacherSubjectView[];
  assessments: SchoolTask[];
  calendarItems: TeacherCalendarItem[];
  selectedTaskId: string;
  setSelectedTaskId: (value: string) => void;
  classId: string;
  setClassId: (value: string) => void;
  subjectId: string;
  setSubjectId: (value: string) => void;
  title: string;
  setTitle: (value: string) => void;
  instructions: string;
  setInstructions: (value: string) => void;
  dueAt: string;
  setDueAt: (value: string) => void;
  onCreateTask: () => void;
  onCreateNote: (payload: { title: string; noteText?: string }) => void;
  onMarkSubmission: (submissionId: string, payload: { marksAwarded: number; comments?: string; released: boolean }) => void;
  onLogout: () => void;
  creatingTask: boolean;
  creatingNote: boolean;
  markingSubmissionId?: string | null;
};

export const TeacherWorkspace = ({
  activeSection,
  setActiveSection,
  dashboard,
  analytics,
  tasks,
  submissions,
  classes,
  subjects,
  assessments,
  calendarItems,
  selectedTaskId,
  setSelectedTaskId,
  classId,
  setClassId,
  subjectId,
  setSubjectId,
  title,
  setTitle,
  instructions,
  setInstructions,
  dueAt,
  setDueAt,
  onCreateTask,
  onCreateNote,
  onMarkSubmission,
  onLogout,
  creatingTask,
  creatingNote,
  markingSubmissionId,
}: TeacherWorkspaceProps) => {
  const [taskFilter, setTaskFilter] = useState('');
  const [noteTitle, setNoteTitle] = useState('');
  const [noteContent, setNoteContent] = useState('');
  const [markForm, setMarkForm] = useState<Record<string, { marks: string; comments: string; released: boolean }>>({});

  const filteredTasks = useMemo(() => {
    const q = taskFilter.trim().toLowerCase();
    if (!q) return tasks;
    return tasks.filter((task) => task.title.toLowerCase().includes(q));
  }, [taskFilter, tasks]);

  const pendingMarking = submissions.filter((item) => !item.released).length;
  const dueSoon = tasks.filter((item) => new Date(item.dueAt).getTime() > Date.now()).length;

  const renderDashboard = () => (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">Classes</p><p className="mt-1 text-2xl font-bold text-slate-900">{dashboard.totalClasses}</p></article>
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">Subjects</p><p className="mt-1 text-2xl font-bold text-slate-900">{dashboard.totalSubjects}</p></article>
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">Pending Marking</p><p className="mt-1 text-2xl font-bold text-slate-900">{pendingMarking}</p></article>
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">Tasks Due</p><p className="mt-1 text-2xl font-bold text-slate-900">{dueSoon}</p></article>
    </div>
  );

  const renderClasses = () => (
    <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="text-sm font-semibold text-slate-900">My Classes</h3>
      <div className="mt-3 space-y-2">
        {classes.map((cls) => (
          <div key={cls.classId} className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
            <p className="font-semibold text-slate-900">{cls.grade} {cls.className}</p>
            <p>{cls.subjectName} · {cls.learnerCount} learners</p>
          </div>
        ))}
        {!classes.length ? <EmptyState title="No assigned classes yet." message="No assigned classes yet." /> : null}
      </div>
    </div>
  );

  const renderSubjects = () => (
    <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="text-sm font-semibold text-slate-900">My Subjects</h3>
      <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {subjects.map((subject) => (
          <div key={subject.subjectId} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
            <p className="text-sm font-semibold text-slate-900">{subject.subjectName}</p>
            <p className="text-xs text-slate-600">{subject.phase}{subject.grade ? ` · ${subject.grade}` : ''}</p>
            <p className="mt-1 text-xs text-slate-500">{subject.classCount} assigned classes</p>
          </div>
        ))}
      </div>
      {!subjects.length ? <div className="mt-3"><EmptyState title="No assigned subjects yet." message="No assigned subjects yet." /></div> : null}
    </div>
  );

  const renderAssignments = () => (
    <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
      <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
        <h3 className="text-sm font-semibold text-slate-900">Create Assignment / SBA / Assessment</h3>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          <select className="h-10 rounded-xl border border-slate-200 px-3 text-sm" value={classId} onChange={(e) => setClassId(e.target.value)}>
            <option value="">Select class</option>
            {classes.map((cls) => <option key={cls.classId} value={cls.classId}>{cls.grade} {cls.className}</option>)}
          </select>
          <select className="h-10 rounded-xl border border-slate-200 px-3 text-sm" value={subjectId} onChange={(e) => setSubjectId(e.target.value)}>
            <option value="">Select subject</option>
            {subjects.map((subject) => <option key={subject.subjectId} value={subject.subjectId}>{subject.subjectName}</option>)}
          </select>
          <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Task title" className="h-10 rounded-xl text-sm sm:col-span-2" />
          <Input value={instructions} onChange={(e) => setInstructions(e.target.value)} placeholder="Instructions" className="h-10 rounded-xl text-sm sm:col-span-2" />
          <Input type="datetime-local" value={dueAt} onChange={(e) => setDueAt(e.target.value)} className="h-10 rounded-xl text-sm sm:col-span-2" />
        </div>
        <div className="mt-3 flex flex-wrap gap-2">
          <Button onClick={onCreateTask} disabled={creatingTask || !classId || !subjectId || !title.trim() || !dueAt} className="h-10 rounded-xl px-4 text-sm">
            {creatingTask ? 'Creating...' : 'Create Task'}
          </Button>
        </div>
      </div>
      <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex items-center justify-between gap-2">
          <h3 className="text-sm font-semibold text-slate-900">My Tasks</h3>
          <Input value={taskFilter} onChange={(e) => setTaskFilter(e.target.value)} placeholder="Filter tasks" className="h-9 w-44 rounded-xl text-xs" />
        </div>
        <div className="mt-3 space-y-2">
          {filteredTasks.map((task) => (
            <div key={task.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
              <p className="text-sm font-semibold text-slate-900">{task.title}</p>
              <p className="text-xs text-slate-600">{task.taskType} · Due {new Date(task.dueAt).toLocaleString()}</p>
            </div>
          ))}
          {!filteredTasks.length ? <EmptyState title="No tasks found." message="No tasks found." /> : null}
        </div>
      </div>
    </div>
  );

  const renderNotes = () => (
    <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="text-sm font-semibold text-slate-900">Upload Notes & Resources</h3>
      <p className="mt-1 text-xs text-slate-500">This saves note metadata via the teacher notes endpoint.</p>
      <div className="mt-3 grid gap-2 sm:grid-cols-2">
        <select className="h-10 rounded-xl border border-slate-200 px-3 text-sm" value={classId} onChange={(e) => setClassId(e.target.value)}>
          <option value="">Select class</option>
          {classes.map((cls) => <option key={cls.classId} value={cls.classId}>{cls.grade} {cls.className}</option>)}
        </select>
        <select className="h-10 rounded-xl border border-slate-200 px-3 text-sm" value={subjectId} onChange={(e) => setSubjectId(e.target.value)}>
          <option value="">Select subject</option>
          {subjects.map((subject) => <option key={subject.subjectId} value={subject.subjectId}>{subject.subjectName}</option>)}
        </select>
        <Input value={noteTitle} onChange={(e) => setNoteTitle(e.target.value)} placeholder="Note title" className="h-10 rounded-xl text-sm sm:col-span-2" />
        <Input value={noteContent} onChange={(e) => setNoteContent(e.target.value)} placeholder="Note content" className="h-10 rounded-xl text-sm sm:col-span-2" />
      </div>
      <div className="mt-3">
        <Button onClick={() => onCreateNote({ title: noteTitle, noteText: noteContent })} disabled={creatingNote || !classId || !subjectId || !noteTitle.trim()} className="h-10 rounded-xl px-4 text-sm">
          {creatingNote ? 'Publishing...' : 'Publish Note'}
        </Button>
      </div>
    </div>
  );

  const renderAssessments = () => (
    <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="text-sm font-semibold text-slate-900">Exams & Assessments</h3>
      <div className="mt-3 space-y-2">
        {assessments.map((assessment) => (
          <div key={assessment.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
            <p className="text-sm font-semibold text-slate-900">{assessment.title}</p>
            <p className="text-xs text-slate-500">{assessment.taskType} · Due {new Date(assessment.dueAt).toLocaleString()}</p>
          </div>
        ))}
      </div>
      {!assessments.length ? <div className="mt-3"><EmptyState title="No assessments created yet." message="No assessments created yet." /></div> : null}
    </div>
  );

  const renderProgress = () => (
    <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-sm font-semibold text-slate-900">Learner Submissions & Marks</h3>
        <select className="h-9 rounded-xl border border-slate-200 px-3 text-sm" value={selectedTaskId} onChange={(e) => setSelectedTaskId(e.target.value)}>
          <option value="">All tasks</option>
          {tasks.map((task) => <option key={task.id} value={task.id}>{task.title}</option>)}
        </select>
      </div>
      <DataTable<any>
        columns={[
          { key: 'learnerName', header: 'Learner' },
          { key: 'status', header: 'Status' },
          { key: 'late', header: 'Late', render: (row) => (row.late ? 'Yes' : 'No') },
          { key: 'similarity', header: 'Similarity %' },
          { key: 'marks', header: 'Marks' },
          {
            key: 'actions',
            header: 'Actions',
            render: (row) => {
              const current = markForm[row.submissionId] ?? { marks: row.marks ? String(row.marks) : '', comments: row.feedback ?? '', released: Boolean(row.released) };
              return (
                <div className="flex flex-wrap items-center gap-2">
                  <input
                    className="h-8 w-20 rounded-md border border-slate-200 px-2 text-xs"
                    type="number"
                    min={0}
                    value={current.marks}
                    onChange={(e) => setMarkForm((prev) => ({ ...prev, [row.submissionId]: { ...current, marks: e.target.value } }))}
                    placeholder="Marks"
                  />
                  <input
                    className="h-8 w-36 rounded-md border border-slate-200 px-2 text-xs"
                    value={current.comments}
                    onChange={(e) => setMarkForm((prev) => ({ ...prev, [row.submissionId]: { ...current, comments: e.target.value } }))}
                    placeholder="Comment"
                  />
                  <label className="inline-flex items-center gap-1 text-xs text-slate-600">
                    <input
                      type="checkbox"
                      checked={current.released}
                      onChange={(e) => setMarkForm((prev) => ({ ...prev, [row.submissionId]: { ...current, released: e.target.checked } }))}
                    />
                    Release
                  </label>
                  <Button
                    type="button"
                    className="h-8 rounded-lg px-2 text-xs"
                    disabled={markingSubmissionId === row.submissionId || current.marks.trim() === ''}
                    onClick={() => onMarkSubmission(row.submissionId, { marksAwarded: Number(current.marks), comments: current.comments, released: current.released })}
                  >
                    {markingSubmissionId === row.submissionId ? 'Saving...' : 'Save'}
                  </Button>
                </div>
              );
            },
          },
        ]}
        data={submissions.map((row) => ({ ...row, id: row.submissionId }))}
      />
      {!submissions.length ? <div className="mt-3"><EmptyState title="No submissions yet." message="No submissions yet." /></div> : null}
    </div>
  );

  const renderAnalytics = () => (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">Pending Marking</p><p className="mt-1 text-2xl font-bold text-slate-900">{analytics?.pendingMarking ?? pendingMarking}</p></article>
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">SBA Tasks Due</p><p className="mt-1 text-2xl font-bold text-slate-900">{analytics?.sbaTasksDue ?? dueSoon}</p></article>
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">Learner Submissions</p><p className="mt-1 text-2xl font-bold text-slate-900">{analytics?.learnerSubmissions ?? submissions.length}</p></article>
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">Average Performance</p><p className="mt-1 text-2xl font-bold text-slate-900">{Math.round(analytics?.averageClassPerformance ?? 0)}%</p></article>
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">Attendance Rate</p><p className="mt-1 text-2xl font-bold text-slate-900">{Math.round(analytics?.attendanceRate ?? 0)}%</p></article>
      <article className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs uppercase tracking-[0.14em] text-slate-500">Upcoming Assessments</p><p className="mt-1 text-2xl font-bold text-slate-900">{analytics?.upcomingAssessments ?? assessments.length}</p></article>
    </div>
  );

  const renderCalendar = () => (
    <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="text-sm font-semibold text-slate-900">Calendar</h3>
      <div className="mt-3 space-y-2">
        {calendarItems.map((item, idx) => (
          <div key={`${item.title}-${idx}`} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
            <p className="text-xs uppercase tracking-[0.12em] text-slate-500">{item.category}</p>
            <p className="text-sm font-semibold text-slate-900">{item.title}</p>
            <p className="text-xs text-slate-500">{new Date(item.dueAt).toLocaleString()}</p>
          </div>
        ))}
      </div>
      {!calendarItems.length ? <div className="mt-3"><EmptyState title="No calendar items yet." message="No calendar items yet." /></div> : null}
    </div>
  );

  const renderSettings = () => (
    <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="text-sm font-semibold text-slate-900">Settings</h3>
      <p className="mt-2 text-sm text-slate-600">Use account settings to update profile and password.</p>
      <div className="mt-3">
        <Button type="button" className="h-10 rounded-xl bg-primary-700 px-4 text-sm hover:bg-primary-700" onClick={onLogout}>Logout</Button>
      </div>
    </div>
  );

  return (
    <section className="space-y-4 overflow-x-hidden">
      <div className="flex min-w-0 gap-4">
        <aside className="hidden w-[250px] shrink-0 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm lg:block">
          <div className="mb-4 flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5">
            <DashboardLogo className="block h-12 w-auto shrink-0 object-contain" />
            <div>
              <p className="text-sm font-bold text-slate-900">Teacher Portal</p>
            </div>
          </div>
          <div className="space-y-1.5">
            {sidebarItems.map((item) => {
              const Icon = item.icon;
              const active = activeSection === item.label;
              return (
                <button
                  key={item.label}
                  type="button"
                  onClick={() => (item.label === 'Logout' ? onLogout() : setActiveSection(item.label))}
                  className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition ${active ? 'bg-primary-600 text-white' : 'text-slate-700 hover:bg-slate-100'}`}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </div>
        </aside>

        <div className="min-w-0 flex-1 space-y-4 overflow-x-hidden">
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-3">
              <DashboardLogo className="block h-10 w-auto shrink-0 object-contain" />
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Teacher Portal</p>
                <h2 className="text-xl font-bold text-slate-900">Classroom workspace</h2>
              </div>
            </div>
          </div>
          {activeSection === 'Dashboard' ? renderDashboard() : null}
          {activeSection === 'My Classes' ? renderClasses() : null}
          {activeSection === 'Subjects' ? renderSubjects() : null}
          {activeSection === 'Assignments & SBA' ? renderAssignments() : null}
          {activeSection === 'Notes & Resources' ? renderNotes() : null}
          {activeSection === 'Exams & Assessments' ? renderAssessments() : null}
          {activeSection === 'Learner Progress' ? renderProgress() : null}
          {activeSection === 'Analytics' ? renderAnalytics() : null}
          {activeSection === 'Calendar' ? renderCalendar() : null}
          {activeSection === 'Settings' ? renderSettings() : null}
        </div>
      </div>
    </section>
  );
};



