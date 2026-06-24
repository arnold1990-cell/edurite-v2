import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { AdminCard, AdminPageHeader, AdminPageLayout } from '@/components/school/admin/AdminUi';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { useAppQuery } from '@/hooks/useAppQuery';
import { schoolService, type AtpTopic, type SchoolClass, type SchoolSubjectManagementView } from '@/services/schoolService';

const currentYear = new Date().getFullYear();

type AssignmentFormState = {
  teacherUserId: string;
  classId: string;
  subjectId: string;
  atpTopicId: string;
  academicYear: number;
  term: string;
  phase: string;
  grade: string;
  taskType: string;
  assessmentType: string;
  assessmentCategory: string;
  weekNumber: string;
  title: string;
  instructions: string;
  resources: string;
  cognitiveLevel: string;
  dueDate: string;
  maxMarks: string;
};

const emptyForm = (): AssignmentFormState => ({
  teacherUserId: '',
  classId: '',
  subjectId: '',
  atpTopicId: '',
  academicYear: currentYear,
  term: 'Term 1',
  phase: '',
  grade: '',
  taskType: 'ASSIGNMENT',
  assessmentType: 'Formal assessment',
  assessmentCategory: 'SBA',
  weekNumber: '',
  title: '',
  instructions: '',
  resources: '',
  cognitiveLevel: '',
  dueDate: '',
  maxMarks: '100',
});

