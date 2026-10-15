import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAppQuery } from '@/hooks/useAppQuery';
import { studyPlanService } from '@/services/studyPlanService';
import { Button } from '@/components/ui/Button';
import { Link } from 'react-router-dom';

export function StudyPlans({ careerName, programmeId }: { careerName: string; programmeId?: string }) {
  const client = useQueryClient();
  const history = useAppQuery({ queryKey: ['study-plans'], queryFn: studyPlanService.history });
  const [selectedId, setSelectedId] = useState('');
  const pending = useRef<{ requestKey: string; careerName: string; programmeId?: string }>();
  const selected = history.data?.find(plan => plan.id === selectedId) ?? history.data?.[0];
  const generate = useMutation({
    mutationFn: () => {
      if (!pending.current || pending.current.careerName !== careerName || pending.current.programmeId !== programmeId) pending.current = { requestKey: crypto.randomUUID(), careerName, programmeId };
      return studyPlanService.generate(pending.current);
    },
    onSuccess: plan => { pending.current = undefined; setSelectedId(plan.id); client.invalidateQueries({ queryKey: ['study-plans'] }); client.invalidateQueries({ queryKey: ['student-career-roadmaps-saved'] }); },
    onSettled: () => client.invalidateQueries({ queryKey: ['subscription-access'] }),
  });
  const progress = useMutation({
    mutationFn: ({ id, completed }: { id: string; completed: number[] }) => studyPlanService.progress(id, completed),
    onSuccess: () => client.invalidateQueries({ queryKey: ['study-plans'] }),
  });
  return <section className="space-y-4 rounded-xl border p-4" aria-label="Saved AI study plans">
    <h2 className="text-lg font-semibold">AI Study Plans</h2>
    <p>AI-generated study guidance. Official admission requirements must be checked with the institution.</p>
    {!careerName && <p>Choose a supported career above to generate a plan. Your saved plans remain available below.</p>}
    {!history.isLoading && !history.isError && !history.data?.length && <p>No saved study plans yet.</p>}
    <p>Uses your saved grade, subjects and marks{careerName ? ` for ${careerName}` : ''}. Save your grade and at least one subject result in <Link className="underline" to="/student/profile?section=academic">Academic Results</Link> first. New generations preserve earlier plans.</p>
    <Button disabled={!careerName || generate.isPending} onClick={() => generate.mutate()}>{generate.isPending ? 'Generating and saving your plan…' : generate.isError ? 'Retry study plan' : selected ? 'Generate a new study plan' : 'Generate Study Plan'}</Button>
    {generate.isError && <p role="alert">{generate.error.message}</p>}
    {history.isLoading && <p role="status">Loading saved plans…</p>}
    {history.isError && <p role="alert">Could not load your plans. <button onClick={() => history.refetch()}>Retry</button></p>}
    {!!history.data?.length && <label className="block">View Study Plan<select className="ml-3 rounded border p-2" value={selected?.id ?? ''} onChange={e => setSelectedId(e.target.value)}>{history.data.map(plan => <option value={plan.id} key={plan.id}>{plan.careerName} — {new Date(plan.createdAt).toLocaleString()}</option>)}</select></label>}
    {selected && <><p>{selected.completedSteps.length} / {selected.steps.length} goals completed</p>{selected.steps.map((step, index) => <article key={index} className="rounded-lg border p-4"><label><input type="checkbox" disabled={progress.isPending} checked={selected.completedSteps.includes(index)} onChange={e => progress.mutate({ id: selected.id, completed: e.target.checked ? [...selected.completedSteps, index] : selected.completedSteps.filter(i => i !== index) })}/> {step.title}</label><p>{step.focus}</p><ul className="list-disc pl-5">{step.actions.map((action, i) => <li key={i}>{action}</li>)}</ul></article>)}</>}
    {progress.isError && <p role="alert">Progress was not saved: {progress.error.message}</p>}
  </section>;
}
