import React from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { App } from '@/app/App';
import { GlobalErrorBoundary } from '@/app/GlobalErrorBoundary';
import { queryClient } from '@/lib/queryClient';
import { AuthProvider } from '@/features/auth/AuthContext';
import '@/styles/index.css';

const rootElement = document.getElementById('root');

if (!rootElement) {
  document.body.innerHTML = `
    <main style="min-height:100vh;display:grid;place-items:center;padding:24px;background:#020617;color:#e2e8f0;font-family:system-ui,sans-serif;">
      <section style="max-width:640px;border:1px solid rgba(255,255,255,0.12);border-radius:24px;padding:32px;background:rgba(255,255,255,0.04);">
        <p style="margin:0 0 12px;font-size:12px;font-weight:700;letter-spacing:0.3em;text-transform:uppercase;color:#67e8f9;">EduRite</p>
        <h1 style="margin:0 0 16px;font-size:32px;line-height:1.2;color:#fff;">We could not start the application.</h1>
        <p style="margin:0;font-size:16px;line-height:1.7;">The page markup is missing the application root. Reload the page or contact support if the problem continues.</p>
      </section>
    </main>
  `;
  throw new Error('Missing #root element in index.html');
}

createRoot(rootElement).render(
  <React.StrictMode>
    <GlobalErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </GlobalErrorBoundary>
  </React.StrictMode>,
);