export const AssignmentManagementPanel = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [gradeFilter, setGradeFilter] = useState('');
  const [termFilter, setTermFilter] = useState('ALL');
  const [form, setForm] = useState<AssignmentFormState>(emptyForm);

  const classesQuery = useAppQuery({ queryKey: ['school', 'admin', 'classes'], queryFn: schoolService.listClasses });
  const subjectsQuery = useAppQuery({ queryKey: ['school', 'subjects', 'management', 'active'], queryFn: () => schoolService.listSubjectManagement(false) });
  const teachersQuery = useAppQuery({ queryKey: ['school', 'admin', 'teachers'], queryFn: schoolService.listTeachers });
  const assignmentsQuery = useAppQuery({ queryKey: ['school', 'admin', 'teacher-assignments'], queryFn: schoolService.listTeacherAssignments });
  const tasksQuery = useAppQuery({ queryKey: ['school', 'tasks'], queryFn: schoolService.schoolTasks });
  const atpTopicsQuery = useAppQuery({
    queryKey: ['school', 'atp-topics', form.phase, form.grade, form.subjectId, form.term],
    queryFn: () => schoolService.listAtpTopics({ phase: form.phase, grade: form.grade, subjectId: form.subjectId, term: form.term }),
    enabled: Boolean(form.phase && form.grade && form.subjectId && form.term),
  });

  const classes = classesQuery.data ?? [];
  const subjects = subjectsQuery.data ?? [];
  const teachers = teachersQuery.data ?? [];
  const teacherAssignments = assignmentsQuery.data ?? [];
  const tasks = tasksQuery.data ?? [];
  const atpTopics = atpTopicsQuery.data ?? [];

  const classById = useMemo(() => Object.fromEntries(classes.map((item) => [item.id, item])), [classes]);
  const subjectById = useMemo(() => Object.fromEntries(subjects.map((item) => [item.id, item])), [subjects]);
  const teacherById = useMemo(() => Object.fromEntries(teachers.map((item) => [item.userId, item])), [teachers]);
  const atpTopicById = useMemo(() => Object.fromEntries(atpTopics.map((item) => [item.id, item])), [atpTopics]);

  const availableTeacherAssignments = useMemo(() => teacherAssignments.filter((item) => item.active && item.classId === form.classId && item.subjectId === form.subjectId), [teacherAssignments, form.classId, form.subjectId]);

  useEffect(() => {
    if (!form.subjectId) return;
    const subject = subjectById[form.subjectId] as SchoolSubjectManagementView | undefined;
    if (!subject) return;
    setForm((current) => ({
      ...current,
      phase: subject.phase || current.phase,
      grade: current.grade || subject.grade || current.grade,
    }));
  }, [form.subjectId, subjectById]);

  useEffect(() => {
    if (!form.classId) return;
    const schoolClass = classById[form.classId] as SchoolClass | undefined;
    if (!schoolClass) return;
    setForm((current) => ({ ...current, grade: schoolClass.grade || current.grade, academicYear: schoolClass.academicYear || current.academicYear }));
  }, [form.classId, classById]);

  useEffect(() => {
    if (availableTeacherAssignments.length === 1 && !form.teacherUserId) {
      setForm((current) => ({ ...current, teacherUserId: availableTeacherAssignments[0].teacherUserId }));
    }
  }, [availableTeacherAssignments, form.teacherUserId]);

  const createTask = useMutation({
    mutationFn: () => schoolService.createSchoolTask({
      teacherUserId: form.teacherUserId || undefined,
      classId: form.classId,
      subjectId: form.subjectId,
      atpTopicId: form.atpTopicId || undefined,
      taskType: form.taskType,
      title: form.title,
      academicYear: form.academicYear,
      phase: form.phase,
      grade: form.grade,
      instructions: form.instructions,
      assessmentType: form.assessmentType,
      weekNumber: form.weekNumber ? Number(form.weekNumber) : undefined,
      dueAt: new Date(form.dueDate).toISOString(),
      term: form.term,
      maxMarks: Number(form.maxMarks),
      resources: form.resources,
      cognitiveLevel: form.cognitiveLevel,
      assessmentCategory: form.assessmentCategory,
    }),
    onSuccess: async () => {
      setForm(emptyForm());
      await queryClient.invalidateQueries({ queryKey: ['school', 'tasks'] });
    },
  });

  const filteredTasks = useMemo(() => tasks.filter((task) => {
    const subject = subjectById[task.subjectId] as SchoolSubjectManagementView | undefined;
    const schoolClass = classById[task.classId] as SchoolClass | undefined;
    const matchesSearch = `${task.title} ${subject?.subjectName ?? ''} ${task.term ?? ''} ${task.grade ?? schoolClass?.grade ?? ''}`.toLowerCase().includes(search.trim().toLowerCase());
    const matchesGrade = !gradeFilter || (task.grade ?? schoolClass?.grade) === gradeFilter;
    const matchesTerm = termFilter === 'ALL' || task.term === termFilter;
    return matchesSearch && matchesGrade && matchesTerm;
  }), [tasks, subjectById, classById, search, gradeFilter, termFilter]);

  if (classesQuery.isLoading || subjectsQuery.isLoading || teachersQuery.isLoading || assignmentsQuery.isLoading || tasksQuery.isLoading) {
    return <LoadingState message="Loading assignment management..." />;
  }
  if (classesQuery.isError || subjectsQuery.isError || teachersQuery.isError || assignmentsQuery.isError || tasksQuery.isError || atpTopicsQuery.isError) {
    return <ErrorState message="Unable to load assignment management." />;
  }

  const onAtpTopicChange = (topicId: string) => {
    const topic = atpTopicById[topicId] as AtpTopic | undefined;
    setForm((current) => ({
      ...current,
      atpTopicId: topicId,
      title: topic ? `${subjectById[current.subjectId]?.subjectName ?? topic.subjectName}: ${topic.topic}` : current.title,
      weekNumber: topic?.weekNumber ? String(topic.weekNumber) : current.weekNumber,
      instructions: topic?.assessmentGuidance ?? current.instructions,
      resources: topic?.recommendedActivities ?? current.resources,
    }));
  };

  return (
    <AdminPageLayout>
      <AdminPageHeader title="Assignments" subtitle="ATP-aligned assignment planning, teacher allocation, and delivery tracking in a compact school admin workspace." />
      {createTask.isError ? <ErrorState message={(createTask.error as Error | null)?.message ?? 'Assignment creation failed.'} /> : null}
      <div className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
        <AdminCard className="p-4">
          <p className="text-[13px] font-semibold text-blue-700">Create Assignment</p>
          <div className="mt-4 grid gap-3">
            <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={form.classId} onChange={(e) => setForm((current) => ({ ...current, classId: e.target.value }))}>
              <option value="">Select class</option>
              {classes.map((item) => <option key={item.id} value={item.id}>{item.grade} {item.className}</option>)}
            </select>
            <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={form.subjectId} onChange={(e) => setForm((current) => ({ ...current, subjectId: e.target.value }))}>
              <option value="">Select school subject</option>
              {subjects.map((item) => <option key={item.id} value={item.id}>{item.subjectName} | {item.phase}</option>)}
            </select>
            <div className="grid gap-3 md:grid-cols-2">
              <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={form.term} onChange={(e) => setForm((current) => ({ ...current, term: e.target.value }))}>
                <option value="Term 1">Term 1</option>
                <option value="Term 2">Term 2</option>
                <option value="Term 3">Term 3</option>
                <option value="Term 4">Term 4</option>
              </select>
              <Input type="number" value={String(form.academicYear)} onChange={(e) => setForm((current) => ({ ...current, academicYear: Number(e.target.value || currentYear) }))} placeholder="Academic year" className="h-11 rounded-2xl" />
            </div>
            <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={form.teacherUserId} onChange={(e) => setForm((current) => ({ ...current, teacherUserId: e.target.value }))}>
              <option value="">Select assigned teacher</option>
              {availableTeacherAssignments.map((assignment) => {
                const teacher = teacherById[assignment.teacherUserId];
                return <option key={`${assignment.id}-${assignment.teacherUserId}`} value={assignment.teacherUserId}>{teacher?.fullName ?? assignment.teacherUserId}</option>;
              })}
            </select>
            <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={form.atpTopicId} onChange={(e) => onAtpTopicChange(e.target.value)} disabled={!form.subjectId || !form.classId}>
              <option value="">Select ATP topic</option>
              {atpTopics.map((topic) => <option key={topic.id} value={topic.id}>Week {topic.weekNumber ?? '-'} | {topic.topic}</option>)}
            </select>
            {!atpTopics.length && form.subjectId && form.classId ? <p className="text-xs text-amber-700">No ATP topics are seeded yet for this grade, subject, and term.</p> : null}
            <Input value={form.title} onChange={(e) => setForm((current) => ({ ...current, title: e.target.value }))} placeholder="Assignment title" className="h-11 rounded-2xl" />
            <div className="grid gap-3 md:grid-cols-2">
              <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={form.taskType} onChange={(e) => setForm((current) => ({ ...current, taskType: e.target.value }))}>
                <option value="ASSIGNMENT">Assignment</option>
                <option value="SBA">SBA</option>
                <option value="TEST">Test</option>
                <option value="ASSESSMENT">Assessment</option>
              </select>
              <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={form.assessmentCategory} onChange={(e) => setForm((current) => ({ ...current, assessmentCategory: e.target.value }))}>
                <option value="SBA">SBA</option>
                <option value="Formal">Formal</option>
                <option value="Informal">Informal</option>
              </select>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              <Input value={form.assessmentType} onChange={(e) => setForm((current) => ({ ...current, assessmentType: e.target.value }))} placeholder="Assessment type" className="h-11 rounded-2xl" />
              <Input value={form.cognitiveLevel} onChange={(e) => setForm((current) => ({ ...current, cognitiveLevel: e.target.value }))} placeholder="Cognitive level" className="h-11 rounded-2xl" />
            </div>
            <div className="grid gap-3 md:grid-cols-3">
              <Input value={form.phase} onChange={(e) => setForm((current) => ({ ...current, phase: e.target.value }))} placeholder="Phase" className="h-11 rounded-2xl" />
              <Input value={form.grade} onChange={(e) => setForm((current) => ({ ...current, grade: e.target.value }))} placeholder="Grade" className="h-11 rounded-2xl" />
              <Input value={form.weekNumber} onChange={(e) => setForm((current) => ({ ...current, weekNumber: e.target.value }))} placeholder="Week number" className="h-11 rounded-2xl" />
            </div>
            <Input type="date" value={form.dueDate} onChange={(e) => setForm((current) => ({ ...current, dueDate: e.target.value }))} className="h-11 rounded-2xl" />
            <Input value={form.maxMarks} onChange={(e) => setForm((current) => ({ ...current, maxMarks: e.target.value }))} placeholder="Total marks" className="h-11 rounded-2xl" />
            <textarea value={form.instructions} onChange={(e) => setForm((current) => ({ ...current, instructions: e.target.value }))} placeholder="Instructions" className="min-h-[110px] rounded-2xl border border-slate-200 px-3 py-3 text-sm" />
            <textarea value={form.resources} onChange={(e) => setForm((current) => ({ ...current, resources: e.target.value }))} placeholder="Resources / materials" className="min-h-[90px] rounded-2xl border border-slate-200 px-3 py-3 text-sm" />
            <Button
              type="button"
              className="h-11 rounded-2xl bg-[#0B5BFF] hover:bg-[#0849cb]"
              disabled={createTask.isPending || !form.classId || !form.subjectId || !form.title.trim() || !form.dueDate || !form.maxMarks.trim()}
              onClick={() => createTask.mutate()}
            >
              {createTask.isPending ? 'Saving...' : 'Create ATP Assignment'}
            </Button>
          </div>
        </AdminCard>

        <AdminCard className="p-4">
          <div className="grid gap-3 md:grid-cols-3">
            <Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search assignments" className="h-11 rounded-2xl" />
            <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={gradeFilter} onChange={(e) => setGradeFilter(e.target.value)}>
              <option value="">All grades</option>
              {Array.from(new Set(classes.map((item) => item.grade))).map((grade) => <option key={grade} value={grade}>{grade}</option>)}
            </select>
            <select className="h-11 rounded-2xl border border-slate-200 px-3 text-sm" value={termFilter} onChange={(e) => setTermFilter(e.target.value)}>
              <option value="ALL">All terms</option>
              <option value="Term 1">Term 1</option>
              <option value="Term 2">Term 2</option>
              <option value="Term 3">Term 3</option>
              <option value="Term 4">Term 4</option>
            </select>
          </div>
          <div className="mt-4 space-y-3">
            {filteredTasks.map((task) => {
              const subject = subjectById[task.subjectId] as SchoolSubjectManagementView | undefined;
              const schoolClass = classById[task.classId] as SchoolClass | undefined;
              const teacher = teacherById[task.teacherUserId];
              return (
                <article key={task.id} className="rounded-[24px] border border-slate-200 bg-slate-50 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{task.title}</p>
                      <p className="mt-1 text-xs text-slate-500">{subject?.subjectName ?? 'Subject'} | {task.phase ?? subject?.phase ?? 'Phase'} | {task.grade ?? schoolClass?.grade ?? 'Grade not set'} | {task.term ?? 'Term not set'}</p>
                      <p className="mt-2 text-xs text-slate-600">Teacher: {teacher?.fullName ?? 'Unassigned'} | Due: {new Date(task.dueAt).toLocaleDateString()} | {task.maxMarks} marks</p>
                      <p className="mt-2 text-xs text-slate-600">{task.assessmentType ?? task.taskType}{task.weekNumber ? ` | Week ${task.weekNumber}` : ''}{task.assessmentCategory ? ` | ${task.assessmentCategory}` : ''}</p>
                    </div>
                  </div>
                </article>
              );
            })}
            {!filteredTasks.length ? <EmptyState title="No assignments created yet" message="Create the first ATP-aligned assignment for this school." /> : null}
          </div>
        </AdminCard>
      </div>
    </AdminPageLayout>
  );
};
