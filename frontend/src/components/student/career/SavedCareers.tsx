import { Bookmark } from 'lucide-react';
import { useAppQuery } from '@/hooks/useAppQuery';
import { studentService } from '@/services/studentService';
import { careerService } from '@/services/careerService';
import type { SavedCareerRoadmap } from '@/types';

export function SavedCareers({ roadmaps, loading, retry, error, onRoadmap, onCareer }: { roadmaps: SavedCareerRoadmap[]; loading: boolean; retry: () => void; error: boolean; onRoadmap: (item: SavedCareerRoadmap) => void; onCareer: (name: string) => void }) {
  const careers = useAppQuery({ queryKey: ['explore-saved-careers'], queryFn: async () => {
    const ids = await studentService.savedCareers();
    return Promise.all(ids.map(id => careerService.details(id)));
  } });
  return <>
    <h3>Saved careers</h3>
    {careers.isLoading && <div className="ex-skeleton" role="status" aria-label="Loading saved careers"/>}
    {careers.isError && <p className="ec-error">Saved careers could not load. <button onClick={() => careers.refetch()}>Retry</button></p>}
    {careers.data?.map(item => <button className="ec-study" key={item.id} onClick={() => onCareer(item.title)}><Bookmark/><span><strong>{item.title}</strong><small>{item.industry}</small></span></button>)}
    {!careers.isLoading && !careers.isError && !careers.data?.length && <p className="ec-empty">No saved careers yet.</p>}
    <h3>Saved roadmaps</h3>
    {loading && <div className="ex-skeleton" role="status" aria-label="Loading saved roadmaps"/>}
    {error && <p role="alert" className="ec-error">Saved roadmaps could not load. <button onClick={retry}>Retry</button></p>}
    {roadmaps.map(item => <button className="ec-study" key={item.id} onClick={() => onRoadmap(item)}><Bookmark/><span><strong>{item.careerName}</strong><small>Saved {new Date(item.updatedAt).toLocaleDateString()} · Snapshot APS {item.learnerAps}</small></span></button>)}
    {!roadmaps.length && !error && !loading && <p className="ec-empty">Generate and save a roadmap to keep it here.</p>}
  </>;
}
