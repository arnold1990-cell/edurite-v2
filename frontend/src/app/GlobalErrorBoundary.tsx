import React from 'react';

type GlobalErrorBoundaryState = {
  hasError: boolean;
};

export class GlobalErrorBoundary extends React.Component<React.PropsWithChildren, GlobalErrorBoundaryState> {
  state: GlobalErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): GlobalErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: unknown, errorInfo: React.ErrorInfo) {
    console.error('EduRite startup error', error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-slate-950 px-6 py-16 text-white">
          <div className="mx-auto flex min-h-[60vh] max-w-2xl flex-col justify-center rounded-3xl border border-white/10 bg-white/5 p-8 shadow-2xl backdrop-blur">
            <p className="text-sm font-semibold uppercase tracking-[0.3em] text-cyan-300">EduRite</p>
            <h1 className="mt-4 text-3xl font-semibold text-white">We could not load this page.</h1>
            <p className="mt-4 text-base leading-7 text-slate-200">
              An unexpected startup error stopped the application before it finished rendering.
              Refresh the page to try again.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <button
                className="rounded-full bg-cyan-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300"
                onClick={this.handleReload}
                type="button"
              >
                Reload page
              </button>
              <a
                className="rounded-full border border-white/20 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
                href="/auth/login"
              >
                Return to sign in
              </a>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
