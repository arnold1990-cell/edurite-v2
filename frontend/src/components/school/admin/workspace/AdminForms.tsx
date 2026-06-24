import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';

type Props = {
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
};

export const AdminForms = ({
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
}: Props) => (
  <div className="grid gap-4 xl:grid-cols-2">
    <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm space-y-3">
      <h2 className="text-sm font-semibold text-slate-900">Create Class</h2>
      <label className="block text-xs font-medium text-slate-700">Grade</label>
      <Input value={grade} onChange={(e) => setGrade(e.target.value)} placeholder="e.g. Grade 10" />
      <label className="block text-xs font-medium text-slate-700">Class Name</label>
      <Input value={className} onChange={(e) => setClassName(e.target.value)} placeholder="e.g. A" />
      <label className="block text-xs font-medium text-slate-700">Academic Year</label>
      <Input type="number" value={academicYear} onChange={(e) => setAcademicYear(Number(e.target.value))} placeholder="e.g. 2026" />
      <Button onClick={onCreateClass} disabled={creatingClass || !grade.trim() || !className.trim() || !Number.isFinite(academicYear)}>
        {creatingClass ? 'Creating...' : 'Create Class'}
      </Button>
    </div>
    <div className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm space-y-3">
      <h2 className="text-sm font-semibold text-slate-900">Create Subject</h2>
      <label className="block text-xs font-medium text-slate-700">Subject Name</label>
      <Input value={subjectName} onChange={(e) => setSubjectName(e.target.value)} placeholder="e.g. Mathematics" />
      <label className="block text-xs font-medium text-slate-700">Phase</label>
      <Input value={phase} onChange={(e) => setPhase(e.target.value)} placeholder="e.g. FET" />
      <Button onClick={onCreateSubject} disabled={creatingSubject || !subjectName.trim() || !phase.trim()}>
        {creatingSubject ? 'Creating...' : 'Create Subject'}
      </Button>
    </div>
  </div>
);
