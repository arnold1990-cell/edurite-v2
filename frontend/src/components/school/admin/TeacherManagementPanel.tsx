import { useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { AdminActionButton, AdminCard, AdminFilterBar, AdminPageHeader, AdminPageLayout } from '@/components/school/admin/AdminUi';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { useAppQuery } from '@/hooks/useAppQuery';
import { schoolService, type SchoolAdminUser, type SchoolClass, type SchoolSubjectManagementView, type TeacherAssignment } from '@/services/schoolService';

type AssignmentDraft = { classId: string; subjectId: string; phase: string; grade: string; isClassTeacher: boolean };

const emptyDraft = (): AssignmentDraft => ({ classId: '', subjectId: '', phase: '', grade: '', isClassTeacher: false });

export const TeacherManagementPanel = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [subjectFilter, setSubjectFilter] = useState('');
  const [gradeFilter, setGradeFilter] = useState('');
  const [teacherForm, setTeacherForm] = useState({ firstName: '', lastName: '', email: '', phoneNumber: '', status: 'ACTIVE', password: '' });
  const [drafts, setDrafts] = useState<AssignmentDraft[]>([emptyDraft()]);
  const [assignmentTeacher, setAssignmentTeacher] = useState<SchoolAdminUser | null>(null);
  const [assignmentModalOpen, setAssignmentModalOpen] = useState(false);

  const teachersQuery = useAppQuery({ queryKey: ['school', 'admin', 'teachers'], queryFn: schoolService.listTeachers });
  const classesQuery = useAppQuery({ queryKey: ['school', 'admin', 'classes'], queryFn: schoolService.listClasses });
  const subjectsQuery = useAppQuery({ queryKey: ['school', 'subjects', 'management', 'active'], queryFn: () => schoolService.listSubjectManagement(false) });
  const assignmentsQuery = useAppQuery({ queryKey: ['school', 'admin', 'teacher-assignments'], queryFn: schoolService.listTeacherAssignments });

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'teachers'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'admin', 'teacher-assignments'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'teacher', 'subjects'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'teacher', 'classes'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'subjects'] }),
    ]);
  };

  const createTeacher = useMutation({
    mutationFn: async () => {
      const teacher = await schoolService.createSchoolUser({ ...teacherForm, roleName: 'ROLE_TEACHER' });
      const validAssignments = drafts.filter((item) => item.classId && item.subjectId).map((item) => ({ teacherUserId: teacher.userId, ...item }));
      if (validAssignments.length) {
        await schoolService.replaceTeacherAssignments(teacher.userId, validAssignments);
      }
      return teacher;
    },
    onSuccess: async () => {
      setTeacherForm({ firstName: '', lastName: '', email: '', phoneNumber: '', status: 'ACTIVE', password: '' });
      setDrafts([emptyDraft()]);
      await refresh();
    },
  });

  const deactivateTeacher = useMutation({
    mutationFn: (teacherId: string) => schoolService.deactivateSchoolUser(teacherId),
    onSuccess: refresh,
  });

  const saveAssignments = useMutation({
    mutationFn: async () => {
      if (!assignmentTeacher) return [];
      return schoolService.replaceTeacherAssignments(assignmentTeacher.userId, drafts.filter((item) => item.classId && item.subjectId).map((item) => ({ teacherUserId: assignmentTeacher.userId, ...item })));
    },
    onSuccess: async () => {
      setAssignmentTeacher(null);
      setAssignmentModalOpen(false);
      setDrafts([emptyDraft()]);
      await refresh();
    },
  });

  const teachers = teachersQuery.data ?? [];
  const classes = classesQuery.data ?? [];
  const subjects = subjectsQuery.data ?? [];
  const assignments = assignmentsQuery.data ?? [];
  const classById = useMemo(() => Object.fromEntries(classes.map((item) => [item.id, item])), [classes]);
  const subjectById = useMemo(() => Object.fromEntries(subjects.map((item) => [item.id, item])), [subjects]);

  const teacherAssignmentsByTeacher = useMemo(() => assignments.reduce<Record<string, TeacherAssignment[]>>((acc, item) => {
    if (!acc[item.teacherUserId]) acc[item.teacherUserId] = [];
    acc[item.teacherUserId].push(item);
    return acc;
  }, {}), [assignments]);

  const filteredTeachers = useMemo(() => teachers.filter((teacher) => {
    const teacherAssignments = teacherAssignmentsByTeacher[teacher.userId] ?? [];
    const subjectNames = teacherAssignments.map((item) => subjectById[item.subjectId]?.subjectName ?? '').join(' ');
    const gradeNames = teacherAssignments.map((item) => classById[item.classId]?.grade ?? item.grade ?? '').join(' ');
    const matchesSearch = `${teacher.fullName} ${teacher.email} ${subjectNames} ${gradeNames}`.toLowerCase().includes(search.trim().toLowerCase());
    const matchesSubject = !subjectFilter || teacherAssignments.some((item) => item.subjectId === subjectFilter);
    const matchesGrade = !gradeFilter || teacherAssignments.some((item) => (classById[item.classId]?.grade ?? item.grade) === gradeFilter);
    return matchesSearch && matchesSubject && matchesGrade;
  }), [teachers, teacherAssignmentsByTeacher, subjectById, classById, search, subjectFilter, gradeFilter]);

  if (teachersQuery.isLoading || classesQuery.isLoading || subjectsQuery.isLoading || assignmentsQuery.isLoading) {
    return <LoadingState message="Loading teacher management..." />;
  }
  if (teachersQuery.isError || classesQuery.isError || subjectsQuery.isError || assignmentsQuery.isError) {
    return <ErrorState message="Unable to load teacher management." />;
  }

  const openAssignmentModal = (teacher: SchoolAdminUser) => {
    const existing = (teacherAssignmentsByTeacher[teacher.userId] ?? []).filter((item) => item.active).map((item) => ({
      classId: item.classId,
      subjectId: item.subjectId,
      phase: item.phase ?? subjectById[item.subjectId]?.phase ?? '',
      grade: item.grade ?? classById[item.classId]?.grade ?? '',
      isClassTeacher: Boolean(item.isClassTeacher),
    }));
    setAssignmentTeacher(teacher);
    setDrafts(existing.length ? existing : [emptyDraft()]);
    setAssignmentModalOpen(true);
  };

  return (
    <AdminPageLayout>
      <AdminPageHeader title="Teachers" subtitle="Teacher approvals, subject assignment, activity visibility, and staffing actions in one compact workspace." />
      {(createTeacher.isError || deactivateTeacher.isError || saveAssignments.isError) ? <ErrorState message={(createTeacher.error as Error | null)?.message ?? (deactivateTeacher.error as Error | null)?.message ?? (saveAssignments.error as Error | null)?.message ?? 'Teacher action failed.'} /> : null}
      <div className="grid gap-4 xl:grid-cols-[1.25fr_0.75fr]">
        <AdminCard className="p-4">
          <AdminFilterBar className="p-0 shadow-none border-0">
          <div className="flex flex-wrap items-center gap-3">
            <Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search teachers" className="h-11 flex-1 rounded-2xl" />
            <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={subjectFilter} onChange={(e) => setSubjectFilter(e.target.value)}>
              <option value="">All subjects</option>
              {subjects.map((item) => <option key={item.id} value={item.id}>{item.subjectName}</option>)}
            </select>
            <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={gradeFilter} onChange={(e) => setGradeFilter(e.target.value)}>
              <option value="">All grades</option>
              {Array.from(new Set(classes.map((item) => item.grade))).map((grade) => <option key={grade} value={grade}>{grade}</option>)}
            </select>
          </div>
          </AdminFilterBar>
          <div className="mt-4 space-y-3">
            {filteredTeachers.map((teacher) => {
              const teacherAssignments = (teacherAssignmentsByTeacher[teacher.userId] ?? []).filter((item) => item.active);
              const subjectSummary = teacherAssignments.map((item) => {
                const subject = subjectById[item.subjectId]?.subjectName ?? 'Subject';
                const clazz = classById[item.classId] as SchoolClass | undefined;
                const classLabel = clazz ? `${clazz.grade} ${clazz.className}` : item.grade || 'Class';
                return `${subject} | ${classLabel}${item.isClassTeacher ? ' | Class teacher' : ''}`;
              });
              return (
                <article key={teacher.userId} className="rounded-[16px] border border-[#e5edf7] bg-slate-50/70 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{teacher.fullName}</p>
                      <p className="text-xs text-slate-500">{teacher.email}</p>
                      <p className="mt-2 text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Assigned Subjects</p>
                      <div className="mt-2 flex flex-wrap gap-2">
                        {subjectSummary.map((item) => <span key={`${teacher.userId}-${item}`} className="rounded-full bg-white px-3 py-1 text-xs text-slate-700">{item}</span>)}
                        {!subjectSummary.length ? <span className="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-700">Unassigned teacher</span> : null}
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <AdminActionButton type="button" variant="primary" onClick={() => openAssignmentModal(teacher)}>Assign Subjects</AdminActionButton>
                      <AdminActionButton type="button" variant="danger" disabled={deactivateTeacher.isPending} onClick={() => deactivateTeacher.mutate(teacher.userId)}>Deactivate</AdminActionButton>
                    </div>
                  </div>
                </article>
              );
            })}
            {!filteredTeachers.length ? <EmptyState title="No teachers found" message="No teachers match the current filters." /> : null}
          </div>
        </AdminCard>

        <AdminCard className="p-4">
          <p className="text-[13px] font-semibold text-primary-700">Teacher Registration</p>
          <h3 className="mt-2 text-[20px] font-semibold text-slate-900">Create teacher and assign subjects</h3>
          <div className="mt-4 grid gap-3">
            <Input value={teacherForm.firstName} onChange={(e) => setTeacherForm((current) => ({ ...current, firstName: e.target.value }))} placeholder="First name" className="h-11 rounded-2xl" />
            <Input value={teacherForm.lastName} onChange={(e) => setTeacherForm((current) => ({ ...current, lastName: e.target.value }))} placeholder="Last name" className="h-11 rounded-2xl" />
            <Input value={teacherForm.email} onChange={(e) => setTeacherForm((current) => ({ ...current, email: e.target.value }))} placeholder="Email" className="h-11 rounded-2xl" />
            <Input value={teacherForm.phoneNumber} onChange={(e) => setTeacherForm((current) => ({ ...current, phoneNumber: e.target.value }))} placeholder="Phone number" className="h-11 rounded-2xl" />
            <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={teacherForm.status} onChange={(e) => setTeacherForm((current) => ({ ...current, status: e.target.value }))}>
              <option value="ACTIVE">Active</option>
              <option value="PENDING">Pending</option>
              <option value="SUSPENDED">Suspended</option>
            </select>
            <Input value={teacherForm.password} onChange={(e) => setTeacherForm((current) => ({ ...current, password: e.target.value }))} placeholder="Password" className="h-11 rounded-2xl" />
            {drafts.map((draft, index) => (
              <div key={`draft-${index}`} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                <div className="grid gap-3">
                  <select className="h-10 rounded-xl border border-slate-200 px-3 text-sm" value={draft.subjectId} onChange={(e) => {
                    const subject = subjectById[e.target.value] as SchoolSubjectManagementView | undefined;
                    setDrafts((current) => current.map((item, currentIndex) => currentIndex === index ? { ...item, subjectId: e.target.value, phase: subject?.phase ?? item.phase, grade: item.grade || subject?.grade || '' } : item));
                  }}>
                    <option value="">Select subject</option>
                    {subjects.map((item) => <option key={item.id} value={item.id}>{item.subjectName}</option>)}
                  </select>
                  <select className="h-10 rounded-xl border border-slate-200 px-3 text-sm" value={draft.classId} onChange={(e) => {
                    const clazz = classById[e.target.value] as SchoolClass | undefined;
                    setDrafts((current) => current.map((item, currentIndex) => currentIndex === index ? { ...item, classId: e.target.value, grade: clazz?.grade ?? item.grade } : item));
                  }}>
                    <option value="">Select class</option>
                    {classes.map((item) => <option key={item.id} value={item.id}>{item.grade} {item.className}</option>)}
                  </select>
                  <label className="inline-flex items-center gap-2 text-sm text-slate-600">
                    <input type="checkbox" checked={draft.isClassTeacher} onChange={(e) => setDrafts((current) => current.map((item, currentIndex) => currentIndex === index ? { ...item, isClassTeacher: e.target.checked } : item))} />
                    Class teacher
                  </label>
                </div>
              </div>
            ))}
            <AdminActionButton type="button" variant="ghost" className="h-11" onClick={() => setDrafts((current) => [...current, emptyDraft()])}>Add Assignment Row</AdminActionButton>
            <Button type="button" className="h-11 rounded-2xl bg-primary-600 hover:bg-primary-500" disabled={createTeacher.isPending || !teacherForm.firstName.trim() || !teacherForm.lastName.trim() || !teacherForm.email.trim() || !teacherForm.password.trim()} onClick={() => createTeacher.mutate()}>
              {createTeacher.isPending ? 'Saving...' : 'Create Teacher'}
            </Button>
          </div>
        </AdminCard>
      </div>

      {assignmentModalOpen && assignmentTeacher ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">
          <div className="w-full max-w-3xl rounded-[32px] border border-slate-200 bg-white p-6 shadow-2xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary-700">Assign Subjects</p>
                <h3 className="mt-2 text-2xl font-semibold text-slate-900">{assignmentTeacher.fullName}</h3>
                <p className="mt-2 text-sm text-slate-600">Assignments are limited to the school&apos;s registered subjects. Teachers will only see these subjects on their dashboard and mark-entry flows.</p>
              </div>
              <Button type="button" className="rounded-2xl bg-slate-200 px-4 text-slate-800 hover:bg-slate-300" onClick={() => { setAssignmentModalOpen(false); setAssignmentTeacher(null); }}>Close</Button>
            </div>
            <div className="mt-6 space-y-3">
              {drafts.map((draft, index) => (
                <div key={`modal-draft-${index}`} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div className="grid gap-3 md:grid-cols-2">
                    <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={draft.subjectId} onChange={(e) => {
                      const subject = subjectById[e.target.value] as SchoolSubjectManagementView | undefined;
                      setDrafts((current) => current.map((item, currentIndex) => currentIndex === index ? { ...item, subjectId: e.target.value, phase: subject?.phase ?? item.phase } : item));
                    }}>
                      <option value="">Select subject</option>
                      {subjects.map((item) => <option key={item.id} value={item.id}>{item.subjectName}</option>)}
                    </select>
                    <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={draft.classId} onChange={(e) => {
                      const clazz = classById[e.target.value] as SchoolClass | undefined;
                      setDrafts((current) => current.map((item, currentIndex) => currentIndex === index ? { ...item, classId: e.target.value, grade: clazz?.grade ?? item.grade } : item));
                    }}>
                      <option value="">Select class</option>
                      {classes.map((item) => <option key={item.id} value={item.id}>{item.grade} {item.className}</option>)}
                    </select>
                  </div>
                  <div className="mt-3 flex flex-wrap items-center gap-4">
                    <span className="text-xs text-slate-500">Phase: {draft.phase || 'Auto'}</span>
                    <span className="text-xs text-slate-500">Grade: {draft.grade || 'Auto'}</span>
                    <label className="inline-flex items-center gap-2 text-sm text-slate-600">
                      <input type="checkbox" checked={draft.isClassTeacher} onChange={(e) => setDrafts((current) => current.map((item, currentIndex) => currentIndex === index ? { ...item, isClassTeacher: e.target.checked } : item))} />
                      Class teacher
                    </label>
                    <button type="button" className="text-xs font-semibold text-red-600" onClick={() => setDrafts((current) => current.filter((_, currentIndex) => currentIndex !== index).length ? current.filter((_, currentIndex) => currentIndex !== index) : [emptyDraft()])}>Remove row</button>
                  </div>
                </div>
              ))}
              <Button type="button" className="rounded-2xl bg-slate-200 text-slate-800 hover:bg-slate-300" onClick={() => setDrafts((current) => [...current, emptyDraft()])}>Add Assignment Row</Button>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <Button type="button" className="rounded-2xl bg-slate-200 px-4 text-slate-800 hover:bg-slate-300" onClick={() => { setAssignmentModalOpen(false); setAssignmentTeacher(null); }}>Cancel</Button>
              <Button type="button" className="rounded-2xl bg-primary-600 px-5 hover:bg-primary-500" disabled={saveAssignments.isPending} onClick={() => saveAssignments.mutate()}>
                {saveAssignments.isPending ? 'Saving...' : 'Save Assignments'}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </AdminPageLayout>
  );
};




