import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';

export function ExploreSearch() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [query, setQuery] = useState(params.get('q') ?? '');
  return <form className="ex-global-search" role="search" onSubmit={event => {
    event.preventDefault();
    navigate(`/student/career-roadmaps?q=${encodeURIComponent(query.trim())}&category=all`);
  }}><Search size={18}/><input aria-label="Search Explore" type="search" value={query} onChange={event=>setQuery(event.target.value)} placeholder="Search careers, courses, bursaries, institutions..."/><button aria-label="Submit Explore search"><Search size={16}/></button></form>;
}
