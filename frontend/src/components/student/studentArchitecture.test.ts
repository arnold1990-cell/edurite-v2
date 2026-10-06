/// <reference types="node" />
import { readFileSync, readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import ts from 'typescript';
import { describe, expect, it } from 'vitest';
import { studentSection } from './StudentSectionNavigation';

describe('student route architecture', () => {
  it('owns all student routes under one authenticated StudentAppShell', () => {
    const source = ts.createSourceFile('App.tsx', readFileSync(resolve('src/app/App.tsx'), 'utf8'), ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX);
    const routes: { path: string; parents: string[]; element: string }[] = [];
    function visit(node: ts.Node, parents: string[] = []) {
      if (ts.isJsxElement(node) || ts.isJsxSelfClosingElement(node)) {
        const tag = ts.isJsxElement(node) ? node.openingElement : node;
        if (tag.tagName.getText(source) === 'Route') {
          const attrs = tag.attributes.properties;
          const attr = (name: string) => attrs.find(item => ts.isJsxAttribute(item) && item.name.getText(source) === name) as ts.JsxAttribute | undefined;
          const path = attr('path')?.initializer;
          const element = attr('element')?.initializer?.getText(source) ?? '';
          if (path && ts.isStringLiteral(path)) routes.push({ path: path.text, parents, element });
          parents = [...parents, element];
        }
      }
      ts.forEachChild(node, child => visit(child, parents));
    }
    visit(source);
    const root = routes.filter(route => route.path === '/student');
    expect(root).toHaveLength(1);
    expect(root[0].element).toContain('<StudentAppShell');
    expect(root[0].parents.join(' ')).toContain('<RequireAuth');
    expect(root[0].parents.join(' ')).toContain('<RequireRole role="STUDENT"');
    expect(routes.filter(route => route.path.startsWith('/student/'))).toEqual([]);
    const students = routes.filter(route => route.parents.some(parent => parent.includes('<StudentAppShell')));
    expect(students.length).toBeGreaterThan(40);
    for (const path of ['dashboard', 'subscription', 'cv-builder', 'profile/cv', 'psychometric', 'settings', 'notifications', '*']) {
      expect(students.some(route => route.path === path)).toBe(true);
    }
    expect(students.some(route => route.parents.join(' ').includes('<DashboardLayout'))).toBe(false);
  });

  it('keeps page components independent of application shells', () => {
    for (const file of readdirSync(resolve('src/pages/student')).filter(file => file.endsWith('.tsx'))) {
      expect(readFileSync(resolve('src/pages/student', file), 'utf8')).not.toMatch(/<(?:StudentAppShell|StudentDashboardLayout|DashboardLayout)\b/);
    }
    expect(readFileSync(resolve('src/components/layout/DashboardLayout.tsx'), 'utf8')).not.toMatch(/studentTopNav|studentPersonalNav|StudentDashboardLayout/);
  });

  it.each([
    ['/student/cv-builder', '', '/student/profile'],
    ['/student/profile/cv', '', '/student/profile'],
    ['/student/subscription', '', undefined],
    ['/student/notifications', '', undefined],
    ['/student/explore', '?category=Courses', '/student/study-options'],
    ['/student/explore', '?category=Bursaries', '/student/funding'],
    ['/student/universities/uct/programmes', '', '/student/study-options'],
    ['/student/psychometric/', '', '/student/career-explorer'],
    ['/student/ai-tutor', '', '/student/learning'],
    ['/student/rewards', '', '/student/dashboard'],
  ])('selects the correct section for %s%s', (path, search, expected) => {
    expect(studentSection(path, search)).toBe(expected);
  });
});
