import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAppQuery } from '@/hooks/useAppQuery';
import { useAuth } from '@/hooks/useAuth';
import { recommendationService } from '@/services/recommendationService';
import { aiGuidanceService } from '@/services/aiGuidanceService';
import { RequireEntitlement, AiUsageDisplay } from '@/features/subscriptions/access';
import { ProfileAssociationButton } from '@/components/student/profile/ProfileAssociations';
import { StudentPsychometricPage } from './StudentPages';

export function StudentCareerMatchPage() {
  const [params] = useSearchParams();
  return <section className="ex-workspace student-page-section">
    <h1 className="student-page-title">Career Match</h1>
    <p>Explore careers using your saved academic results, interests, preferences and optional assessment responses. These suggestions are not a diagnosis, an admission decision or a guaranteed career outcome.</p>
    {params.get('step') === 'assessment' ? <>
      <Link className="ep-primary" to="/student/career-match">Back to Career Match</Link>
      <RequireEntitlement feature="CAREER_ASSESSMENT"><StudentPsychometricPage /></RequireEntitlement>
    </> : <RequireEntitlement feature="PERSONALISED_CAREER_RECOMMENDATIONS"><CareerMatches /></RequireEntitlement>}
  </section>;
}

function CareerMatches() {
  const { user } = useAuth();
  const client = useQueryClient();
  const recs = useAppQuery({ queryKey: ['recs', user?.id], queryFn: recommendationService.mine, enabled: Boolean(user?.id), staleTime: 0 });
  const ai = useMutation({ mutationFn: aiGuidanceService.getPersonalisedCareerAdvice,
    onSettled: () => client.invalidateQueries({ queryKey: ['subscription-access'] }) });
  const resetAi = ai.reset;
  useEffect(() => { resetAi(); }, [user?.id, recs.dataUpdatedAt, resetAi]);
  const refresh = () => { ai.reset(); void recs.refetch(); };
  const data = recs.data;
  return <>
    <div className="ec-controls">
      <Link to="/student/profile?section=academic">Update academic subjects and marks</Link>
      <Link to="/student/profile?section=interests">Update interests and skills</Link>
      <Link to="/student/profile?section=career">Update career goals and preferences</Link>
      <Link to="/student/career-match?step=assessment">Assessment and saved results</Link>
      <button className="ep-primary" disabled={recs.isFetching || ai.isPending} onClick={refresh}>{recs.isFetching ? 'Refreshing...' : 'Refresh recommendations'}</button>
    </div>
    {recs.isPending && <p role="status">Loading your saved evidence...</p>}
    {recs.isError && <p role="alert">Could not load your career matches. <button onClick={refresh}>Retry</button></p>}
    {data && <>
      <section className="ed-panel"><h2>Inputs used</h2>
        {data.inputsUsed?.length ? <ul>{data.inputsUsed.map(input => <li key={input}>{input}</li>)}</ul> : <p>No usable matching evidence is available yet. Add academic results, interests or an assessment to get started.</p>}
        <h2>Missing information</h2><ul>{data.missingInformation?.map(input => <li key={input}>{input}</li>)}</ul>
        {!data.missingInformation?.length && <p>All input categories are available. Keep your saved information up to date.</p>}
        {data.limitations?.map(text => <p key={text}>{text}</p>)}
      </section>
      <RequireEntitlement feature="PERSONALISED_CAREER_RECOMMENDATIONS"><section className="ed-panel">
        <h2>EduRite AI Guidance</h2><p>Request an AI explanation using your current saved profile and available assessment evidence. It is optional; catalogue recommendations remain available if AI fails.</p>
        <AiUsageDisplay /><button className="ep-primary" disabled={ai.isPending || recs.isFetching || !data.inputsUsed?.length} onClick={() => ai.mutate()}>{ai.isPending ? 'Reviewing your evidence...' : 'Explore with EduRite AI Guidance'}</button>
        {ai.isError && <p role="alert">AI guidance is unavailable. Your catalogue recommendations remain below. You can retry.</p>}
      </section></RequireEntitlement>
      <h2>{ai.data ? 'AI career suggestions' : 'Career recommendations'}</h2>
      {ai.data ? <>
        <p>AI suggestions use your saved inputs and may need verification. No matching percentages or admission eligibility have been calculated.</p>
        <button onClick={() => ai.reset()}>Show catalogue recommendations</button>
        <div className="ed-feature-grid">{ai.data.recommendedCareers.map((item, index) => <article className="card p-5" key={`${item.name}-${index}`}>
          <h3>{item.name}</h3><h4>Why explore this career</h4><p>{item.reason}</p>
          <ul>{item.improvements.map(text => <li key={text}>{text}</li>)}</ul>
          <Link to={`/student/career-explorer?section=career-path&career=${encodeURIComponent(item.name)}`}>Explore career pathway</Link>
        </article>)}</div>
      </> : <>
        {!data.suggestedCareers.length && <p>No supported catalogue matches yet. Add more information above or <Link to="/student/career-explorer">browse Career Explorer</Link>. An assessment is optional.</p>}
        <div className="ed-feature-grid">{data.suggestedCareers.map(item => <article className="card p-5" key={item.id}>
          <h3>{item.title}</h3><h4>Why this career is recommended</h4><p>{item.rationale}</p>
          <ProfileAssociationButton type="CAREER" targetId={item.id} />
          <Link to={`/student/career-explorer?section=career-path&career=${encodeURIComponent(item.title)}`}>Explore career pathway</Link>
        </article>)}</div>
      </>}
      {data.careerSuggestionsLimited && <p>{data.upgradeMessage}</p>}
    </>}
  </>;
}
