import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom/server';
import { StudentProfileView } from './StudentProfileView';
import type { StudentProfile } from '@/types';

describe('restored academic profile', () => {
  it.each(['/student/documents', '/student/documents?section=academic', '/student/documents/', '/student/profile?tab=settings', '/student/profile?section=documents'])('opens uploads while retaining identity editing at %s', location => {
    const html = renderToStaticMarkup(<StaticRouter location={location}><StudentProfileView
      profile={{ firstName: 'Learner', profileCompleteness: 0 } as StudentProfile}
      form={{}} onChange={() => {}} onSave={async () => {}} saving={false}
      personalContent="Personal editor" academicContent="Academic editor" readinessContent="Readiness"
      guidanceContent="Guidance" documentsContent="Upload controls" savedProfilesContent="Saved versions"
    /></StaticRouter>);
    expect(html).toContain('Upload controls');
    expect(html).toContain('Saved versions');
    expect(html).toContain('Edit Profile');
    expect(html).not.toContain('Academic editor');
  });
  it('keeps all profile sections reachable and renders the existing academic editor', () => {
    const html = renderToStaticMarkup(<StaticRouter location="/student/profile?section=academic"><StudentProfileView
      profile={{ firstName: 'Learner', profileCompleteness: 0 } as StudentProfile}
      form={{}} onChange={() => {}} onSave={async () => {}} saving={false}
      personalContent="Personal editor" academicContent="Academic editor" readinessContent="Readiness"
      guidanceContent="Guidance" documentsContent="Documents" savedProfilesContent="Saved versions"
    /></StaticRouter>);
    for (const title of ['Overview', 'Academic Details', 'Interests &amp; Skills', 'Career Aspirations', 'Goals', 'Documents', 'Academic editor', 'Readiness', 'Save Profile']) expect(html).toContain(title);
    expect(html).toContain('aria-current="page"');
  });
});
