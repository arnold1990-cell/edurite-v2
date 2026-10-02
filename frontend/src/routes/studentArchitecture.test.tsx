import { describe, expect, it, vi } from 'vitest';
import { createRoutesFromChildren, matchRoutes, Route, Routes, type RouteObject } from 'react-router-dom';
import { StaticRouter } from 'react-router-dom/server';
import { renderToStaticMarkup } from 'react-dom/server';
import { App } from '@/app/App';
import { StudentAppShell } from '@/components/student/StudentAppShell';
import { studentSections } from '@/components/student/StudentSectionNavigation';
import { StudentRedirect } from './StudentRedirect';

vi.mock('@/hooks/useAuth', () => ({ useAuth: () => ({ user: { fullName: 'Test Student' }, logout: vi.fn() }) }));
vi.mock('@/hooks/useAppQuery', () => ({ useAppQuery: () => ({ data: undefined }) }));
vi.mock('@/components/student/career/ExploreSearch', () => ({ ExploreSearch: () => <input aria-label="Search careers" /> }));

const routes = createRoutesFromChildren(App().props.children[1].props.children);
const flatten = (items: RouteObject[]): RouteObject[] => items.flatMap(route => [route, ...flatten(route.children ?? [])]);
const student = flatten(routes).find(route => route.path === '/student')!;
const elementType = (route: RouteObject) => (route.element as React.ReactElement)?.type;

describe('Student portal architecture', () => {
  it('keeps every student destination under the same authenticated shell', () => {
    expect(elementType(student)).toBe(StudentAppShell);
    for (const route of student.children ?? []) {
      const path = `/student/${(route.path ?? '').replace(':id', '123').replace(':slug', 'university').replace('*', 'unknown-page')}`;
      const matches = matchRoutes(routes, path)!;
      expect(matches.filter(match => elementType(match.route) === StudentAppShell)).toHaveLength(1);
      expect(matches.slice(-1)[0]?.route).toBe(route);
      expect(matches[0].route.element).toBeDefined();
      expect((matches[1].route.element as React.ReactElement).props.role).toBe('STUDENT');
    }
  });

  it('resolves all section links to real pages, including redirects', () => {
    for (const section of studentSections) {
      for (const path of [section.to, ...section.links.map(([, path]) => `/student/${path}`)]) {
        const route = matchRoutes(routes, path)?.slice(-1)[0]?.route;
        expect(route?.path, path).not.toBe('*');
        expect(route, path).toBeDefined();
      }
    }
    for (const route of student.children ?? []) {
      if (elementType(route) !== StudentRedirect) continue;
      const to = (route.element as React.ReactElement).props.to;
      const target = matchRoutes(routes, to)?.slice(-1)[0]?.route;
      expect(target?.path, to).not.toBe('*');
      expect(elementType(target!), to).not.toBe(StudentRedirect);
    }
  });

  it('renders identical sidebar and header markup at every student route', () => {
    const warning = vi.spyOn(console, 'error').mockImplementation(() => {});
    try {
      let expectedSidebar = '', expectedHeader = '';
      for (const route of student.children ?? []) {
        const location = `/student/${(route.path ?? '').replace(':id', '123').replace(':slug', 'university')}`;
        const html = renderToStaticMarkup(<StaticRouter location={location}><Routes><Route path="/student" element={<StudentAppShell />}><Route index element={<p>Feature content</p>} /><Route path="*" element={<p>Feature content</p>} /></Route></Routes></StaticRouter>);
        const sidebar = html.match(/<aside[\s\S]*?<\/aside>/)?.[0].replace(/ class="is-active"| aria-current="page"/g, '').replace(/ class=""/g, '') ?? '';
        const header = html.match(/<header[\s\S]*?<\/header>/)?.[0] ?? '';
        expectedSidebar ||= sidebar; expectedHeader ||= header;
        expect(sidebar, location).toBe(expectedSidebar);
        expect(header, location).toBe(expectedHeader);
        expect(html).toContain('Feature content');
        expect(html).not.toContain('Search dashboard');
        expect(html).not.toContain('student-top-tabs-row');
      }
    } finally { warning.mockRestore(); }
  });
});
