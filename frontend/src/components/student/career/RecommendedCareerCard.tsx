import { ArrowRight } from 'lucide-react';
import type { UniversityRecommendedCareer } from '@/types';
import careerImage from '@/assets/images/careers.jpeg';
import studyImage from '@/assets/images/courses.jpeg';

export function RecommendedCareerCard({ item, index, selected, onSelect, onDetails }: { item: UniversityRecommendedCareer; index: number; selected?: boolean; onSelect?: () => void; onDetails: () => void }) {
 return <article className={selected ? 'is-selected' : ''}><button type="button" className={`ec-career-art ec-tone-${index%4}`} aria-label={`${onSelect ? 'Select' : 'View'} ${item.name}`} aria-pressed={selected} onClick={() => onSelect ? onSelect() : onDetails()}><img src={index % 2 ? studyImage : careerImage} alt="" loading="lazy" style={{width:'100%',height:'100%',objectFit:'cover',objectPosition:`${index % 2 ? '60%' : '40%'} center`}}/></button><div className="ec-career-copy"><h3>{item.name}</h3><p>{item.reason || item.recommendationReason || 'Open details to review this recommendation.'}</p>{item.rankingCategory && <span className={`ec-tag ec-tone-${index%4}`}>{item.rankingCategory}</span>}<button onClick={() => { onSelect?.(); onDetails(); }}>View Details <ArrowRight size={12} /></button></div></article>;
}
