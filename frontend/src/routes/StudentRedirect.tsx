import { Navigate, useLocation } from 'react-router-dom';

export function studentRedirectTarget(to: string, search: string, hash = '') {
  const [path, targetSearch] = to.split('?');
  const query = new URLSearchParams(search);
  new URLSearchParams(targetSearch).forEach((value, key) => query.set(key, value));
  let destination = path;
  if (path === '/student/career-explorer' && !targetSearch) {
    const category = query.get('category');
    if (category === 'Courses' || category === 'Subjects') destination = '/student/study-options';
    if (category === 'Institutions') destination = '/student/institutions';
    if (category === 'Bursaries' || category === 'Opportunities') {
      destination = '/student/funding'; query.set('section', category === 'Bursaries' ? 'bursaries' : 'opportunities');
    }
  }
  return `${destination}${query.size ? `?${query}` : ''}${hash}`;
}
export function StudentRedirect({ to }: { to: string }) {
  const { search, hash } = useLocation();
  return <Navigate replace to={studentRedirectTarget(to, search, hash)} />;
}
