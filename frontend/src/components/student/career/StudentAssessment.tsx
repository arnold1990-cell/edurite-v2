import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAppQuery } from '@/hooks/useAppQuery';
import { useAuth } from '@/hooks/useAuth';
import { psychometricService } from '@/services/psychometricService';
import type { PsychometricAttemptResult, PsychometricResult } from '@/types';

export function StudentAssessment() {
  const { user } = useAuth();
  const client = useQueryClient();
  const [assessmentId, setAssessmentId] = useState('');
  const [answering, setAnswering] = useState(false);
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [selected, setSelected] = useState<PsychometricAttemptResult | PsychometricResult | null>(null);
  const assessments = useAppQuery({ queryKey: ['psychometric', 'assessments'], queryFn: psychometricService.assessments });
  const history = useAppQuery({ queryKey: ['psychometric', 'history', user?.id], queryFn: psychometricService.history, enabled: Boolean(user?.id) });
  const legacy = useAppQuery({ queryKey: ['psychometric', 'latest', user?.id], queryFn: psychometricService.latestStudent, enabled: Boolean(user?.id), retry: false });
  useEffect(() => { if (!assessmentId && assessments.data?.length) setAssessmentId((assessments.data.find(item => item.code === 'EDURITE_CAREER_V2') || assessments.data[0]).id); }, [assessmentId, assessments.data]);
  const questions = useAppQuery({ queryKey: ['psychometric', 'questions', assessmentId], queryFn: () => psychometricService.assessmentQuestions(assessmentId), enabled: Boolean(assessmentId) });
  const rows = questions.data ?? [];
  const complete = rows.length > 0 && rows.every(q => Number.isInteger(answers[q.id]) && answers[q.id] >= q.minScore && answers[q.id] <= q.maxScore);
  const submit = useMutation({
    mutationFn: () => {
      if (!complete || questions.isError || questions.isFetching) throw new Error('Answer every question before submitting.');
      return psychometricService.submitAssessmentAttempt(assessmentId, rows.map(q => ({ questionId: q.id, score: answers[q.id] })));
    },
    onSuccess: result => {
      setSelected(result); setAnswering(false); setAnswers({});
      for (const key of ['psychometric', 'recs', 'dash', 'study-recommendations', 'profile-ai-recommendations-fallback']) void client.invalidateQueries({ queryKey: [key] });
    },
  });
  return <section className="ed-panel">
    <h2>Assessment and saved results</h2>
    <p>This optional assessment records your self-reported interests and work preferences. Scores describe your responses, not ability, a diagnosis or a guaranteed career outcome. Career Match also works without an assessment.</p>
    <p>Answer each statement honestly. No answers are selected for you.</p>
    {assessments.isPending && <p role="status">Loading assessments...</p>}
    {assessments.isError && <p role="alert">Assessments could not load. <button onClick={() => assessments.refetch()}>Retry</button></p>}
    {!!assessments.data?.length && <label>Assessment <select value={assessmentId} disabled={submit.isPending} onChange={event => { setAssessmentId(event.target.value); setAnswers({}); setAnswering(false); submit.reset(); }}>
      {assessments.data.map(item => <option key={item.id} value={item.id}>{item.name} ({item.version})</option>)}
    </select></label>}
    {(questions.isError || (!questions.isPending && !rows.length)) && <p role="alert">Questions are unavailable. Your saved results remain below. <button onClick={() => questions.refetch()}>Retry questions</button></p>}
    {!answering && <button className="ep-primary" disabled={!rows.length || questions.isFetching || questions.isError} onClick={() => { setSelected(null); setAnswering(true); submit.reset(); }}>Start assessment</button>}
    {answering && <>
      <p role="status">{rows.filter(q => answers[q.id] != null).length} of {rows.length} answered</p>
      {rows.map((q, index) => <fieldset className="rounded border p-4 my-3" key={q.id} disabled={submit.isPending}>
        <legend>{index + 1}. {q.prompt}</legend>
        <div className="flex flex-wrap gap-3">{Array.from({ length: q.maxScore - q.minScore + 1 }, (_, i) => i + q.minScore).map(score => <label key={score}>
          <input type="radio" name={q.id} checked={answers[q.id] === score} onChange={() => setAnswers(current => ({ ...current, [q.id]: score }))} /> {score}{score === q.minScore ? ' — Strongly disagree' : score === q.maxScore ? ' — Strongly agree' : ''}
        </label>)}</div>
      </fieldset>)}
      <button className="ep-primary" disabled={!complete || submit.isPending || questions.isFetching || questions.isError} onClick={() => submit.mutate()}>{submit.isPending ? 'Saving...' : 'Save assessment results'}</button>
      <button className="ml-3" disabled={submit.isPending} onClick={() => setAnswering(false)}>Cancel</button>
      {submit.isError && <p role="alert">Your assessment could not be saved. Your answers are retained; please retry.</p>}
    </>}
    {selected && <section aria-label="Saved assessment result" className="rounded border p-4 my-4">
      <h3>Your saved assessment result</h3><p>{'submittedAt' in selected ? selected.submittedAt : selected.createdAt}</p>
      <p>{selected.interpretation}</p><p>Self-reported strengths: {selected.strengthAreas.join(', ') || 'No distinct areas reported'}</p>
      <p>Areas to explore: {selected.growthAreas.join(', ') || 'No areas reported'}</p>
      <dl>{Object.entries(selected.scores).map(([dimension, score]) => <div key={dimension}><dt>{dimension}</dt><dd>{score} (recorded response average)</dd></div>)}</dl>
      <p>Career Match uses your latest saved responses. Viewing an older result does not change that input.</p>
      <Link className="ep-primary" to="/student/career-match">Return to refreshed career recommendations</Link>
    </section>}
    <h3>Assessment history</h3>
    {history.isPending && <p role="status">Loading saved results...</p>}
    {history.isError && <p role="alert">History could not load. <button onClick={() => history.refetch()}>Retry history</button></p>}
    {history.data?.length === 0 && <p>No question-based assessments saved yet.</p>}
    <ul>{history.data?.map(result => <li key={result.attemptId}><button onClick={() => { setSelected(result); setAnswering(false); }}>
      View {assessments.data?.find(item => item.id === result.assessmentId)?.name || 'Archived assessment'} — {result.submittedAt}
    </button></li>)}</ul>
    {legacy.isError && <p role="alert">Earlier saved results could not load. <button onClick={() => legacy.refetch()}>Retry earlier results</button></p>}
    {legacy.data && !history.data?.length && <button onClick={() => { setSelected(legacy.data!); setAnswering(false); }}>View latest earlier assessment result</button>}
  </section>;
}
