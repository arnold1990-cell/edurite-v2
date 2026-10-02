import { Link } from 'react-router-dom';
import { BookOpen, Plus, Trash2 } from 'lucide-react';
import { calculateNscLevel, normalizeManualRow, type SubjectRow, type ResultSource } from '@/pages/student/roadmapAps.utils';

type Props = {
  subjects: SubjectRow[]; source: ResultSource; grade: string; province: string;
  grades: string[]; provinces: string[]; subjectOptions: string[];
  onGrade: (value: string) => void; onProvince: (value: string) => void;
  onChange: (id: string, field: 'subjectName' | 'markPercentage', value: string) => void;
  onRemove: (id: string) => void; onAdd: () => void; onManual: () => void; onLink: () => void;
  aps: number | null; loading: boolean; error: boolean; onRetry: () => void; feedback?: string;
};
export function SubjectEditor(p: Props) {
  return <div className="ex-subject-editor">
    <p className="ec-note">Link your profile results or try a set of marks here. Manual changes are used for this roadmap; save permanent academic changes in My Profile.</p>
    <div className="ex-filter"><label>Grade<select value={p.grade} onChange={e => p.onGrade(e.target.value)}><option value="">Select grade</option>{p.grades.map(value => <option key={value}>{value}</option>)}</select></label><label>Province<select value={p.province} onChange={e => p.onProvince(e.target.value)}><option value="">Select province</option>{p.provinces.map(value => <option key={value}>{value}</option>)}</select></label></div>
    <div className="ec-controls"><button onClick={p.onLink}>Link My Profile</button><button onClick={p.onManual}>Enter marks manually</button><Link to="/student/profile?tab=academic">Edit My Profile</Link></div>
    <p className="ec-note">Results source: {p.source === 'PROFILE' ? 'My Profile' : 'Manual preview'}</p>
    {p.feedback && <p className="ec-note" role="status">{p.feedback}</p>}
    {p.error && <p className="ec-error" role="alert">APS could not be calculated. <button onClick={p.onRetry}>Retry</button></p>}
    {!p.subjects.length && <p className="ec-empty">No profile subjects are available. Add subjects or update your academic profile.</p>}
    <div className="ex-subject-rows">{p.subjects.map((row,index) => {
      const value = normalizeManualRow(row);
      const level = value?.level ?? calculateNscLevel(value?.markPercentage);
      const invalid = row.markPercentage !== '' && (value?.markPercentage == null || value.markPercentage < 0 || value.markPercentage > 100);
      return <div className="ex-subject-row" key={row.id}><BookOpen size={20}/><label>Subject {index + 1}<input list="explore-subject-options" value={row.subjectName} disabled={p.source === 'PROFILE'} onChange={e => p.onChange(row.id,'subjectName',e.target.value)} placeholder="Subject name"/></label><label>Mark (%)<input aria-invalid={invalid} type="number" min={0} max={100} value={row.markPercentage} disabled={p.source === 'PROFILE'} onChange={e => p.onChange(row.id,'markPercentage',e.target.value)}/></label><span className="ec-note">Level {level ?? '—'}{row.included === false && <small>{row.exclusionReason}</small>}</span>{p.source === 'MANUAL' && <button aria-label={`Remove subject ${index + 1}`} onClick={() => p.onRemove(row.id)}><Trash2 size={16}/></button>}{invalid && <p className="ec-error">Enter a mark between 0 and 100.</p>}</div>;
    })}</div>
    <datalist id="explore-subject-options">{p.subjectOptions.map(value => <option key={value} value={value}/>)}</datalist>
    <div className="ex-subject-footer"><button className="ex-saved" onClick={p.onAdd}><Plus size={16}/> Add Subject</button><div><small>Your APS</small><strong aria-live="polite">{p.loading ? 'Calculating…' : p.aps ?? 'Not available'}</strong></div></div>
  </div>;
}
