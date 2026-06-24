import { useMemo, useState, type Dispatch, type SetStateAction } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { MoreHorizontal, Search, Upload } from 'lucide-react';
import { useAppQuery } from '@/hooks/useAppQuery';
import {
  schoolService,
  type SchoolAdminLearner,
  type SchoolAdminUser,
  type SchoolSubjectManagementView,
  type SubjectCatalogueItem,
  type TeacherAssignment,
  type TeacherWorkloadItem,
} from '@/services/schoolService';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { AdminActionButton, AdminCard, AdminPageHeader, AdminPageLayout } from '@/components/school/admin/AdminUi';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { CAPS_CATEGORY_OPTIONS, CAPS_PHASE_GROUPS, CAPS_SUBJECT_OPTIONS, DEFAULT_CAREER_PATHWAYS, type CapsPhaseGroup } from '@/components/school/admin/capsSubjectCatalogue';

type SubjectFormState = {
  id?: string;
  subjectCatalogueId: string;
  subjectName: string;
  phase: string;
  grade: string;
  gradeRange: string;
  languageLevel: string;
  subjectType: string;
  isLanguage: boolean;
  isCompulsory: boolean;
  hodUserId: string;
  capsAligned: boolean;
  active: boolean;
};

type AssignmentState = {
  subjectIds: string[];
  teacherUserId: string;
  hodUserId: string;
  classId: string;
  learnerScope: 'none' | 'class' | 'grade';
  learnerGrade: string;
};

type AssessmentState = {
  subjectId: string;
  classId: string;
  teacherUserId: string;
  title: string;
  dueDate: string;
  maxMarks: string;
  term: string;
};

type UploadState = {
  subjectId: string;
  file: File | null;
};

type AiAction = 'lesson-plan' | 'sba-task' | 'test' | 'exam' | 'memorandum' | 'rubric' | 'intervention-plan';

type SubjectInsight = {
  subjectId: string;
  subjectName: string;
  averageMark: number | null;
  passRate: number | null;
  distinctionRate: number | null;
  learnersAtRisk: number;
  assessmentCompletionRate: number | null;
  attendanceTrend: string | null;
  riskLevel: 'Healthy' | 'Attention' | 'Critical';
};

type SubjectResourceBreakdown = {
  notes: number;
  worksheets: number;
  pastPapers: number;
  memorandums: number;
  videos: number;
  presentations: number;
  pdfResources: number;
};

const phases = ['All', 'ECD', 'Foundation', 'Intermediate', 'Senior', 'FET'] as const;
const gradeOptions = ['Grade R', 'Grade 1', 'Grade 2', 'Grade 3', 'Grade 4', 'Grade 5', 'Grade 6', 'Grade 7', 'Grade 8', 'Grade 9', 'Grade 10', 'Grade 11', 'Grade 12'];
const riskLevels = ['All', 'Healthy', 'Attention', 'Critical'] as const;
const assignmentFilters = ['All', 'Assigned', 'Unassigned'] as const;
const atpFilters = ['All', 'Available', 'Missing'] as const;
const subjectStatusFilters = ['ALL', 'ACTIVE', 'INACTIVE'] as const;
const compulsoryFilters = ['All', 'Compulsory', 'Elective'] as const;
const aiActions: Array<{ key: AiAction; label: string }> = [
  { key: 'lesson-plan', label: 'Generate lesson plan' },
  { key: 'sba-task', label: 'Generate SBA task' },
  { key: 'test', label: 'Generate test' },
  { key: 'exam', label: 'Generate exam' },
  { key: 'memorandum', label: 'Generate memorandum' },
  { key: 'rubric', label: 'Generate rubric' },
  { key: 'intervention-plan', label: 'Generate intervention plan' },
];

const emptyForm: SubjectFormState = {
  subjectCatalogueId: '',
  subjectName: '',
  phase: 'Foundation',
  grade: '',
  gradeRange: 'Grades 1-3',
  languageLevel: '',
  subjectType: '',
  isLanguage: false,
  isCompulsory: false,
  hodUserId: '',
  capsAligned: true,
  active: true,
};

const emptyAssessment: AssessmentState = {
  subjectId: '',
  classId: '',
  teacherUserId: '',
  title: '',
  dueDate: '',
  maxMarks: '100',
  term: 'Term 1',
};

const emptyAssignmentState: AssignmentState = {
  subjectIds: [],
  teacherUserId: '',
  hodUserId: '',
  classId: '',
  learnerScope: 'none',
  learnerGrade: '',
};

const toneClasses: Record<SubjectInsight['riskLevel'], string> = {
  Healthy: 'bg-emerald-50 text-emerald-700',
  Attention: 'bg-amber-50 text-amber-700',
  Critical: 'bg-rose-50 text-rose-700',
};

const formatPercent = (value: number | null) => value == null ? 'Not available' : `${value.toFixed(0)}%`;

const toClassLabel = (item: { grade?: string | null; className?: string | null }) => `${item.grade ?? ''} ${item.className ?? ''}`.trim();

const toDateTime = (date: string) => new Date(date).toISOString();

const csvEscape = (value: string | number | null | undefined) => {
  const text = String(value ?? '');
  return `"${text.replace(/"/g, '""')}"`;
};

const downloadBlob = (fileName: string, contentType: string, content: BlobPart) => {
  const blob = new Blob([content], { type: contentType });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
};

const FieldLabel = ({ children }: { children: React.ReactNode }) => <span className="text-sm text-slate-700">{children}</span>;

const ModalShell = ({ title, subtitle, children, onClose }: { title: string; subtitle?: string; children: React.ReactNode; onClose: () => void }) => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">
    <div className="max-h-[92vh] w-full max-w-6xl overflow-y-auto rounded-[32px] border border-slate-200 bg-white p-6 shadow-2xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="text-2xl font-semibold text-slate-900">{title}</h3>
          {subtitle ? <p className="mt-2 text-sm text-slate-600">{subtitle}</p> : null}
        </div>
        <Button type="button" className="rounded-2xl bg-slate-200 px-4 text-slate-800 hover:bg-slate-300" onClick={onClose}>Close</Button>
      </div>
      <div className="mt-6">{children}</div>
    </div>
  </div>
);

