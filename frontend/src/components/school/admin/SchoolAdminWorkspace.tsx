import { useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AdminSidebar } from './workspace/AdminSidebar';
import { AdminTopbar } from './workspace/AdminTopbar';
import type { Feedback, SchoolAdminSummary } from './workspace/types';
import { useAppQuery } from '@/hooks/useAppQuery';
import { schoolService } from '@/services/schoolService';
import { notificationService } from '@/services/notificationService';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { SubjectManagementPanel } from '@/components/school/admin/SubjectManagementPanel';
import { TeacherManagementPanel } from '@/components/school/admin/TeacherManagementPanel';

type Props = {
  fullName?: string;
  activeSection: string;
  setActiveSection: (section: string) => void;
  search: string;
  setSearch: (value: string) => void;
  feedback: Feedback;
  summary: SchoolAdminSummary;
  grade: string;
  className: string;
  academicYear: number;
  subjectName: string;
  phase: string;
  setGrade: (value: string) => void;
  setClassName: (value: string) => void;
  setAcademicYear: (value: number) => void;
  setSubjectName: (value: string) => void;
  setPhase: (value: string) => void;
  onCreateClass: () => void;
  onCreateSubject: () => void;
  creatingClass: boolean;
  creatingSubject: boolean;
  onLogout: () => void;
};

