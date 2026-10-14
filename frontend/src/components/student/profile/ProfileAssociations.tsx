import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAppQuery } from '@/hooks/useAppQuery';
import { useAuth } from '@/hooks/useAuth';
import { studentService, type ProfileAssociation } from '@/services/studentService';

function useAssociations() {
  const { user } = useAuth();
  const client = useQueryClient();
  const key = ['profile-associations', user?.id];
  const query = useAppQuery({ queryKey: key, queryFn: studentService.associations, enabled: Boolean(user?.id) });
  const save = useMutation({
    mutationFn: ({ type, targetId, remove }: { type: ProfileAssociation['type']; targetId: string; remove?: boolean }) =>
      remove ? studentService.unlinkAssociation(type, targetId) : studentService.linkAssociation(type, targetId),
    onSuccess: data => {
      client.setQueryData(key, data);
      for (const prefix of ['me', 'recs', 'ai-guidance-university-sources']) client.invalidateQueries({ queryKey: [prefix] });
    },
  });
  return { query, save };
}

export function ProfileAssociationButton({ type, targetId }: { type: ProfileAssociation['type']; targetId: string }) {
  const { query, save } = useAssociations();
  const linked = query.data?.some(item => item.type === type && item.targetId === targetId);
  if (query.isError) return <p role="alert">Could not load profile links. <button onClick={() => query.refetch()}>Retry</button></p>;
  return <div className="space-y-2">
    <button type="button" className="ep-primary" disabled={query.isPending || save.isPending || linked} onClick={() => save.mutate({ type, targetId })}>
      {query.isPending ? 'Loading profile link...' : save.isPending ? 'Linking to your profile...' : linked ? 'Linked to My Profile' : 'Link to My Profile'}
    </button>
    {linked && <Link className="ml-3 underline" to="/student/profile?section=career">View in My Profile</Link>}
    {save.isError && <p role="alert">{save.error.message || 'Could not link this selection. Please retry.'}</p>}
    {save.isSuccess && linked && <p role="status">Saved to your profile.</p>}
  </div>;
}

export function ProfileAssociations() {
  const { query, save } = useAssociations();
  return <section className="ed-panel ep-card"><h2>Linked courses and careers</h2>
    {query.isPending && <p role="status">Loading your linked choices...</p>}
    {query.isError && <p role="alert">Could not load your linked choices. <button onClick={() => query.refetch()}>Retry</button></p>}
    {query.data?.length === 0 && <p>Link a course or career from Study Options or Career Explorer to keep it in your profile.</p>}
    <ul className="space-y-3">{query.data?.map(item => <li key={`${item.type}-${item.targetId}`}>
      <Link className="underline" to={item.type === 'PROGRAMME' ? `/student/career-explorer?section=career-path&career=${encodeURIComponent(item.careerName ?? '')}&programme=${encodeURIComponent(item.targetId)}` : item.type === 'CAREER' ? `/student/career-explorer?section=career-path&career=${encodeURIComponent(item.title)}` : `/student/study-options?section=programmes&q=${encodeURIComponent(item.title)}`}>{item.title}</Link>
      <span className="ml-2">({item.type === 'CAREER' ? 'Career' : item.type === 'PROGRAMME' ? 'Programme' : 'Course'})</span>
      <button type="button" className="ml-3 underline" disabled={save.isPending} onClick={() => save.mutate({ type: item.type, targetId: item.targetId, remove: true })}>Unlink<span className="sr-only"> {item.title}</span></button>
    </li>)}</ul>
    {save.isError && <p role="alert">{save.error.message}</p>}
    {save.isSuccess && <p role="status">Your profile links have been updated.</p>}
  </section>;
}