const SubjectEditModal = ({
  form,
  setForm,
  teachers,
  catalogue,
  onClose,
  onSave,
  saving,
}: {
  form: SubjectFormState;
  setForm: Dispatch<SetStateAction<SubjectFormState>>;
  teachers: SchoolAdminUser[];
  catalogue: SubjectCatalogueItem[];
  onClose: () => void;
  onSave: () => void;
  saving: boolean;
}) => {
  const filteredCatalogue = catalogue.filter((item) => item.phase === form.phase);
  return (
    <ModalShell title={form.id ? 'Edit school subject' : 'Add School Subject'} subtitle="Keep existing CRUD intact while allowing custom subjects for special schools." onClose={onClose}>
      <div className="grid gap-4 md:grid-cols-2">
        <label className="space-y-2">
          <FieldLabel>Phase</FieldLabel>
          <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={form.phase} onChange={(e) => setForm((current) => ({ ...current, phase: e.target.value }))}>
            {phases.filter((item) => item !== 'All').map((phase) => <option key={phase} value={phase}>{phase}</option>)}
          </select>
        </label>
        <label className="space-y-2">
          <FieldLabel>Subject catalogue</FieldLabel>
          <select
            className="h-11 w-full rounded-2xl border border-slate-200 px-3"
            value={form.subjectCatalogueId}
            onChange={(e) => {
              const item = filteredCatalogue.find((entry) => entry.id === e.target.value);
              setForm((current) => ({
                ...current,
                subjectCatalogueId: e.target.value,
                subjectName: item?.name ?? current.subjectName,
                gradeRange: item?.gradeRange ?? current.gradeRange,
                subjectType: item?.subjectType ?? current.subjectType,
                languageLevel: item?.languageLevel ?? current.languageLevel,
                isLanguage: item?.isLanguage ?? current.isLanguage,
                isCompulsory: item?.isCompulsory ?? current.isCompulsory,
                capsAligned: Boolean(item),
              }));
            }}
          >
            <option value="">Custom Subject</option>
            {filteredCatalogue.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
        </label>
        <label className="space-y-2">
          <FieldLabel>Subject name</FieldLabel>
          <Input value={form.subjectName} onChange={(e) => setForm((current) => ({ ...current, subjectName: e.target.value }))} className="h-11 rounded-2xl" />
        </label>
        <label className="space-y-2">
          <FieldLabel>Grade range</FieldLabel>
          <Input value={form.gradeRange} onChange={(e) => setForm((current) => ({ ...current, gradeRange: e.target.value }))} className="h-11 rounded-2xl" />
        </label>
        <label className="space-y-2">
          <FieldLabel>Grade</FieldLabel>
          <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={form.grade} onChange={(e) => setForm((current) => ({ ...current, grade: e.target.value }))}>
            <option value="">Select grade</option>
            {gradeOptions.map((grade) => <option key={grade} value={grade}>{grade}</option>)}
          </select>
        </label>
        <label className="space-y-2">
          <FieldLabel>Language type</FieldLabel>
          <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={form.languageLevel} onChange={(e) => setForm((current) => ({ ...current, languageLevel: e.target.value }))}>
            <option value="">Not applicable</option>
            <option value="Home Language">Home Language</option>
            <option value="First Additional Language">First Additional Language</option>
            <option value="Second Additional Language">Second Additional Language</option>
          </select>
        </label>
        <label className="space-y-2">
          <FieldLabel>Subject category</FieldLabel>
          <Input value={form.subjectType} onChange={(e) => setForm((current) => ({ ...current, subjectType: e.target.value }))} className="h-11 rounded-2xl" />
        </label>
        <label className="space-y-2">
          <FieldLabel>HOD</FieldLabel>
          <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={form.hodUserId} onChange={(e) => setForm((current) => ({ ...current, hodUserId: e.target.value }))}>
            <option value="">No HOD assigned</option>
            {teachers.map((teacher) => <option key={teacher.userId} value={teacher.userId}>{teacher.fullName}</option>)}
          </select>
        </label>
      </div>
      <div className="mt-4 flex flex-wrap gap-6 text-sm text-slate-700">
        <label className="inline-flex items-center gap-2">
          <input type="checkbox" checked={form.isLanguage} onChange={(e) => setForm((current) => ({ ...current, isLanguage: e.target.checked }))} />
          Language subject
        </label>
        <label className="inline-flex items-center gap-2">
          <input type="checkbox" checked={form.isCompulsory} onChange={(e) => setForm((current) => ({ ...current, isCompulsory: e.target.checked }))} />
          Compulsory subject
        </label>
        <label className="inline-flex items-center gap-2">
          <input type="checkbox" checked={form.capsAligned} onChange={(e) => setForm((current) => ({ ...current, capsAligned: e.target.checked }))} />
          CAPS aligned
        </label>
      </div>
      <div className="mt-6 flex justify-end gap-3">
        <Button type="button" className="rounded-2xl bg-slate-200 px-4 text-slate-800 hover:bg-slate-300" onClick={onClose}>Cancel</Button>
        <Button type="button" className="rounded-2xl bg-[#0B5BFF] px-5 hover:bg-[#0849cb]" disabled={saving || !form.subjectName.trim() || !form.phase.trim() || !(form.grade.trim() || form.gradeRange.trim())} onClick={onSave}>
          {saving ? 'Saving...' : 'Save subject'}
        </Button>
      </div>
    </ModalShell>
  );
};

export const SubjectManagementPanel = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [phaseFilter, setPhaseFilter] = useState<(typeof phases)[number]>('All');
  const [gradeFilter, setGradeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<(typeof subjectStatusFilters)[number]>('ALL');
  const [teacherFilter, setTeacherFilter] = useState('');
  const [hodFilter, setHodFilter] = useState('');
  const [assignmentFilter, setAssignmentFilter] = useState<(typeof assignmentFilters)[number]>('All');
  const [atpFilter, setAtpFilter] = useState<(typeof atpFilters)[number]>('All');
  const [riskFilter, setRiskFilter] = useState<(typeof riskLevels)[number]>('All');
  const [languageFilter, setLanguageFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [compulsoryFilter, setCompulsoryFilter] = useState<(typeof compulsoryFilters)[number]>('All');
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isCapsModalOpen, setIsCapsModalOpen] = useState(false);
  const [isBulkAssignOpen, setIsBulkAssignOpen] = useState(false);
  const [viewSubjectId, setViewSubjectId] = useState<string | null>(null);
  const [assessmentState, setAssessmentState] = useState<AssessmentState>(emptyAssessment);
  const [uploadState, setUploadState] = useState<UploadState>({ subjectId: '', file: null });
  const [aiState, setAiState] = useState<{ subjectId: string; action: AiAction } | null>(null);
  const [form, setForm] = useState<SubjectFormState>(emptyForm);
  const [selectedSubjectIds, setSelectedSubjectIds] = useState<string[]>([]);
  const [assignmentState, setAssignmentState] = useState<AssignmentState>(emptyAssignmentState);
  const [capsPhaseGroup, setCapsPhaseGroup] = useState<CapsPhaseGroup>('foundation-stream');
  const [capsGrade, setCapsGrade] = useState('Grade 10');
  const [capsCategory, setCapsCategory] = useState('Languages');
  const [capsSearch, setCapsSearch] = useState('');
  const [capsSelected, setCapsSelected] = useState<string[]>([]);
  const [localMessage, setLocalMessage] = useState<{ tone: 'success' | 'error' | 'neutral'; text: string } | null>(null);

  const subjectSummaryQuery = useAppQuery({ queryKey: ['school', 'subjects', 'summary'], queryFn: schoolService.subjectSummary });
  const subjectCatalogueQuery = useAppQuery({ queryKey: ['school', 'subjects', 'catalogue'], queryFn: schoolService.listSubjectCatalogue });
  const subjectsQuery = useAppQuery({ queryKey: ['school', 'subjects', 'management'], queryFn: () => schoolService.listSubjectManagement(true) });
  const teachersQuery = useAppQuery({ queryKey: ['school', 'teachers'], queryFn: schoolService.listTeachers });
  const classesQuery = useAppQuery({ queryKey: ['school', 'classes'], queryFn: schoolService.listClasses });
  const teacherAssignmentsQuery = useAppQuery({ queryKey: ['school', 'teacher-assignments'], queryFn: schoolService.listTeacherAssignments });
  const learnerEnrollmentsQuery = useAppQuery({ queryKey: ['school', 'learner-enrollments'], queryFn: schoolService.listLearnerEnrollments });
  const atpTopicsQuery = useAppQuery({ queryKey: ['school', 'atp-topics', 'all'], queryFn: () => schoolService.listAtpTopics() });
  const notesQuery = useAppQuery<Array<{ id?: string; subjectId?: string; title?: string; noteText?: string; pdfUrl?: string }>>({ queryKey: ['school', 'notes'], queryFn: schoolService.schoolNotes });
  const tasksQuery = useAppQuery({ queryKey: ['school', 'tasks'], queryFn: schoolService.schoolTasks });
  const resultsQuery = useAppQuery({ queryKey: ['school', 'results'], queryFn: schoolService.schoolResults });
  const schoolAdminLearnersQuery = useAppQuery({ queryKey: ['school-admin', 'learners', 'subjects-panel'], queryFn: () => schoolService.schoolAdminLearners() });
  const teacherWorkloadQuery = useAppQuery({ queryKey: ['school-admin', 'teacher-workload', 'subjects-panel'], queryFn: schoolService.schoolAdminTeacherWorkload });

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['school', 'subjects'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'teacher-assignments'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'learner-enrollments'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'tasks'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'results'] }),
      queryClient.invalidateQueries({ queryKey: ['school', 'notes'] }),
      queryClient.invalidateQueries({ queryKey: ['school-admin', 'learners'] }),
      queryClient.invalidateQueries({ queryKey: ['school-admin', 'teacher-workload'] }),
    ]);
  };

  const createSubject = useMutation({
    mutationFn: () => schoolService.createSubject({
      subjectCatalogueId: form.subjectCatalogueId || undefined,
      subjectName: form.subjectName,
      phase: form.phase,
      grade: form.grade || undefined,
      gradeRange: form.gradeRange || undefined,
      languageLevel: form.languageLevel || undefined,
      subjectType: form.subjectType || undefined,
      isLanguage: form.isLanguage,
      isCompulsory: form.isCompulsory,
      hodUserId: form.hodUserId || null,
      capsAligned: form.capsAligned,
      active: form.active,
    }),
    onSuccess: async () => {
      setForm(emptyForm);
      setIsEditModalOpen(false);
      setLocalMessage({ tone: 'success', text: 'Subject saved successfully.' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ tone: 'error', text: error instanceof Error ? error.message : 'Could not create subject.' }),
  });

  const updateSubject = useMutation({
    mutationFn: () => schoolService.updateSubject(form.id as string, {
      subjectCatalogueId: form.subjectCatalogueId || undefined,
      subjectName: form.subjectName,
      phase: form.phase,
      grade: form.grade || undefined,
      gradeRange: form.gradeRange || undefined,
      languageLevel: form.languageLevel || undefined,
      subjectType: form.subjectType || undefined,
      isLanguage: form.isLanguage,
      isCompulsory: form.isCompulsory,
      hodUserId: form.hodUserId || null,
      capsAligned: form.capsAligned,
      active: form.active,
    }),
    onSuccess: async () => {
      setForm(emptyForm);
      setIsEditModalOpen(false);
      setLocalMessage({ tone: 'success', text: 'Subject updated successfully.' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ tone: 'error', text: error instanceof Error ? error.message : 'Could not update subject.' }),
  });

  const deactivateSubject = useMutation({
    mutationFn: (subjectId: string) => schoolService.deactivateSubject(subjectId),
    onSuccess: async () => {
      setLocalMessage({ tone: 'success', text: 'Subject deactivated.' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ tone: 'error', text: error instanceof Error ? error.message : 'Could not deactivate subject.' }),
  });

  const addCapsSubjects = useMutation({
    mutationFn: async () => {
      const selectedItems = CAPS_SUBJECT_OPTIONS.filter((item) => capsSelected.includes(item.id));
      if (!selectedItems.length) throw new Error('Select at least one CAPS subject.');
      const teacherHod = assignmentState.hodUserId || '';
      const results = await Promise.allSettled(selectedItems.map((item) => schoolService.createSubject({
        subjectName: item.subjectName,
        phase: item.phase,
        grade: item.grade,
        gradeRange: item.grade,
        languageLevel: item.languageLevel,
        subjectType: item.category,
        isLanguage: item.isLanguage,
        isCompulsory: item.isCompulsory,
        hodUserId: teacherHod || null,
        capsAligned: true,
        active: true,
      })));
      const successes = results.filter((result) => result.status === 'fulfilled').length;
      const failures = results.filter((result) => result.status === 'rejected');
      if (!successes && failures.length) {
        throw new Error(failures[0].status === 'rejected' ? failures[0].reason?.message ?? 'Could not add CAPS subjects.' : 'Could not add CAPS subjects.');
      }
      return { successes, failures: failures.length };
    },
    onSuccess: async ({ successes, failures }) => {
      setCapsSelected([]);
      setIsCapsModalOpen(false);
      setLocalMessage({ tone: failures ? 'neutral' : 'success', text: failures ? `${successes} subject(s) added. ${failures} duplicate or invalid item(s) were skipped.` : `${successes} subject(s) added from the CAPS catalogue.` });
      await refresh();
    },
    onError: (error) => setLocalMessage({ tone: 'error', text: error instanceof Error ? error.message : 'Could not add CAPS subjects.' }),
  });

  const bulkAssign = useMutation({
    mutationFn: async () => {
      if (!assignmentState.subjectIds.length) throw new Error('Select at least one subject.');
      const subjects = (subjectsQuery.data ?? []).filter((item) => assignmentState.subjectIds.includes(item.id));
      const classes = classesQuery.data ?? [];
      const learners = (schoolAdminLearnersQuery.data?.items ?? []) as SchoolAdminLearner[];

      if (assignmentState.hodUserId) {
        await Promise.all(subjects.map((subject) => schoolService.updateSubject(subject.id, {
          subjectCatalogueId: subject.subjectCatalogueId ?? undefined,
          subjectName: subject.subjectName,
          phase: subject.phase,
          grade: subject.grade ?? undefined,
          gradeRange: subject.gradeRange ?? undefined,
          languageLevel: subject.languageLevel ?? undefined,
          subjectType: subject.subjectType ?? undefined,
          isLanguage: subject.isLanguage,
          isCompulsory: subject.isCompulsory,
          hodUserId: assignmentState.hodUserId,
          capsAligned: subject.capsAligned ?? true,
          active: subject.active,
        })));
      }

      if (assignmentState.teacherUserId && assignmentState.classId) {
        const selectedClass = classes.find((item) => item.id === assignmentState.classId);
        await Promise.all(subjects.map((subject) => schoolService.assignTeacher({
          teacherUserId: assignmentState.teacherUserId,
          classId: assignmentState.classId,
          subjectId: subject.id,
          phase: subject.phase,
          grade: selectedClass?.grade ?? subject.grade,
        })));
      }

      if (assignmentState.learnerScope !== 'none') {
        const targets = learners.filter((learner) => {
          if (assignmentState.learnerScope === 'class') {
            const selectedClass = classes.find((item) => item.id === assignmentState.classId);
            return selectedClass ? toClassLabel(learner).toLowerCase() === toClassLabel(selectedClass).toLowerCase() : false;
          }
          return assignmentState.learnerGrade ? (learner.grade ?? '') === assignmentState.learnerGrade : false;
        });
        for (const learner of targets) {
          const learnerClass = classes.find((item) => toClassLabel(item).toLowerCase() === toClassLabel(learner).toLowerCase());
          if (!learnerClass) continue;
          const applicableSubjects = subjects.filter((subject) => !subject.grade || subject.grade === learnerClass.grade || subject.gradeRange?.includes(learnerClass.grade));
          await Promise.all(applicableSubjects.map((subject) => schoolService.enrollLearner({
            learnerUserId: learner.learnerUserId,
            classId: learnerClass.id,
            subjectId: subject.id,
          })));
        }
      }
    },
    onSuccess: async () => {
      setIsBulkAssignOpen(false);
      setAssignmentState(emptyAssignmentState);
      setLocalMessage({ tone: 'success', text: 'Bulk subject assignment completed.' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ tone: 'error', text: error instanceof Error ? error.message : 'Bulk assignment failed.' }),
  });

  const createAssessment = useMutation({
    mutationFn: () => schoolService.createSchoolTask({
      teacherUserId: assessmentState.teacherUserId || undefined,
      classId: assessmentState.classId,
      subjectId: assessmentState.subjectId,
      taskType: 'ASSESSMENT',
      title: assessmentState.title,
      dueAt: toDateTime(assessmentState.dueDate),
      maxMarks: Number(assessmentState.maxMarks),
      term: assessmentState.term,
      grade: classesQuery.data?.find((item) => item.id === assessmentState.classId)?.grade,
      phase: subjectsQuery.data?.find((item) => item.id === assessmentState.subjectId)?.phase,
    }),
    onSuccess: async () => {
      setAssessmentState(emptyAssessment);
      setLocalMessage({ tone: 'success', text: 'Assessment created successfully.' });
      await refresh();
    },
    onError: (error) => setLocalMessage({ tone: 'error', text: error instanceof Error ? error.message : 'Could not create assessment.' }),
  });

  const subjects = subjectsQuery.data ?? [];
  const teachers = (teachersQuery.data ?? []) as SchoolAdminUser[];
  const classes = classesQuery.data ?? [];
  const teacherAssignments = (teacherAssignmentsQuery.data ?? []) as TeacherAssignment[];
  const learnerEnrollments = learnerEnrollmentsQuery.data ?? [];
  const atpTopics = atpTopicsQuery.data ?? [];
  const notes = notesQuery.data ?? [];
  const tasks = tasksQuery.data ?? [];
  const results = resultsQuery.data ?? [];
  const workloadItems = (teacherWorkloadQuery.data?.items ?? []) as TeacherWorkloadItem[];

  const teacherById = useMemo(() => Object.fromEntries(teachers.map((item) => [item.userId, item])), [teachers]);
  const classById = useMemo(() => Object.fromEntries(classes.map((item) => [item.id, item])), [classes]);
  const workloadByTeacherId = useMemo(() => Object.fromEntries(workloadItems.map((item) => [item.teacherUserId, item])), [workloadItems]);
  const tasksById = useMemo(() => Object.fromEntries(tasks.map((item) => [item.id, item])), [tasks]);

  const subjectMetrics = useMemo(() => {
    const assignmentBySubject = teacherAssignments.reduce<Record<string, TeacherAssignment[]>>((acc, item) => {
      (acc[item.subjectId] ??= []).push(item);
      return acc;
    }, {});
    const enrollmentsBySubject = learnerEnrollments.reduce<Record<string, number>>((acc, item) => {
      acc[item.subjectId] = (acc[item.subjectId] ?? 0) + 1;
      return acc;
    }, {});
    const atpBySubject = subjects.reduce<Record<string, number>>((acc, subject) => {
      acc[subject.id] = atpTopics.filter((topic) => (subject.subjectCatalogueId && topic.subjectCatalogueId === subject.subjectCatalogueId)
        || (!subject.subjectCatalogueId && topic.subjectName.toLowerCase() === subject.subjectName.toLowerCase() && (!subject.grade || topic.grade === subject.grade))).length;
      return acc;
    }, {});
    const assessmentsBySubject = tasks.reduce<Record<string, number>>((acc, task) => {
      if (['ASSESSMENT', 'TEST', 'EXAM', 'QUIZ', 'CAT', 'SBA'].includes(task.taskType)) {
        acc[task.subjectId] = (acc[task.subjectId] ?? 0) + 1;
      }
      return acc;
    }, {});
    const resultsBySubject = results.reduce<Record<string, Array<number>>>((acc, result) => {
      const task = tasksById[result.taskId];
      if (!task || result.marks == null || task.maxMarks <= 0) return acc;
      const percent = (Number(result.marks) / Number(task.maxMarks)) * 100;
      (acc[task.subjectId] ??= []).push(percent);
      return acc;
    }, {});
    const notesBySubject = notes.reduce<Record<string, SubjectResourceBreakdown>>((acc, note) => {
      if (!note.subjectId) return acc;
      const item = acc[note.subjectId] ?? { notes: 0, worksheets: 0, pastPapers: 0, memorandums: 0, videos: 0, presentations: 0, pdfResources: 0 };
      const descriptor = `${note.title ?? ''} ${note.noteText ?? ''}`.toLowerCase();
      item.notes += 1;
      if (note.pdfUrl) item.pdfResources += 1;
      if (descriptor.includes('worksheet')) item.worksheets += 1;
      if (descriptor.includes('past paper')) item.pastPapers += 1;
      if (descriptor.includes('memo')) item.memorandums += 1;
      if (descriptor.includes('presentation') || descriptor.includes('slides')) item.presentations += 1;
      acc[note.subjectId] = item;
      return acc;
    }, {});

    const insights: SubjectInsight[] = subjects.map((subject) => {
      const marks = resultsBySubject[subject.id] ?? [];
      const averageMark = marks.length ? marks.reduce((sum, mark) => sum + mark, 0) / marks.length : null;
      const passRate = marks.length ? (marks.filter((mark) => mark >= 50).length / marks.length) * 100 : null;
      const distinctionRate = marks.length ? (marks.filter((mark) => mark >= 75).length / marks.length) * 100 : null;
      const learnersAtRisk = marks.filter((mark) => mark < 40).length;
      const learnerCount = enrollmentsBySubject[subject.id] ?? 0;
      const assessmentCount = assessmentsBySubject[subject.id] ?? 0;
      const assessmentCompletionRate = learnerCount && assessmentCount ? Math.min(100, ((marks.length) / (learnerCount * assessmentCount)) * 100) : null;
      let riskLevel: SubjectInsight['riskLevel'] = 'Healthy';
      if ((passRate != null && passRate < 50) || learnersAtRisk >= 5) riskLevel = 'Critical';
      else if ((passRate != null && passRate < 70) || learnersAtRisk > 0 || !assessmentCount) riskLevel = 'Attention';
      return {
        subjectId: subject.id,
        subjectName: subject.subjectName,
        averageMark,
        passRate,
        distinctionRate,
        learnersAtRisk,
        assessmentCompletionRate,
        attendanceTrend: null,
        riskLevel,
      };
    });

    return { assignmentBySubject, enrollmentsBySubject, atpBySubject, assessmentsBySubject, resultsBySubject, notesBySubject, insights };
  }, [learnerEnrollments, notes, results, subjects, tasks, tasksById, teacherAssignments, atpTopics]);

  const filteredCapsOptions = useMemo(() => CAPS_SUBJECT_OPTIONS.filter((item) =>
    item.phaseGroup === capsPhaseGroup
    && item.grade === capsGrade
    && item.category === capsCategory
    && `${item.subjectName} ${item.languageLevel ?? ''}`.toLowerCase().includes(capsSearch.trim().toLowerCase())), [capsCategory, capsGrade, capsPhaseGroup, capsSearch]);

  const filteredSubjects = useMemo(() => subjects.filter((item) => {
    const assignments = subjectMetrics.assignmentBySubject[item.id] ?? [];
    const insight = subjectMetrics.insights.find((entry) => entry.subjectId === item.id);
    const hasAtp = (subjectMetrics.atpBySubject[item.id] ?? 0) > 0;
    const teacherMatch = !teacherFilter || assignments.some((assignment) => assignment.teacherUserId === teacherFilter);
    const hodMatch = !hodFilter || item.hodUserId === hodFilter;
    const searchHaystack = `${item.subjectName} ${item.phase} ${item.grade ?? ''} ${item.gradeRange ?? ''} ${item.languageLevel ?? ''} ${item.subjectType ?? ''}`.toLowerCase();
    const categoryMatch = !categoryFilter || (item.subjectType ?? '').toLowerCase() === categoryFilter.toLowerCase();
    const languageMatch = !languageFilter || (item.languageLevel ?? '').toLowerCase() === languageFilter.toLowerCase();
    const assignedMatch = assignmentFilter === 'All'
      || (assignmentFilter === 'Assigned' && assignments.length > 0)
      || (assignmentFilter === 'Unassigned' && assignments.length === 0);
    const atpMatch = atpFilter === 'All'
      || (atpFilter === 'Available' && hasAtp)
      || (atpFilter === 'Missing' && !hasAtp);
    const riskMatch = riskFilter === 'All' || insight?.riskLevel === riskFilter;
    const compulsoryMatch = compulsoryFilter === 'All'
      || (compulsoryFilter === 'Compulsory' && item.isCompulsory)
      || (compulsoryFilter === 'Elective' && !item.isCompulsory);
    const statusMatch = statusFilter === 'ALL' || (statusFilter === 'ACTIVE' ? item.active : !item.active);
    const gradeMatch = !gradeFilter || item.grade === gradeFilter || item.gradeRange?.includes(gradeFilter);
    const phaseMatch = phaseFilter === 'All' || item.phase === phaseFilter;
    return searchHaystack.includes(search.trim().toLowerCase())
      && teacherMatch
      && hodMatch
      && categoryMatch
      && languageMatch
      && assignedMatch
      && atpMatch
      && riskMatch
      && compulsoryMatch
      && statusMatch
      && gradeMatch
      && phaseMatch;
  }), [assignmentFilter, atpFilter, categoryFilter, gradeFilter, hodFilter, languageFilter, phaseFilter, riskFilter, search, statusFilter, subjects, subjectMetrics.assignmentBySubject, subjectMetrics.atpBySubject, subjectMetrics.insights, teacherFilter, compulsoryFilter]);

  const selectedSubjects = useMemo(() => subjects.filter((item) => selectedSubjectIds.includes(item.id)), [selectedSubjectIds, subjects]);
  const viewedSubject = subjects.find((item) => item.id === viewSubjectId) ?? null;

  const languages = useMemo(() => Array.from(new Set(subjects.map((item) => item.languageLevel).filter(Boolean))) as string[], [subjects]);

  const setAllVisibleSelected = () => {
    setSelectedSubjectIds((current) => {
      const visibleIds = filteredSubjects.map((item) => item.id);
      const allSelected = visibleIds.every((id) => current.includes(id));
      return allSelected
        ? current.filter((id) => !visibleIds.includes(id))
        : Array.from(new Set([...current, ...visibleIds]));
    });
  };

  const openEditModal = (subject?: SchoolSubjectManagementView) => {
    if (!subject) {
      setForm(emptyForm);
    } else {
      setForm({
        id: subject.id,
        subjectCatalogueId: subject.subjectCatalogueId ?? '',
        subjectName: subject.subjectName,
        phase: subject.phase,
        grade: subject.grade ?? '',
        gradeRange: subject.gradeRange ?? '',
        languageLevel: subject.languageLevel ?? '',
        subjectType: subject.subjectType ?? '',
        isLanguage: Boolean(subject.isLanguage),
        isCompulsory: Boolean(subject.isCompulsory),
        hodUserId: subject.hodUserId ?? '',
        capsAligned: subject.capsAligned ?? true,
        active: subject.active,
      });
    }
    setIsEditModalOpen(true);
  };

  const openAssignModal = (subjectIds: string[]) => {
    setAssignmentState((current) => ({ ...emptyAssignmentState, subjectIds, teacherUserId: current.teacherUserId, hodUserId: current.hodUserId }));
    setIsBulkAssignOpen(true);
  };

  const exportSubjectsCsv = () => {
    const rows = [
      ['Subject', 'Phase', 'Grade', 'Language', 'HOD', 'Teachers', 'Classes', 'Learners', 'ATP Status', 'SBA Status', 'Pass Rate', 'Risk Level', 'Status'],
      ...filteredSubjects.map((subject) => {
        const assignments = subjectMetrics.assignmentBySubject[subject.id] ?? [];
        const classCount = new Set(assignments.map((item) => item.classId)).size;
        const insight = subjectMetrics.insights.find((entry) => entry.subjectId === subject.id);
        return [
          subject.subjectName,
          subject.phase,
          subject.grade ?? subject.gradeRange ?? '',
          subject.languageLevel ?? '',
          subject.hodUserId ? teacherById[subject.hodUserId]?.fullName ?? '' : '',
          String(subject.assignedTeacherCount),
          String(classCount),
          String(subject.learnerCount),
          (subjectMetrics.atpBySubject[subject.id] ?? 0) > 0 ? 'Available' : 'Missing',
          (subjectMetrics.assessmentsBySubject[subject.id] ?? 0) > 0 ? 'Configured' : 'Missing',
          formatPercent(insight?.passRate ?? null),
          insight?.riskLevel ?? 'Healthy',
          subject.active ? 'Active' : 'Inactive',
        ];
      }),
    ];
    downloadBlob('subjects-report.csv', 'text/csv;charset=utf-8', rows.map((row) => row.map(csvEscape).join(',')).join('\n'));
  };

  if ([
    subjectSummaryQuery,
    subjectCatalogueQuery,
    subjectsQuery,
    teachersQuery,
    classesQuery,
    teacherAssignmentsQuery,
    learnerEnrollmentsQuery,
    atpTopicsQuery,
    notesQuery,
    tasksQuery,
    resultsQuery,
    schoolAdminLearnersQuery,
    teacherWorkloadQuery,
  ].some((query) => query.isLoading)) {
    return <LoadingState message="Loading subject management..." />;
  }

  if ([
    subjectSummaryQuery,
    subjectCatalogueQuery,
    subjectsQuery,
    teachersQuery,
    classesQuery,
    teacherAssignmentsQuery,
    learnerEnrollmentsQuery,
    atpTopicsQuery,
    notesQuery,
    tasksQuery,
    resultsQuery,
    schoolAdminLearnersQuery,
    teacherWorkloadQuery,
  ].some((query) => query.isError)) {
    return <ErrorState message="Unable to load the subjects management module." />;
  }

  const resetFilters = () => {
    setSearch('');
    setPhaseFilter('All');
    setGradeFilter('');
    setTeacherFilter('');
    setHodFilter('');
    setAssignmentFilter('All');
    setAtpFilter('All');
    setRiskFilter('All');
    setStatusFilter('ALL');
    setLanguageFilter('');
    setCategoryFilter('');
    setCompulsoryFilter('All');
  };

  return (
    <AdminPageLayout>
      {localMessage ? (
        <div className={`rounded-2xl border px-4 py-3 text-sm ${
          localMessage.tone === 'success' ? 'border-emerald-200 bg-emerald-50 text-emerald-800'
            : localMessage.tone === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700'
              : 'border-slate-200 bg-slate-50 text-slate-700'
        }`}>
          {localMessage.text}
        </div>
      ) : null}

      <AdminPageHeader
        title="Subjects"
        subtitle="Manage all subjects and track curriculum configuration, readiness, and performance."
        actions={
          <>
            <Button type="button" className="h-11 rounded-2xl bg-[#0B5BFF] px-4 hover:bg-[#0849cb]" onClick={() => openEditModal()}>Add Subject</Button>
            <Button type="button" className="h-11 rounded-2xl bg-white px-4 text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50" onClick={() => setIsCapsModalOpen(true)}>Import Subjects</Button>
            <Button type="button" className="h-11 rounded-2xl bg-white px-4 text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50" onClick={exportSubjectsCsv}>Export</Button>
          </>
        }
      />

      <section className="overflow-hidden rounded-[16px] border border-[#e5edf7] bg-white shadow-sm shadow-slate-200/40">
        <div className="border-b border-slate-200 px-5 py-5">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <h3 className="text-[20px] font-semibold text-slate-950">Subjects ({filteredSubjects.length})</h3>
              <p className="mt-1 text-[14px] text-slate-500">{filteredSubjects.length} subject(s) match the current filters</p>
            </div>
            <div className="relative w-full lg:max-w-[280px]">
              <Search className="pointer-events-none absolute right-4 top-3.5 h-4 w-4 text-slate-400" />
              <Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search subjects..." className="h-11 rounded-[14px] border-slate-200 pr-10 text-[14px]" />
            </div>
          </div>
          <div className="mt-4 grid gap-3 md:grid-cols-3 xl:grid-cols-7">
            <select className="h-11 rounded-[14px] border border-slate-200 px-3 text-[14px]" value={phaseFilter} onChange={(e) => setPhaseFilter(e.target.value as (typeof phases)[number])}>
              {phases.map((phase) => <option key={phase} value={phase}>{phase === 'All' ? 'All phases' : phase}</option>)}
            </select>
            <select className="h-11 rounded-[14px] border border-slate-200 px-3 text-[14px]" value={gradeFilter} onChange={(e) => setGradeFilter(e.target.value)}>
              <option value="">All grades</option>
              {gradeOptions.map((grade) => <option key={grade} value={grade}>{grade}</option>)}
            </select>
            <select className="h-11 rounded-[14px] border border-slate-200 px-3 text-[14px]" value={languageFilter} onChange={(e) => setLanguageFilter(e.target.value)}>
              <option value="">All languages</option>
              {languages.map((language) => <option key={language} value={language}>{language}</option>)}
            </select>
            <select className="h-11 rounded-[14px] border border-slate-200 px-3 text-[14px]" value={hodFilter} onChange={(e) => setHodFilter(e.target.value)}>
              <option value="">All HODs</option>
              {teachers.map((teacher) => <option key={teacher.userId} value={teacher.userId}>{teacher.fullName}</option>)}
            </select>
            <select className="h-11 rounded-[14px] border border-slate-200 px-3 text-[14px]" value={atpFilter} onChange={(e) => setAtpFilter(e.target.value as (typeof atpFilters)[number])}>
              {atpFilters.map((item) => <option key={item} value={item}>{item === 'All' ? 'All ATP states' : item}</option>)}
            </select>
            <select className="h-11 rounded-[14px] border border-slate-200 px-3 text-[14px]" value={riskFilter} onChange={(e) => setRiskFilter(e.target.value as (typeof riskLevels)[number])}>
              {riskLevels.map((item) => <option key={item} value={item}>{item === 'All' ? 'All risk levels' : item}</option>)}
            </select>
            <Button type="button" className="h-11 rounded-[14px] bg-white px-4 text-[14px] text-slate-700 ring-1 ring-slate-200 hover:bg-slate-50" onClick={resetFilters}>
              Clear Filters
            </Button>
          </div>
        </div>

        <div className="hidden grid-cols-[2fr_1fr_1fr_1fr_0.65fr_0.65fr_0.7fr_0.9fr_0.9fr_0.7fr_0.8fr_1fr] gap-4 px-5 py-4 text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500 xl:grid">
          <span>Subject</span>
          <span>Phase / Grade</span>
          <span>Language</span>
          <span>HOD</span>
          <span>Teachers</span>
          <span>Classes</span>
          <span>Learners</span>
          <span>ATP Status</span>
          <span>SBA Status</span>
          <span>Pass Rate</span>
          <span>Risk Level</span>
          <span>Actions</span>
        </div>
        <div className="divide-y divide-slate-100">
          {filteredSubjects.map((subject) => {
            const assignments = subjectMetrics.assignmentBySubject[subject.id] ?? [];
            const classCount = new Set(assignments.map((item) => item.classId)).size;
            const insight = subjectMetrics.insights.find((entry) => entry.subjectId === subject.id);
            const resourceCounts = subjectMetrics.notesBySubject[subject.id] ?? { notes: 0, worksheets: 0, pastPapers: 0, memorandums: 0, videos: 0, presentations: 0, pdfResources: 0 };
            const workloadWarning = assignments.some((assignment) => (workloadByTeacherId[assignment.teacherUserId]?.workloadBand ?? '').toLowerCase() === 'high');
            return (
              <div key={subject.id}>
                <div className="hidden xl:hidden">
                  <div className="hidden gap-4 px-5 py-4 xl:grid xl:grid-cols-[2fr_1fr_1fr_1fr_0.65fr_0.65fr_0.7fr_0.9fr_0.9fr_0.7fr_0.8fr_1fr] xl:items-center">
                  <div>
                    <p className="text-[15px] font-semibold text-slate-950">{subject.subjectName}</p>
                    <p className="mt-1 text-xs text-slate-500">{subject.subjectType || 'General'}{subject.isCompulsory ? ' · Compulsory' : ' · Elective'}{subject.capsAligned === false ? ' · Review CAPS alignment' : ' · CAPS aligned'}</p>
                  </div>
                  <div className="text-sm text-slate-700">{subject.phase}<div className="text-xs text-slate-500">{subject.grade || subject.gradeRange || 'Not set'}</div></div>
                  <div className="text-sm text-slate-700">{subject.languageLevel || (subject.isLanguage ? 'Language subject' : 'Not applicable')}</div>
                  <div className="text-sm text-slate-700">{subject.hodUserId ? teacherById[subject.hodUserId]?.fullName ?? 'Assigned' : 'Unassigned'}</div>
                  <div className="text-sm text-slate-700">{subject.assignedTeacherCount}</div>
                  <div className="text-sm text-slate-700">{classCount}</div>
                  <div className="text-sm text-slate-700">{subject.learnerCount}</div>
                  <div className="text-sm text-slate-700">{(subjectMetrics.atpBySubject[subject.id] ?? 0) > 0 ? 'Available' : 'Missing'}</div>
                  <div className="text-sm text-slate-700">{(subjectMetrics.assessmentsBySubject[subject.id] ?? 0) > 0 ? 'Configured' : 'Missing'}</div>
                  <div className="text-sm text-slate-700">{formatPercent(insight?.passRate ?? null)}</div>
                  <div><span className={`rounded-full px-2.5 py-1 text-xs font-medium ${toneClasses[insight?.riskLevel ?? 'Healthy']}`}>{insight?.riskLevel ?? 'Healthy'}</span></div>
                  <div><span className={`rounded-full px-2.5 py-1 text-xs font-medium ${subject.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-700'}`}>{subject.active ? 'Active' : 'Inactive'}</span></div>
                  <div className="flex flex-wrap gap-2">
                    <Button type="button" className="h-9 rounded-xl bg-slate-900 px-3 text-xs hover:bg-slate-800" onClick={() => setViewSubjectId(subject.id)}>View</Button>
                    <Button type="button" className="h-9 rounded-xl bg-white px-3 text-xs text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50" onClick={() => openEditModal(subject)}>Edit</Button>
                    <Button type="button" className="h-9 rounded-xl bg-white px-3 text-xs text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50" onClick={() => openAssignModal([subject.id])}>Assign Teacher</Button>
                    <Button type="button" className="h-9 rounded-xl bg-white px-3 text-xs text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50" onClick={() => openAssignModal([subject.id])}>Assign HOD</Button>
                    <Button type="button" className="h-9 rounded-xl bg-white px-3 text-xs text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50" onClick={() => setUploadState({ subjectId: subject.id, file: null })}>Upload ATP</Button>
                    <Button type="button" className="h-9 rounded-xl bg-white px-3 text-xs text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50" onClick={() => setAssessmentState({ ...emptyAssessment, subjectId: subject.id })}>Create Assessment</Button>
                    <Button type="button" className="h-9 rounded-xl bg-[#0B5BFF] px-3 text-xs hover:bg-[#0849cb]" onClick={() => setAiState({ subjectId: subject.id, action: 'lesson-plan' })}>Generate AI Lesson Plan</Button>
                    <Button type="button" className="h-9 rounded-xl bg-amber-500 px-3 text-xs hover:bg-amber-600" disabled={!subject.active || deactivateSubject.isPending} onClick={() => deactivateSubject.mutate(subject.id)}>Deactivate</Button>
                  </div>
                  </div>
                </div>

                <div className="hidden xl:grid xl:grid-cols-[2fr_1fr_1fr_1fr_0.65fr_0.65fr_0.7fr_0.9fr_0.9fr_0.7fr_0.8fr_1fr] gap-4 px-5 py-4 xl:items-center">
                  <div>
                    <p className="text-[15px] font-semibold text-slate-950">{subject.subjectName}</p>
                    <p className="mt-1 text-[13px] text-slate-500">{subject.subjectType || 'General'}{subject.isCompulsory ? ' · Compulsory' : ' · Elective'}{subject.capsAligned === false ? ' · Review CAPS alignment' : ' · CAPS aligned'}</p>
                  </div>
                  <div className="text-[14px] text-slate-700">{subject.phase}<div className="text-[13px] text-slate-500">{subject.grade || subject.gradeRange || 'Not set'}</div></div>
                  <div className="text-[14px] text-slate-700">{subject.languageLevel || (subject.isLanguage ? 'Language subject' : 'Not applicable')}</div>
                  <div className="text-[14px] text-slate-700">{subject.hodUserId ? teacherById[subject.hodUserId]?.fullName ?? 'Assigned' : 'Unassigned'}</div>
                  <div className="text-[14px] text-slate-700">{subject.assignedTeacherCount}</div>
                  <div className="text-[14px] text-slate-700">{classCount}</div>
                  <div className="text-[14px] text-slate-700">{subject.learnerCount}</div>
                  <div><span className={`rounded-full px-2.5 py-1 text-[12px] font-semibold ${(subjectMetrics.atpBySubject[subject.id] ?? 0) > 0 ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>{(subjectMetrics.atpBySubject[subject.id] ?? 0) > 0 ? 'Available' : 'Missing'}</span></div>
                  <div><span className={`rounded-full px-2.5 py-1 text-[12px] font-semibold ${(subjectMetrics.assessmentsBySubject[subject.id] ?? 0) > 0 ? 'bg-blue-50 text-blue-700' : 'bg-rose-50 text-rose-700'}`}>{(subjectMetrics.assessmentsBySubject[subject.id] ?? 0) > 0 ? 'Configured' : 'Missing'}</span></div>
                  <div className="text-[14px] text-slate-700">{formatPercent(insight?.passRate ?? null)}</div>
                  <div><span className={`rounded-full px-2.5 py-1 text-[12px] font-semibold ${toneClasses[insight?.riskLevel ?? 'Healthy']}`}>{insight?.riskLevel ?? 'Healthy'}</span></div>
                  <div className="flex items-center gap-2">
                    <AdminActionButton type="button" onClick={() => setViewSubjectId(subject.id)}>View</AdminActionButton>
                    <AdminActionButton type="button" onClick={() => openAssignModal([subject.id])}>Assign</AdminActionButton>
                    <AdminActionButton type="button" variant="ghost" className="w-9 px-0" onClick={() => openEditModal(subject)} aria-label={`More actions for ${subject.subjectName}`}>
                      <MoreHorizontal className="h-4 w-4" />
                    </AdminActionButton>
                  </div>
                </div>

                <div className="hidden xl:hidden">
                  <div className="space-y-3 px-4 py-4 xl:hidden">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-base font-semibold text-slate-900">{subject.subjectName}</p>
                      <p className="mt-1 text-sm text-slate-500">{subject.phase} · {subject.grade || subject.gradeRange || 'Not set'}</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={selectedSubjectIds.includes(subject.id)}
                      onChange={(e) => setSelectedSubjectIds((current) => e.target.checked ? Array.from(new Set([...current, subject.id])) : current.filter((id) => id !== subject.id))}
                    />
                  </div>
                  <div className="grid gap-2 sm:grid-cols-2">
                    <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Language:</span> {subject.languageLevel || 'Not applicable'}</p>
                    <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">HOD:</span> {subject.hodUserId ? teacherById[subject.hodUserId]?.fullName ?? 'Assigned' : 'Unassigned'}</p>
                    <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Teachers:</span> {subject.assignedTeacherCount}</p>
                    <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Learners:</span> {subject.learnerCount}</p>
                    <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">ATP:</span> {(subjectMetrics.atpBySubject[subject.id] ?? 0) > 0 ? 'Available' : 'Missing'}</p>
                    <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Pass rate:</span> {formatPercent(insight?.passRate ?? null)}</p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${toneClasses[insight?.riskLevel ?? 'Healthy']}`}>{insight?.riskLevel ?? 'Healthy'}</span>
                    {workloadWarning ? <span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-700">Teacher workload warning</span> : null}
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${subject.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-700'}`}>{subject.active ? 'Active' : 'Inactive'}</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button type="button" className="h-9 rounded-xl bg-slate-900 px-3 text-xs hover:bg-slate-800" onClick={() => setViewSubjectId(subject.id)}>View</Button>
                    <Button type="button" className="h-9 rounded-xl bg-white px-3 text-xs text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50" onClick={() => openEditModal(subject)}>Edit</Button>
                    <Button type="button" className="h-9 rounded-xl bg-white px-3 text-xs text-slate-900 ring-1 ring-slate-200 hover:bg-slate-50" onClick={() => openAssignModal([subject.id])}>Assign</Button>
                    <Button type="button" className="h-9 rounded-xl bg-[#0B5BFF] px-3 text-xs hover:bg-[#0849cb]" onClick={() => setAiState({ subjectId: subject.id, action: 'lesson-plan' })}>AI</Button>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3 text-xs text-slate-600">
                    Resource centre: {resourceCounts.notes} notes, {resourceCounts.pdfResources} PDFs, {resourceCounts.worksheets} worksheets, {resourceCounts.memorandums} memorandums.
                  </div>
                  </div>
                </div>

                <div className="space-y-3 px-4 py-4 xl:hidden">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-[15px] font-semibold text-slate-950">{subject.subjectName}</p>
                      <p className="mt-1 text-[13px] text-slate-500">{subject.phase} · {subject.grade || subject.gradeRange || 'Not set'}</p>
                    </div>
                    <span className={`rounded-full px-2.5 py-1 text-[12px] font-semibold ${subject.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-700'}`}>{subject.active ? 'Active' : 'Inactive'}</span>
                  </div>
                  <div className="grid gap-2 sm:grid-cols-2">
                    <p className="text-[14px] text-slate-700"><span className="font-medium text-slate-900">Language:</span> {subject.languageLevel || 'Not applicable'}</p>
                    <p className="text-[14px] text-slate-700"><span className="font-medium text-slate-900">HOD:</span> {subject.hodUserId ? teacherById[subject.hodUserId]?.fullName ?? 'Assigned' : 'Unassigned'}</p>
                    <p className="text-[14px] text-slate-700"><span className="font-medium text-slate-900">Teachers:</span> {subject.assignedTeacherCount}</p>
                    <p className="text-[14px] text-slate-700"><span className="font-medium text-slate-900">Learners:</span> {subject.learnerCount}</p>
                    <p className="text-[14px] text-slate-700"><span className="font-medium text-slate-900">ATP:</span> {(subjectMetrics.atpBySubject[subject.id] ?? 0) > 0 ? 'Available' : 'Missing'}</p>
                    <p className="text-[14px] text-slate-700"><span className="font-medium text-slate-900">Pass rate:</span> {formatPercent(insight?.passRate ?? null)}</p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <span className={`rounded-full px-2.5 py-1 text-[12px] font-semibold ${toneClasses[insight?.riskLevel ?? 'Healthy']}`}>{insight?.riskLevel ?? 'Healthy'}</span>
                    {workloadWarning ? <span className="rounded-full bg-amber-50 px-2.5 py-1 text-[12px] font-semibold text-amber-700">Teacher workload warning</span> : null}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <AdminActionButton type="button" onClick={() => setViewSubjectId(subject.id)}>View</AdminActionButton>
                    <AdminActionButton type="button" onClick={() => openAssignModal([subject.id])}>Assign</AdminActionButton>
                    <AdminActionButton type="button" variant="ghost" onClick={() => openEditModal(subject)}>More</AdminActionButton>
                  </div>
                  <div className="rounded-[14px] border border-slate-200 bg-slate-50 p-3 text-[12px] text-slate-600">
                    Resource centre: {resourceCounts.notes} notes, {resourceCounts.pdfResources} PDFs, {resourceCounts.worksheets} worksheets, {resourceCounts.memorandums} memorandums.
                  </div>
                </div>
              </div>
            );
          })}
          {!filteredSubjects.length ? <div className="p-6"><EmptyState title="No subjects found" message="No subjects match the current search and filters." /></div> : null}
        </div>
      </section>

      <AdminCard>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h3 className="text-[18px] font-semibold text-slate-950">Subject Performance Insights</h3>
            <p className="mt-1 text-[14px] text-slate-500">A compact readiness view of curriculum setup, assessment coverage, and risk across the current subject register.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <AdminActionButton type="button" onClick={setAllVisibleSelected}>
              {filteredSubjects.every((subject) => selectedSubjectIds.includes(subject.id)) ? 'Clear visible selection' : 'Select visible'}
            </AdminActionButton>
            <AdminActionButton type="button" onClick={() => setIsBulkAssignOpen(true)} disabled={!selectedSubjectIds.length}>
              Bulk assign
            </AdminActionButton>
          </div>
        </div>
        <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <div className="rounded-[14px] border border-slate-200 bg-slate-50 p-4">
            <p className="text-[13px] font-semibold text-slate-600">Curriculum Configured</p>
            <p className="mt-2 text-[24px] font-bold text-slate-950">{filteredSubjects.filter((subject) => (subjectMetrics.assessmentsBySubject[subject.id] ?? 0) > 0).length}</p>
            <p className="mt-2 text-[13px] text-slate-500">Subjects with at least one SBA assessment configured.</p>
          </div>
          <div className="rounded-[14px] border border-slate-200 bg-slate-50 p-4">
            <p className="text-[13px] font-semibold text-slate-600">ATP Available</p>
            <p className="mt-2 text-[24px] font-bold text-slate-950">{filteredSubjects.filter((subject) => (subjectMetrics.atpBySubject[subject.id] ?? 0) > 0).length}</p>
            <p className="mt-2 text-[13px] text-slate-500">Subjects already carrying ATP seed data or uploads.</p>
          </div>
          <div className="rounded-[14px] border border-slate-200 bg-slate-50 p-4">
            <p className="text-[13px] font-semibold text-slate-600">Attention Needed</p>
            <p className="mt-2 text-[24px] font-bold text-slate-950">{filteredSubjects.filter((subject) => {
              const risk = subjectMetrics.insights.find((entry) => entry.subjectId === subject.id)?.riskLevel ?? 'Healthy';
              return risk !== 'Healthy';
            }).length}</p>
            <p className="mt-2 text-[13px] text-slate-500">Subjects currently flagged for attention or critical follow-up.</p>
          </div>
          <div className="rounded-[14px] border border-slate-200 bg-slate-50 p-4">
            <p className="text-[13px] font-semibold text-slate-600">Average Pass Rate</p>
            <p className="mt-2 text-[24px] font-bold text-slate-950">
              {(() => {
                const rates = filteredSubjects
                  .map((subject) => subjectMetrics.insights.find((entry) => entry.subjectId === subject.id)?.passRate ?? null)
                  .filter((value): value is number => value != null);
                if (!rates.length) return '—';
                return `${Math.round(rates.reduce((sum, value) => sum + value, 0) / rates.length)}%`;
              })()}
            </p>
            <p className="mt-2 text-[13px] text-slate-500">Calculated from available subject result summaries.</p>
          </div>
        </div>
      </AdminCard>

      {isEditModalOpen ? (
        <SubjectEditModal
          form={form}
          setForm={setForm}
          teachers={teachers}
          catalogue={subjectCatalogueQuery.data ?? []}
          onClose={() => { setIsEditModalOpen(false); setForm(emptyForm); }}
          onSave={() => { if (form.id) updateSubject.mutate(); else createSubject.mutate(); }}
          saving={createSubject.isPending || updateSubject.isPending}
        />
      ) : null}

      {isCapsModalOpen ? (
        <ModalShell title="Add from CAPS Catalogue" subtitle="Select CAPS subjects through a guided phase, grade, category, and searchable multi-select flow." onClose={() => setIsCapsModalOpen(false)}>
          <div className="grid gap-4 lg:grid-cols-4">
            <label className="space-y-2">
              <FieldLabel>Phase</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={capsPhaseGroup} onChange={(e) => { const next = e.target.value as CapsPhaseGroup; setCapsPhaseGroup(next); setCapsGrade(CAPS_PHASE_GROUPS.find((item) => item.value === next)?.grades[0] ?? 'Grade 10'); setCapsCategory(CAPS_CATEGORY_OPTIONS[next][0]); }}>
                {CAPS_PHASE_GROUPS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
              </select>
            </label>
            <label className="space-y-2">
              <FieldLabel>Grade</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={capsGrade} onChange={(e) => setCapsGrade(e.target.value)}>
                {(CAPS_PHASE_GROUPS.find((item) => item.value === capsPhaseGroup)?.grades ?? []).map((grade) => <option key={grade} value={grade}>{grade}</option>)}
              </select>
            </label>
            <label className="space-y-2">
              <FieldLabel>Subject category</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={capsCategory} onChange={(e) => setCapsCategory(e.target.value)}>
                {CAPS_CATEGORY_OPTIONS[capsPhaseGroup].map((category) => <option key={category} value={category}>{category}</option>)}
              </select>
            </label>
            <label className="space-y-2">
              <FieldLabel>Search subjects</FieldLabel>
              <Input value={capsSearch} onChange={(e) => setCapsSearch(e.target.value)} placeholder="Search within CAPS subjects" className="h-11 rounded-2xl" />
            </label>
          </div>
          <div className="mt-5 grid max-h-[420px] gap-3 overflow-y-auto pr-1 md:grid-cols-2 xl:grid-cols-3">
            {filteredCapsOptions.map((item) => {
              const selected = capsSelected.includes(item.id);
              const duplicateExists = subjects.some((subject) =>
                subject.subjectName.toLowerCase() === item.subjectName.toLowerCase()
                && subject.phase === item.phase
                && (subject.grade === item.grade || subject.gradeRange === item.grade)
                && (subject.languageLevel ?? '') === (item.languageLevel ?? ''),
              );
              return (
                <button
                  key={item.id}
                  type="button"
                  disabled={duplicateExists}
                  onClick={() => setCapsSelected((current) => selected ? current.filter((id) => id !== item.id) : [...current, item.id])}
                  className={`rounded-[24px] border p-4 text-left transition ${
                    duplicateExists ? 'cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400'
                      : selected ? 'border-[#0B5BFF] bg-blue-50 text-slate-900'
                        : 'border-slate-200 bg-white text-slate-900 hover:border-slate-300'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold">{item.subjectName}</p>
                      <p className="mt-1 text-xs text-slate-500">{item.grade} · {item.category}</p>
                    </div>
                    <input type="checkbox" readOnly checked={selected} />
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2 text-xs">
                    {item.languageLevel ? <span className="rounded-full bg-slate-100 px-2 py-1">{item.languageLevel}</span> : null}
                    <span className="rounded-full bg-slate-100 px-2 py-1">{item.isCompulsory ? 'Compulsory' : 'Elective'}</span>
                    <span className="rounded-full bg-slate-100 px-2 py-1">CAPS aligned</span>
                  </div>
                  {duplicateExists ? <p className="mt-3 text-xs text-amber-700">Already created for this school, grade, and language combination.</p> : null}
                </button>
              );
            })}
            {!filteredCapsOptions.length ? <div className="md:col-span-2 xl:col-span-3"><EmptyState title="No CAPS subjects match" message="Change the grade, category, or search text to see CAPS subject options." /></div> : null}
          </div>
          <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-slate-600">{capsSelected.length} subject(s) selected.</p>
            <div className="flex gap-2">
              <Button type="button" className="rounded-2xl bg-slate-200 px-4 text-slate-800 hover:bg-slate-300" onClick={() => setCapsSelected([])}>Clear</Button>
              <Button type="button" className="rounded-2xl bg-[#0B5BFF] px-5 hover:bg-[#0849cb]" disabled={addCapsSubjects.isPending || !capsSelected.length} onClick={() => addCapsSubjects.mutate()}>
                {addCapsSubjects.isPending ? 'Adding...' : 'Add selected CAPS subjects'}
              </Button>
            </div>
          </div>
        </ModalShell>
      ) : null}

      {isBulkAssignOpen ? (
        <ModalShell title="Bulk Assign Subjects" subtitle="Assign selected subjects to teachers, HODs, classes, and learner groups without duplicating records." onClose={() => setIsBulkAssignOpen(false)}>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <label className="space-y-2">
              <FieldLabel>Selected subjects</FieldLabel>
              <div className="max-h-40 overflow-y-auto rounded-2xl border border-slate-200 p-3 text-sm text-slate-700">
                {selectedSubjects.map((subject) => <p key={subject.id}>{subject.subjectName} · {subject.grade || subject.gradeRange || subject.phase}</p>)}
              </div>
            </label>
            <label className="space-y-2">
              <FieldLabel>Teacher</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={assignmentState.teacherUserId} onChange={(e) => setAssignmentState((current) => ({ ...current, teacherUserId: e.target.value }))}>
                <option value="">No teacher assignment</option>
                {teachers.map((teacher) => <option key={teacher.userId} value={teacher.userId}>{teacher.fullName}</option>)}
              </select>
              {assignmentState.teacherUserId && workloadByTeacherId[assignmentState.teacherUserId] ? (
                <p className={`text-xs ${(workloadByTeacherId[assignmentState.teacherUserId].workloadBand ?? '').toLowerCase() === 'high' ? 'text-amber-700' : 'text-slate-500'}`}>
                  Workload: {workloadByTeacherId[assignmentState.teacherUserId].workloadBand} · {workloadByTeacherId[assignmentState.teacherUserId].subjectsAssigned} subjects · {workloadByTeacherId[assignmentState.teacherUserId].classesAssigned} classes
                </p>
              ) : null}
            </label>
            <label className="space-y-2">
              <FieldLabel>HOD</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={assignmentState.hodUserId} onChange={(e) => setAssignmentState((current) => ({ ...current, hodUserId: e.target.value }))}>
                <option value="">No HOD assignment</option>
                {teachers.map((teacher) => <option key={teacher.userId} value={teacher.userId}>{teacher.fullName}</option>)}
              </select>
            </label>
            <label className="space-y-2">
              <FieldLabel>Class</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={assignmentState.classId} onChange={(e) => setAssignmentState((current) => ({ ...current, classId: e.target.value }))}>
                <option value="">No class selected</option>
                {classes.map((item) => <option key={item.id} value={item.id}>{toClassLabel(item)}</option>)}
              </select>
            </label>
          </div>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <label className="space-y-2">
              <FieldLabel>Learner assignment scope</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={assignmentState.learnerScope} onChange={(e) => setAssignmentState((current) => ({ ...current, learnerScope: e.target.value as AssignmentState['learnerScope'] }))}>
                <option value="none">Do not bulk assign learners</option>
                <option value="class">Assign to all learners in selected class</option>
                <option value="grade">Assign to all learners in selected grade</option>
              </select>
            </label>
            <label className="space-y-2">
              <FieldLabel>Grade for learner assignment</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={assignmentState.learnerGrade} onChange={(e) => setAssignmentState((current) => ({ ...current, learnerGrade: e.target.value }))}>
                <option value="">Select grade</option>
                {gradeOptions.map((grade) => <option key={grade} value={grade}>{grade}</option>)}
              </select>
            </label>
          </div>
          <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
            <p className="font-medium text-slate-900">Bulk assignment rules</p>
            <ul className="mt-2 list-disc pl-5">
              <li>Duplicate teacher assignments are blocked by the existing backend logic.</li>
              <li>Duplicate learner-subject enrollments are skipped safely.</li>
              <li>Compulsory CAPS subjects can be assigned to full grades or classes without removing current subject data.</li>
            </ul>
          </div>
          <div className="mt-6 flex justify-end gap-3">
            <Button type="button" className="rounded-2xl bg-slate-200 px-4 text-slate-800 hover:bg-slate-300" onClick={() => setIsBulkAssignOpen(false)}>Cancel</Button>
            <Button type="button" className="rounded-2xl bg-[#0B5BFF] px-5 hover:bg-[#0849cb]" disabled={bulkAssign.isPending || !assignmentState.subjectIds.length} onClick={() => bulkAssign.mutate()}>
              {bulkAssign.isPending ? 'Assigning...' : 'Apply subject assignments'}
            </Button>
          </div>
        </ModalShell>
      ) : null}

      {viewedSubject ? (
        <ModalShell title={viewedSubject.subjectName} subtitle="View subject intelligence, curriculum readiness, resources, and pathway mapping for this registered school subject." onClose={() => setViewSubjectId(null)}>
          {(() => {
            const insight = subjectMetrics.insights.find((entry) => entry.subjectId === viewedSubject.id);
            const assignments = subjectMetrics.assignmentBySubject[viewedSubject.id] ?? [];
            const resources = subjectMetrics.notesBySubject[viewedSubject.id] ?? { notes: 0, worksheets: 0, pastPapers: 0, memorandums: 0, videos: 0, presentations: 0, pdfResources: 0 };
            const pathways = DEFAULT_CAREER_PATHWAYS[viewedSubject.subjectName] ?? ['Career pathways will be configurable for this subject once pathway mapping is captured for the school.'];
            return (
              <div className="grid gap-5 xl:grid-cols-[1.15fr_0.85fr]">
                <div className="space-y-5">
                  <section className="rounded-[24px] border border-slate-200 bg-slate-50/70 p-5">
                    <h4 className="text-lg font-semibold text-slate-900">Subject Performance Insights</h4>
                    <div className="mt-4 grid gap-3 sm:grid-cols-2">
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Average mark:</span> {insight?.averageMark == null ? 'Not available' : `${insight.averageMark.toFixed(1)}%`}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Pass rate:</span> {formatPercent(insight?.passRate ?? null)}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Distinction rate:</span> {formatPercent(insight?.distinctionRate ?? null)}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Learners at risk:</span> {insight?.learnersAtRisk ?? 0}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Assessment completion:</span> {formatPercent(insight?.assessmentCompletionRate ?? null)}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Attendance trend:</span> {insight?.attendanceTrend ?? 'Performance insights will deepen once attendance is captured.'}</p>
                    </div>
                  </section>
                  <section className="rounded-[24px] border border-slate-200 bg-white p-5">
                    <h4 className="text-lg font-semibold text-slate-900">Curriculum / ATP Management</h4>
                    <div className="mt-4 grid gap-3 sm:grid-cols-2">
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">ATP status:</span> {(subjectMetrics.atpBySubject[viewedSubject.id] ?? 0) > 0 ? 'Available' : 'Not uploaded yet'}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">CAPS alignment:</span> {viewedSubject.capsAligned !== false ? 'Aligned' : 'Review required'}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Term coverage:</span> {(subjectMetrics.atpBySubject[viewedSubject.id] ?? 0) > 0 ? `${subjectMetrics.atpBySubject[viewedSubject.id]} topic(s) seeded` : 'Awaiting ATP upload or seed data'}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Weekly teaching progress:</span> {(subjectMetrics.atpBySubject[viewedSubject.id] ?? 0) > 0 ? 'Trackable once ATP plans are expanded' : 'No ATP sequence yet'}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">SBA task coverage:</span> {(subjectMetrics.assessmentsBySubject[viewedSubject.id] ?? 0)} task(s)</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Assessment plan status:</span> {(subjectMetrics.assessmentsBySubject[viewedSubject.id] ?? 0) > 0 ? 'Configured' : 'Missing assessments'}</p>
                    </div>
                  </section>
                </div>
                <div className="space-y-5">
                  <section className="rounded-[24px] border border-slate-200 bg-white p-5">
                    <h4 className="text-lg font-semibold text-slate-900">Teacher and HOD Allocation</h4>
                    <p className="mt-3 text-sm text-slate-700"><span className="font-medium text-slate-900">HOD:</span> {viewedSubject.hodUserId ? teacherById[viewedSubject.hodUserId]?.fullName ?? 'Assigned' : 'Not assigned'}</p>
                    <div className="mt-3 space-y-2">
                      {assignments.length ? assignments.map((assignment) => (
                        <div key={assignment.id} className="rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
                          {teacherById[assignment.teacherUserId]?.fullName ?? assignment.teacherUserId} · {toClassLabel(classById[assignment.classId] ?? {})}
                        </div>
                      )) : <p className="text-sm text-slate-500">No teacher allocations yet.</p>}
                    </div>
                  </section>
                  <section className="rounded-[24px] border border-slate-200 bg-white p-5">
                    <h4 className="text-lg font-semibold text-slate-900">Subject Resource Centre</h4>
                    <div className="mt-4 grid gap-3 sm:grid-cols-2">
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Notes:</span> {resources.notes}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Worksheets:</span> {resources.worksheets}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Past papers:</span> {resources.pastPapers}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Memorandums:</span> {resources.memorandums}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Videos:</span> {resources.videos}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">Presentations:</span> {resources.presentations}</p>
                      <p className="text-sm text-slate-700"><span className="font-medium text-slate-900">PDF resources:</span> {resources.pdfResources}</p>
                    </div>
                  </section>
                  <section className="rounded-[24px] border border-slate-200 bg-white p-5">
                    <h4 className="text-lg font-semibold text-slate-900">Subject → Career Pathways</h4>
                    <div className="mt-3 flex flex-wrap gap-2">
                      {pathways.map((pathway) => <span key={pathway} className="rounded-full bg-blue-50 px-3 py-1 text-sm text-blue-700">{pathway}</span>)}
                    </div>
                  </section>
                </div>
              </div>
            );
          })()}
        </ModalShell>
      ) : null}

      {assessmentState.subjectId ? (
        <ModalShell title="Create Assessment" subtitle="Create a real assessment against the selected school subject using the existing school task backend." onClose={() => setAssessmentState(emptyAssessment)}>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="space-y-2">
              <FieldLabel>Subject</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={assessmentState.subjectId} onChange={(e) => setAssessmentState((current) => ({ ...current, subjectId: e.target.value }))}>
                <option value="">Select subject</option>
                {subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.subjectName}</option>)}
              </select>
            </label>
            <label className="space-y-2">
              <FieldLabel>Class</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={assessmentState.classId} onChange={(e) => setAssessmentState((current) => ({ ...current, classId: e.target.value }))}>
                <option value="">Select class</option>
                {classes.map((item) => <option key={item.id} value={item.id}>{toClassLabel(item)}</option>)}
              </select>
            </label>
            <label className="space-y-2">
              <FieldLabel>Teacher</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={assessmentState.teacherUserId} onChange={(e) => setAssessmentState((current) => ({ ...current, teacherUserId: e.target.value }))}>
                <option value="">Auto-assign from subject/class</option>
                {teachers.map((teacher) => <option key={teacher.userId} value={teacher.userId}>{teacher.fullName}</option>)}
              </select>
            </label>
            <label className="space-y-2">
              <FieldLabel>Term</FieldLabel>
              <select className="h-11 w-full rounded-2xl border border-slate-200 px-3" value={assessmentState.term} onChange={(e) => setAssessmentState((current) => ({ ...current, term: e.target.value }))}>
                {['Term 1', 'Term 2', 'Term 3', 'Term 4'].map((term) => <option key={term} value={term}>{term}</option>)}
              </select>
            </label>
            <label className="space-y-2 md:col-span-2">
              <FieldLabel>Assessment title</FieldLabel>
              <Input value={assessmentState.title} onChange={(e) => setAssessmentState((current) => ({ ...current, title: e.target.value }))} placeholder="Term assessment title" className="h-11 rounded-2xl" />
            </label>
            <label className="space-y-2">
              <FieldLabel>Due date</FieldLabel>
              <Input type="datetime-local" value={assessmentState.dueDate} onChange={(e) => setAssessmentState((current) => ({ ...current, dueDate: e.target.value }))} className="h-11 rounded-2xl" />
            </label>
            <label className="space-y-2">
              <FieldLabel>Max marks</FieldLabel>
              <Input value={assessmentState.maxMarks} onChange={(e) => setAssessmentState((current) => ({ ...current, maxMarks: e.target.value }))} className="h-11 rounded-2xl" />
            </label>
          </div>
          <div className="mt-6 flex justify-end gap-3">
            <Button type="button" className="rounded-2xl bg-slate-200 px-4 text-slate-800 hover:bg-slate-300" onClick={() => setAssessmentState(emptyAssessment)}>Cancel</Button>
            <Button type="button" className="rounded-2xl bg-[#0B5BFF] px-5 hover:bg-[#0849cb]" disabled={createAssessment.isPending || !assessmentState.subjectId || !assessmentState.classId || !assessmentState.title.trim() || !assessmentState.dueDate || !assessmentState.maxMarks} onClick={() => createAssessment.mutate()}>
              {createAssessment.isPending ? 'Creating...' : 'Create Assessment'}
            </Button>
          </div>
        </ModalShell>
      ) : null}

      {uploadState.subjectId ? (
        <ModalShell title="Upload ATP" subtitle="ATP upload status is shown on subjects. This school instance does not yet have document storage enabled for ATP files." onClose={() => setUploadState({ subjectId: '', file: null })}>
          <div className="space-y-4">
            <label className="block rounded-[24px] border border-dashed border-slate-300 p-6 text-center">
              <Upload className="mx-auto h-8 w-8 text-slate-400" />
              <p className="mt-3 text-sm font-medium text-slate-900">Select ATP file</p>
              <p className="mt-1 text-xs text-slate-500">Accepted formats: PDF, DOC, DOCX, XLSX</p>
              <input
                type="file"
                accept=".pdf,.doc,.docx,.xlsx"
                className="mt-4 block w-full text-sm text-slate-600"
                onChange={(event) => setUploadState((current) => ({ ...current, file: event.target.files?.[0] ?? null }))}
              />
            </label>
            {uploadState.file ? <p className="text-sm text-slate-700">Selected file: {uploadState.file.name}</p> : null}
            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
              ATP file persistence is not yet enabled on this deployment. Subject ATP status continues to use real seeded ATP coverage and will show uploaded documents once storage is connected.
            </div>
          </div>
          <div className="mt-6 flex justify-end">
            <Button type="button" className="rounded-2xl bg-slate-200 px-4 text-slate-800 hover:bg-slate-300" onClick={() => setUploadState({ subjectId: '', file: null })}>Close</Button>
          </div>
        </ModalShell>
      ) : null}

      {aiState ? (
        <ModalShell title="AI Subject Assistant" subtitle="Use the existing EduRite AI patterns when AI provider configuration is available." onClose={() => setAiState(null)}>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {aiActions.map((action) => (
              <button
                key={action.key}
                type="button"
                onClick={() => setAiState({ ...aiState, action: action.key })}
                className={`rounded-[24px] border p-4 text-left ${aiState.action === action.key ? 'border-[#0B5BFF] bg-blue-50' : 'border-slate-200 bg-white hover:border-slate-300'}`}
              >
                <p className="font-semibold text-slate-900">{action.label}</p>
                <p className="mt-2 text-sm text-slate-600">Prepare a subject-specific {action.label.toLowerCase()} workflow.</p>
              </button>
            ))}
          </div>
          <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
            <p className="font-medium text-slate-900">{aiActions.find((item) => item.key === aiState.action)?.label}</p>
            <p className="mt-2">AI generation will appear here once subject-authoring endpoints are connected to EduRite&apos;s live AI provider workflow. If AI is unavailable, the app will keep showing a friendly fallback instead of fabricated content.</p>
          </div>
          <div className="mt-6 flex justify-end">
            <Button type="button" className="rounded-2xl bg-slate-200 px-4 text-slate-800 hover:bg-slate-300" onClick={() => setAiState(null)}>Close</Button>
          </div>
        </ModalShell>
      ) : null}
    </AdminPageLayout>
  );
};