export const SchoolAdminWorkspace = ({
  fullName,
  activeSection,
  setActiveSection,
  search,
  setSearch,
  feedback,
  summary,
  grade,
  className,
  academicYear,
  subjectName,
  phase,
  setGrade,
  setClassName,
  setAcademicYear,
  setSubjectName,
  setPhase,
  onCreateClass,
  onCreateSubject,
  creatingClass,
  creatingSubject,
  onLogout,
}: Props) => {
  const queryClient = useQueryClient();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [localMessage, setLocalMessage] = useState<Feedback>(null);
  const [teacherForm, setTeacherForm] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [learnerForm, setLearnerForm] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [editUser, setEditUser] = useState<{ userId: string; firstName: string; lastName: string; email: string } | null>(null);
  const [editClass, setEditClass] = useState<{ id: string; grade: string; className: string; academicYear: number; term?: string; active: boolean } | null>(null);
  const [editSubject, setEditSubject] = useState<{ id: string; subjectName: string; phase: string; grade?: string; active: boolean } | null>(null);
  const [assignmentForm, setAssignmentForm] = useState({ teacherUserId: '', classId: '', subjectId: '' });
  const [enrollmentForm, setEnrollmentForm] = useState({ learnerUserId: '', classId: '', subjectId: '' });
  const [profileForm, setProfileForm] = useState({ schoolName: '', registrationNumber: '', district: '', province: '', contactEmail: '', contactPhone: '', address: '' });
  const loadTeachers = ['Teachers', 'Assignments', 'Dashboard'].includes(activeSection);
  const loadLearners = ['Learners', 'Settings', 'Dashboard'].includes(activeSection);
  const loadClasses = ['Classes', 'Assignments', 'Learners', 'Dashboard'].includes(activeSection);
  const loadSubjects = ['Subjects', 'Assignments', 'Learners', 'Dashboard'].includes(activeSection);
  const loadTeacherAssignments = ['Assignments', 'Settings', 'Dashboard'].includes(activeSection);
  const loadLearnerEnrollments = ['Learners', 'Settings'].includes(activeSection);
  const loadAssessments = ['Assessments', 'Dashboard'].includes(activeSection);
  const loadSubmissions = ['Results', 'Reports', 'Dashboard'].includes(activeSection);
  const loadResults = ['Results', 'Dashboard'].includes(activeSection);
  const loadNotes = ['Settings', 'Dashboard'].includes(activeSection);
  const loadInbox = ['Notifications', 'Dashboard'].includes(activeSection);
  const moduleQueryConfig = { staleTime: 60000, retry: 1, refetchOnWindowFocus: false } as const;

  const teachers = useAppQuery({ queryKey: ['school', 'admin', 'teachers'], queryFn: schoolService.listTeachers, enabled: loadTeachers, ...moduleQueryConfig });
  const learners = useAppQuery({ queryKey: ['school', 'admin', 'learners'], queryFn: schoolService.listLearners, enabled: loadLearners, ...moduleQueryConfig });
  const classes = useAppQuery({ queryKey: ['school', 'admin', 'classes'], queryFn: schoolService.listClasses, enabled: loadClasses, ...moduleQueryConfig });
  const subjects = useAppQuery({ queryKey: ['school', 'admin', 'subjects'], queryFn: schoolService.listSubjects, enabled: loadSubjects, ...moduleQueryConfig });
  const teacherAssignments = useAppQuery({ queryKey: ['school', 'admin', 'teacher-assignments'], queryFn: schoolService.listTeacherAssignments, enabled: loadTeacherAssignments, ...moduleQueryConfig });
  const learnerEnrollments = useAppQuery({ queryKey: ['school', 'admin', 'learner-enrollments'], queryFn: schoolService.listLearnerEnrollments, enabled: loadLearnerEnrollments, ...moduleQueryConfig });
  const assessments = useAppQuery({ queryKey: ['school', 'admin', 'assessments'], queryFn: schoolService.schoolAssessments, enabled: loadAssessments, ...moduleQueryConfig });
  const submissions = useAppQuery({ queryKey: ['school', 'admin', 'submissions'], queryFn: schoolService.schoolSubmissions, enabled: loadSubmissions, ...moduleQueryConfig });
  const results = useAppQuery({ queryKey: ['school', 'admin', 'results'], queryFn: schoolService.schoolResults, enabled: loadResults, ...moduleQueryConfig });
  const notes = useAppQuery({ queryKey: ['school', 'admin', 'notes'], queryFn: schoolService.schoolNotes, enabled: loadNotes, ...moduleQueryConfig });
  const inbox = useAppQuery({ queryKey: ['school', 'admin', 'inbox'], queryFn: () => notificationService.mine({ page: 0, size: 30 }), enabled: loadInbox, ...moduleQueryConfig });

  const searchText = search.trim().toLowerCase();
  const filteredTeachers = useMemo(() => (teachers.data ?? []).filter((u) => `${u.fullName} ${u.email}`.toLowerCase().includes(searchText)), [teachers.data, searchText]);
  const filteredLearners = useMemo(() => (learners.data ?? []).filter((u) => `${u.fullName} ${u.email}`.toLowerCase().includes(searchText)), [learners.data, searchText]);
  const filteredClasses = useMemo(() => (classes.data ?? []).filter((c) => `${c.grade} ${c.className} ${c.academicYear}`.toLowerCase().includes(searchText)), [classes.data, searchText]);
  const filteredSubjects = useMemo(() => (subjects.data ?? []).filter((s) => `${s.subjectName} ${s.phase} ${s.grade ?? ''}`.toLowerCase().includes(searchText)), [subjects.data, searchText]);
  const teacherNameById = useMemo(() => Object.fromEntries((teachers.data ?? []).map((t) => [t.userId, t.fullName])), [teachers.data]);
  const learnerNameById = useMemo(() => Object.fromEntries((learners.data ?? []).map((l) => [l.userId, l.fullName])), [learners.data]);
  const classLabelById = useMemo(() => Object.fromEntries((classes.data ?? []).map((c) => [c.id, `${c.grade} ${c.className}`])), [classes.data]);
  const subjectLabelById = useMemo(() => Object.fromEntries((subjects.data ?? []).map((s) => [s.id, s.subjectName])), [subjects.data]);

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'teachers'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'learners'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'classes'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'subjects'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'teacher-assignments'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'learner-enrollments'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'tasks'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'assessments'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'submissions'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'results'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'notes'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'inbox'] }),
    ]);
  };

  const addTeacher = useMutation({
    mutationFn: () => schoolService.createSchoolUser({ ...teacherForm, roleName: 'ROLE_TEACHER' }),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'Teacher created.' });
      setTeacherForm({ firstName: '', lastName: '', email: '', password: '' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not create teacher.' }),
  });
  const addLearner = useMutation({
    mutationFn: () => schoolService.createSchoolUser({ ...learnerForm, roleName: 'ROLE_SCHOOL_STUDENT' }),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'Learner created.' });
      setLearnerForm({ firstName: '', lastName: '', email: '', password: '' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not create learner.' }),
  });
  const updateUser = useMutation({
    mutationFn: (payload: { userId: string; firstName: string; lastName: string; email: string }) => schoolService.updateSchoolUser(payload.userId, { firstName: payload.firstName, lastName: payload.lastName, email: payload.email }),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'User updated.' });
      setEditUser(null);
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not update user.' }),
  });
  const deactivateUser = useMutation({
    mutationFn: (userId: string) => schoolService.deactivateSchoolUser(userId),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'User deactivated.' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not deactivate user.' }),
  });
  const updateClass = useMutation({
    mutationFn: (payload: { id: string; grade: string; className: string; academicYear: number; term?: string; active: boolean }) => schoolService.updateClass(payload.id, payload),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'Class updated.' });
      setEditClass(null);
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not update class.' }),
  });
  const deactivateClass = useMutation({
    mutationFn: (classId: string) => schoolService.deactivateClass(classId),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'Class deactivated.' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not deactivate class.' }),
  });
  const updateSubject = useMutation({
    mutationFn: (payload: { id: string; subjectName: string; phase: string; grade?: string; active: boolean }) => schoolService.updateSubject(payload.id, payload),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'Subject updated.' });
      setEditSubject(null);
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not update subject.' }),
  });
  const deactivateSubject = useMutation({
    mutationFn: (subjectId: string) => schoolService.deactivateSubject(subjectId),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'Subject deactivated.' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not deactivate subject.' }),
  });
  const assignTeacher = useMutation({
    mutationFn: () => schoolService.assignTeacher(assignmentForm),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'Teacher assignment created.' });
      setAssignmentForm({ teacherUserId: '', classId: '', subjectId: '' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not assign teacher.' }),
  });
  const enrollLearner = useMutation({
    mutationFn: () => schoolService.enrollLearner(enrollmentForm),
    onSuccess: async () => {
      setLocalMessage({ type: 'success', message: 'Learner enrollment created.' });
      setEnrollmentForm({ learnerUserId: '', classId: '', subjectId: '' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not enroll learner.' }),
  });
  const saveProfile = useMutation({
    mutationFn: () => schoolService.upsertSchoolProfile(profileForm),
    onSuccess: () => setLocalMessage({ type: 'success', message: 'School profile saved.' }),
    onError: (error) => setLocalMessage({ type: 'error', message: error instanceof Error ? error.message : 'Could not save profile.' }),
  });
  const markNotificationRead = useMutation({
    mutationFn: (id: string) => notificationService.markRead(id),
    onSuccess: async () => { await refresh(); },
  });
  const markAllNotificationsRead = useMutation({
    mutationFn: () => notificationService.markAllRead(),
    onSuccess: async () => { await refresh(); },
  });

  const userModule = (label: 'Teachers' | 'Learners') => {
    const isTeacher = label === 'Teachers';
    const list = isTeacher ? filteredTeachers : filteredLearners;
    const createAction = isTeacher ? addTeacher : addLearner;
    const form = isTeacher ? teacherForm : learnerForm;
    const setForm = isTeacher ? setTeacherForm : setLearnerForm;
    return (
      <div className="grid gap-4 xl:grid-cols-[1.4fr_0.6fr]">
        <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900">{label}</h2>
          {(isTeacher ? teachers.isLoading : learners.isLoading) ? <LoadingState message={`Loading ${label.toLowerCase()}...`} /> : null}
          {(isTeacher ? teachers.isError : learners.isError) ? <ErrorState message={`Could not load ${label.toLowerCase()}.`} /> : null}
          {!((isTeacher ? teachers.isLoading : learners.isLoading) || (isTeacher ? teachers.isError : learners.isError)) ? (
            <div className="mt-3 space-y-2">
              {list.map((item) => (
                <div key={item.userId} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                  {editUser?.userId === item.userId ? (
                    <div className="grid gap-2 sm:grid-cols-3">
                      <Input value={editUser.firstName} onChange={(e) => setEditUser((prev) => prev ? ({ ...prev, firstName: e.target.value }) : prev)} placeholder="First name" />
                      <Input value={editUser.lastName} onChange={(e) => setEditUser((prev) => prev ? ({ ...prev, lastName: e.target.value }) : prev)} placeholder="Last name" />
                      <Input value={editUser.email} onChange={(e) => setEditUser((prev) => prev ? ({ ...prev, email: e.target.value }) : prev)} placeholder="Email" />
                      <div className="sm:col-span-3 flex gap-2">
                        <Button type="button" disabled={updateUser.isPending} onClick={() => void updateUser.mutate(editUser)}>{updateUser.isPending ? 'Saving...' : 'Save'}</Button>
                        <Button type="button" className="bg-primary-600 hover:bg-primary-600" onClick={() => setEditUser(null)}>Cancel</Button>
                      </div>
                    </div>
                  ) : (
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-900">{item.fullName}</p>
                        <p className="text-xs text-slate-600">{item.email}</p>
                      </div>
                      <div className="flex gap-2">
                        <Button type="button" className="bg-primary-600 hover:bg-primary-700" onClick={() => {
                          const [firstName, ...rest] = item.fullName.split(' ');
                          setEditUser({ userId: item.userId, firstName: firstName || '', lastName: rest.join(' '), email: item.email });
                        }}>Edit</Button>
                        <Button type="button" className="bg-red-600 hover:bg-red-700" disabled={deactivateUser.isPending} onClick={() => deactivateUser.mutate(item.userId)}>Deactivate</Button>
                      </div>
                    </div>
                  )}
                </div>
              ))}
              {!list.length ? <EmptyState title={`No ${label.toLowerCase()}`} message={`Create ${label.toLowerCase()} to get started.`} /> : null}
            </div>
          ) : null}
        </div>
        <div className="space-y-4">
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
            <h3 className="text-sm font-semibold text-slate-900">Create {isTeacher ? 'Teacher' : 'Learner'}</h3>
            <Input value={form.firstName} onChange={(e) => setForm((prev) => ({ ...prev, firstName: e.target.value }))} placeholder="First name" />
            <Input value={form.lastName} onChange={(e) => setForm((prev) => ({ ...prev, lastName: e.target.value }))} placeholder="Last name" />
            <Input value={form.email} onChange={(e) => setForm((prev) => ({ ...prev, email: e.target.value }))} placeholder="Email" />
            <Input value={form.password} onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))} placeholder="Password" />
            <Button type="button" disabled={createAction.isPending || !form.firstName.trim() || !form.lastName.trim() || !form.email.trim() || !form.password.trim()} onClick={() => createAction.mutate()}>
              {createAction.isPending ? 'Saving...' : `Create ${isTeacher ? 'Teacher' : 'Learner'}`}
            </Button>
          </div>
          {!isTeacher ? (
            <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
              <h3 className="text-sm font-semibold text-slate-900">Enroll Learner</h3>
              <select className="h-10 w-full rounded-xl border border-slate-200 px-3 text-sm" value={enrollmentForm.learnerUserId} onChange={(e) => setEnrollmentForm((prev) => ({ ...prev, learnerUserId: e.target.value }))}>
                <option value="">Select learner</option>
                {(learners.data ?? []).map((l) => <option key={l.userId} value={l.userId}>{l.fullName}</option>)}
              </select>
              <select className="h-10 w-full rounded-xl border border-slate-200 px-3 text-sm" value={enrollmentForm.classId} onChange={(e) => setEnrollmentForm((prev) => ({ ...prev, classId: e.target.value }))}>
                <option value="">Select class</option>
                {(classes.data ?? []).map((c) => <option key={c.id} value={c.id}>{c.grade} {c.className}</option>)}
              </select>
              <select className="h-10 w-full rounded-xl border border-slate-200 px-3 text-sm" value={enrollmentForm.subjectId} onChange={(e) => setEnrollmentForm((prev) => ({ ...prev, subjectId: e.target.value }))}>
                <option value="">Select subject</option>
                {(subjects.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.subjectName}</option>)}
              </select>
              <Button type="button" disabled={enrollLearner.isPending || !enrollmentForm.learnerUserId || !enrollmentForm.classId || !enrollmentForm.subjectId} onClick={() => enrollLearner.mutate()}>
                {enrollLearner.isPending ? 'Enrolling...' : 'Enroll Learner'}
              </Button>
            </div>
          ) : null}
        </div>
      </div>
    );
  };

  const renderModule = () => {
    if (activeSection === 'Dashboard') {
      return (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {[
            { label: 'Classes', value: summary.totalClasses },
            { label: 'Subjects', value: summary.totalSubjects },
            { label: 'Teachers', value: (teachers.data ?? []).length },
            { label: 'Learners', value: (learners.data ?? []).length },
            { label: 'Assignments', value: (teacherAssignments.data ?? []).length },
            { label: 'Assessments', value: (assessments.data ?? []).length },
            { label: 'Results', value: (results.data ?? []).length },
            { label: 'Notifications', value: inbox.data?.totalElements ?? 0 },
          ].map((card) => (
            <article key={card.label} className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
              <p className="text-xs uppercase tracking-[0.14em] text-slate-500">{card.label}</p>
              <p className="mt-1 text-2xl font-bold text-slate-900">{card.value}</p>
            </article>
          ))}
        </div>
      );
    }

    if (activeSection === 'Teachers') return <TeacherManagementPanel />;
    if (activeSection === 'Learners') return userModule('Learners');
    if (activeSection === 'Subjects') return <SubjectManagementPanel />;

    if (activeSection === 'Classes') {
      return (
        <div className="grid gap-4 xl:grid-cols-[1.3fr_0.7fr]">
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-900">Classes</h2>
            {classes.isLoading ? <LoadingState message="Loading classes..." /> : null}
            {classes.isError ? <ErrorState message="Could not load classes." /> : null}
            {!classes.isLoading && !classes.isError ? (
              <div className="mt-3 space-y-2">
                {filteredClasses.map((item) => (
                  <div key={item.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                    {editClass?.id === item.id ? (
                      <div className="grid gap-2 sm:grid-cols-4">
                        <Input value={editClass.grade} onChange={(e) => setEditClass((prev) => prev ? ({ ...prev, grade: e.target.value }) : prev)} />
                        <Input value={editClass.className} onChange={(e) => setEditClass((prev) => prev ? ({ ...prev, className: e.target.value }) : prev)} />
                        <Input type="number" value={editClass.academicYear} onChange={(e) => setEditClass((prev) => prev ? ({ ...prev, academicYear: Number(e.target.value) }) : prev)} />
                        <Input value={editClass.term ?? ''} onChange={(e) => setEditClass((prev) => prev ? ({ ...prev, term: e.target.value }) : prev)} placeholder="Term" />
                        <div className="sm:col-span-4 flex gap-2">
                          <Button type="button" disabled={updateClass.isPending} onClick={() => void updateClass.mutate(editClass)}>{updateClass.isPending ? 'Saving...' : 'Save'}</Button>
                          <Button type="button" className="bg-primary-600 hover:bg-primary-600" onClick={() => setEditClass(null)}>Cancel</Button>
                        </div>
                      </div>
                    ) : (
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <p className="text-sm text-slate-800">{item.grade} {item.className} · {item.academicYear}</p>
                        <div className="flex gap-2">
                          <Button type="button" className="bg-primary-600 hover:bg-primary-700" onClick={() => setEditClass(item)}>Edit</Button>
                          <Button type="button" className="bg-red-600 hover:bg-red-700" onClick={() => deactivateClass.mutate(item.id)} disabled={deactivateClass.isPending}>Deactivate</Button>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
                {!filteredClasses.length ? <EmptyState title="No classes" message="Create your first class from the form on the right." /> : null}
              </div>
            ) : null}
          </div>
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
            <h3 className="text-sm font-semibold text-slate-900">Create Class</h3>
            <Input value={grade} onChange={(e) => setGrade(e.target.value)} placeholder="Grade" />
            <Input value={className} onChange={(e) => setClassName(e.target.value)} placeholder="Class name" />
            <Input type="number" value={academicYear} onChange={(e) => setAcademicYear(Number(e.target.value))} placeholder="Academic year" />
            <Button type="button" onClick={onCreateClass} disabled={creatingClass || !grade.trim() || !className.trim()}>
              {creatingClass ? 'Creating...' : 'Create Class'}
            </Button>
          </div>
        </div>
      );
    }

    if (activeSection === 'Subjects') {
      return (
        <div className="grid gap-4 xl:grid-cols-[1.3fr_0.7fr]">
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-900">Subjects</h2>
            {subjects.isLoading ? <LoadingState message="Loading subjects..." /> : null}
            {subjects.isError ? <ErrorState message="Could not load subjects." /> : null}
            {!subjects.isLoading && !subjects.isError ? (
              <div className="mt-3 space-y-2">
                {filteredSubjects.map((item) => (
                  <div key={item.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                    {editSubject?.id === item.id ? (
                      <div className="grid gap-2 sm:grid-cols-3">
                        <Input value={editSubject.subjectName} onChange={(e) => setEditSubject((prev) => prev ? ({ ...prev, subjectName: e.target.value }) : prev)} />
                        <Input value={editSubject.phase} onChange={(e) => setEditSubject((prev) => prev ? ({ ...prev, phase: e.target.value }) : prev)} />
                        <Input value={editSubject.grade ?? ''} onChange={(e) => setEditSubject((prev) => prev ? ({ ...prev, grade: e.target.value }) : prev)} placeholder="Grade" />
                        <div className="sm:col-span-3 flex gap-2">
                          <Button type="button" disabled={updateSubject.isPending} onClick={() => void updateSubject.mutate(editSubject)}>{updateSubject.isPending ? 'Saving...' : 'Save'}</Button>
                          <Button type="button" className="bg-primary-600 hover:bg-primary-600" onClick={() => setEditSubject(null)}>Cancel</Button>
                        </div>
                      </div>
                    ) : (
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <p className="text-sm text-slate-800">{item.subjectName} · {item.phase} {item.grade ? `· ${item.grade}` : ''}</p>
                        <div className="flex gap-2">
                          <Button type="button" className="bg-primary-600 hover:bg-primary-700" onClick={() => setEditSubject(item)}>Edit</Button>
                          <Button type="button" className="bg-red-600 hover:bg-red-700" onClick={() => deactivateSubject.mutate(item.id)} disabled={deactivateSubject.isPending}>Deactivate</Button>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
                {!filteredSubjects.length ? <EmptyState title="No subjects" message="Create your first subject from the form on the right." /> : null}
              </div>
            ) : null}
          </div>
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
            <h3 className="text-sm font-semibold text-slate-900">Create Subject</h3>
            <Input value={subjectName} onChange={(e) => setSubjectName(e.target.value)} placeholder="Subject name" />
            <Input value={phase} onChange={(e) => setPhase(e.target.value)} placeholder="Phase" />
            <Button type="button" onClick={onCreateSubject} disabled={creatingSubject || !subjectName.trim() || !phase.trim()}>
              {creatingSubject ? 'Creating...' : 'Create Subject'}
            </Button>
          </div>
        </div>
      );
    }

    if (activeSection === 'Assignments') {
      return (
        <div className="grid gap-4 xl:grid-cols-2">
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
            <h2 className="text-lg font-semibold text-slate-900">Teacher Assignment</h2>
            <select className="h-10 rounded-xl border border-slate-200 px-3 text-sm" value={assignmentForm.teacherUserId} onChange={(e) => setAssignmentForm((prev) => ({ ...prev, teacherUserId: e.target.value }))}>
              <option value="">Select teacher</option>
              {(teachers.data ?? []).map((t) => <option key={t.userId} value={t.userId}>{t.fullName}</option>)}
            </select>
            <select className="h-10 rounded-xl border border-slate-200 px-3 text-sm" value={assignmentForm.classId} onChange={(e) => setAssignmentForm((prev) => ({ ...prev, classId: e.target.value }))}>
              <option value="">Select class</option>
              {(classes.data ?? []).map((c) => <option key={c.id} value={c.id}>{c.grade} {c.className}</option>)}
            </select>
            <select className="h-10 rounded-xl border border-slate-200 px-3 text-sm" value={assignmentForm.subjectId} onChange={(e) => setAssignmentForm((prev) => ({ ...prev, subjectId: e.target.value }))}>
              <option value="">Select subject</option>
              {(subjects.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.subjectName}</option>)}
            </select>
            <Button type="button" disabled={assignTeacher.isPending || !assignmentForm.teacherUserId || !assignmentForm.classId || !assignmentForm.subjectId} onClick={() => assignTeacher.mutate()}>
              {assignTeacher.isPending ? 'Assigning...' : 'Assign Teacher'}
            </Button>
          </div>
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
            <h3 className="text-sm font-semibold text-slate-900">Existing Assignments</h3>
            <div className="mt-3 space-y-2">
              {(teacherAssignments.data ?? []).map((item) => (
                <div key={item.id} className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700">
                  Teacher: {teacherNameById[item.teacherUserId] ?? item.teacherUserId} · Class: {classLabelById[item.classId] ?? item.classId} · Subject: {subjectLabelById[item.subjectId] ?? item.subjectId}
                </div>
              ))}
              {!(teacherAssignments.data ?? []).length ? <EmptyState title="No assignments" message="Assign teachers to class-subject combinations." /> : null}
            </div>
          </div>
        </div>
      );
    }

    if (activeSection === 'Assessments') {
      return (
        <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900">Assessments</h2>
          {assessments.isLoading ? <LoadingState message="Loading assessments..." /> : null}
          {assessments.isError ? <ErrorState message="Could not load assessments." /> : null}
          {!assessments.isLoading && !assessments.isError ? (
            <div className="mt-3 space-y-2">
              {(assessments.data ?? []).map((item) => (
                <div key={item.id} className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
                  <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                  <p className="text-xs text-slate-600">{item.taskType} · Due {new Date(item.dueAt).toLocaleDateString()}</p>
                </div>
              ))}
              {!(assessments.data ?? []).length ? <EmptyState title="No assessments" message="Assessments are created in the teacher workflow and listed here." /> : null}
            </div>
          ) : null}
        </div>
      );
    }

    if (activeSection === 'Results' || activeSection === 'Reports') {
      const data = activeSection === 'Results' ? (results.data ?? []) : (submissions.data ?? []);
      return (
        <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900">{activeSection}</h2>
          {(activeSection === 'Results' ? results.isLoading : submissions.isLoading) ? <LoadingState message={`Loading ${activeSection.toLowerCase()}...`} /> : null}
          {(activeSection === 'Results' ? results.isError : submissions.isError) ? <ErrorState message={`Could not load ${activeSection.toLowerCase()}.`} /> : null}
          {!((activeSection === 'Results' ? results.isLoading : submissions.isLoading) || (activeSection === 'Results' ? results.isError : submissions.isError)) ? (
            <div className="mt-3 space-y-2">
              {data.map((row) => (
                <div key={row.submissionId} className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
                  <p className="text-sm font-semibold text-slate-900">{row.learnerName}</p>
                  <p className="text-xs text-slate-600">Status: {row.status} · Marks: {row.marks ?? '-'} · Similarity: {row.similarity}%</p>
                </div>
              ))}
              {!data.length ? <EmptyState title={`No ${activeSection.toLowerCase()}`} message="No records available yet." /> : null}
            </div>
          ) : null}
        </div>
      );
    }

    if (activeSection === 'Notifications') {
      const items = inbox.data?.content ?? [];
      return (
        <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-slate-900">My Notifications</h2>
            <Button type="button" className="bg-primary-600 hover:bg-primary-700" disabled={markAllNotificationsRead.isPending || !items.length} onClick={() => markAllNotificationsRead.mutate()}>
              Mark all read
            </Button>
          </div>
          {inbox.isLoading ? <LoadingState message="Loading notifications..." /> : null}
          {inbox.isError ? <ErrorState message="Could not load notifications." /> : null}
          {!inbox.isLoading && !inbox.isError ? (
            <div className="space-y-2">
              {items.map((item) => (
                <div key={item.id} className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                      <p className="text-xs text-slate-600">{item.message}</p>
                    </div>
                    {!item.isRead ? <Button type="button" onClick={() => markNotificationRead.mutate(item.id)} disabled={markNotificationRead.isPending}>Mark read</Button> : null}
                  </div>
                </div>
              ))}
              {!items.length ? <EmptyState title="No notifications" message="You do not have notifications yet." /> : null}
            </div>
          ) : null}
        </div>
      );
    }

    if (activeSection === 'Settings') {
      return (
        <div className="grid gap-4 xl:grid-cols-2">
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
            <h2 className="text-lg font-semibold text-slate-900">School Profile</h2>
            <Input value={profileForm.schoolName} onChange={(e) => setProfileForm((p) => ({ ...p, schoolName: e.target.value }))} placeholder="School name" />
            <Input value={profileForm.registrationNumber} onChange={(e) => setProfileForm((p) => ({ ...p, registrationNumber: e.target.value }))} placeholder="Registration number" />
            <Input value={profileForm.district} onChange={(e) => setProfileForm((p) => ({ ...p, district: e.target.value }))} placeholder="District" />
            <Input value={profileForm.province} onChange={(e) => setProfileForm((p) => ({ ...p, province: e.target.value }))} placeholder="Province" />
            <Input value={profileForm.contactEmail} onChange={(e) => setProfileForm((p) => ({ ...p, contactEmail: e.target.value }))} placeholder="Contact email" />
            <Input value={profileForm.contactPhone} onChange={(e) => setProfileForm((p) => ({ ...p, contactPhone: e.target.value }))} placeholder="Contact phone" />
            <Input value={profileForm.address} onChange={(e) => setProfileForm((p) => ({ ...p, address: e.target.value }))} placeholder="Address" />
            <Button type="button" disabled={saveProfile.isPending || !profileForm.schoolName.trim()} onClick={() => saveProfile.mutate()}>
              {saveProfile.isPending ? 'Saving...' : 'Save Profile'}
            </Button>
          </div>
          <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
            <h3 className="text-sm font-semibold text-slate-900">Operational Overview</h3>
            <ul className="mt-3 space-y-2 text-sm text-slate-700">
              <li>Teacher assignments: {(teacherAssignments.data ?? []).length}</li>
              <li>Learner enrollments: {(learnerEnrollments.data ?? []).length}</li>
              <li>Learning notes available: {Array.isArray(notes.data) ? notes.data.length : 0}</li>
            </ul>
            <div className="mt-4 space-y-2">
              <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Recent Enrollments</p>
              {(learnerEnrollments.data ?? []).slice(0, 5).map((item) => (
                <div key={item.id} className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700">
                  Learner: {learnerNameById[item.learnerUserId] ?? item.learnerUserId} · Class: {classLabelById[item.classId] ?? item.classId} · Subject: {subjectLabelById[item.subjectId] ?? item.subjectId}
                </div>
              ))}
              {!(learnerEnrollments.data ?? []).length ? <p className="text-xs text-slate-500">No learner enrollments yet.</p> : null}
            </div>
          </div>
        </div>
      );
    }

    if (activeSection === 'Change Password') {
      return <EmptyState title="Change Password" message="Use the Change Password page from the sidebar route." />;
    }

    return <EmptyState title="Section not available" message="This section is not available for school admin." />;
  };

  return (
    <section className="space-y-4 overflow-x-hidden">
      <div className="flex min-w-0 gap-4">
        <AdminSidebar
          activeSection={activeSection}
          setActiveSection={setActiveSection}
          onLogout={onLogout}
          mobileOpen={mobileMenuOpen}
          onCloseMobile={() => setMobileMenuOpen(false)}
        />
        <div className="min-w-0 flex-1 space-y-4 overflow-x-hidden">
          <AdminTopbar fullName={fullName} search={search} setSearch={setSearch} onOpenMenu={() => setMobileMenuOpen(true)} />
          {(feedback || localMessage) ? (
            <div className={`rounded-xl border px-3 py-2 text-sm ${(localMessage ?? feedback)?.type === 'success' ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-red-200 bg-red-50 text-red-700'}`}>
              {(localMessage ?? feedback)?.message}
            </div>
          ) : null}
          {renderModule()}
        </div>
      </div>
    </section>
  );
};



