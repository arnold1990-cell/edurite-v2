import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { authStore } from '@/features/auth/authStore';
import { Button } from '@/components/ui/Button';
import { authService } from '@/services/authService';

const IDLE_TIMEOUT_MS = 25 * 60 * 1000;
const WARNING_BEFORE_MS = 60 * 1000;
const CHANNEL_NAME = 'edurite-session';
const STORAGE_EVENT_KEY = 'edurite_session_event';
const LOGOUT_REASON_INACTIVITY = 'INACTIVITY';

type SessionEventMessage = { type: 'logout'; reason: string; at: number };

export const InactivitySessionManager = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, isHydrated, logout } = useAuth();
  const [showWarning, setShowWarning] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(Math.floor(WARNING_BEFORE_MS / 1000));
  const warningTimerRef = useRef<number | null>(null);
  const logoutTimerRef = useRef<number | null>(null);
  const countdownIntervalRef = useRef<number | null>(null);
  const channelRef = useRef<BroadcastChannel | null>(null);
  const loggingOutRef = useRef(false);

  const shouldTrack = useMemo(() => isHydrated && isAuthenticated, [isHydrated, isAuthenticated]);

  const clearTimers = useCallback(() => {
    if (warningTimerRef.current !== null) {
      window.clearTimeout(warningTimerRef.current);
      warningTimerRef.current = null;
    }
    if (logoutTimerRef.current !== null) {
      window.clearTimeout(logoutTimerRef.current);
      logoutTimerRef.current = null;
    }
    if (countdownIntervalRef.current !== null) {
      window.clearInterval(countdownIntervalRef.current);
      countdownIntervalRef.current = null;
    }
  }, []);

  const hardClearClientState = useCallback(() => {
    authStore.clear();
  }, []);

  const navigateToLoginWithExpiredMessage = useCallback(() => {
    navigate('/auth/login', {
      replace: true,
      state: { sessionExpiredMessage: 'Your session expired due to inactivity.' },
    });
  }, [navigate]);

  const broadcastLogout = useCallback((reason: string) => {
    const payload: SessionEventMessage = { type: 'logout', reason, at: Date.now() };
    channelRef.current?.postMessage(payload);
    try {
      localStorage.setItem(STORAGE_EVENT_KEY, JSON.stringify(payload));
    } catch {
      if (import.meta.env.DEV) {
        console.warn('[auth] unable to persist inactivity logout event to localStorage.');
      }
    }
  }, []);

  const finalizeLocalLogout = useCallback(() => {
    setShowWarning(false);
    setSecondsLeft(Math.floor(WARNING_BEFORE_MS / 1000));
    clearTimers();
    hardClearClientState();
    navigateToLoginWithExpiredMessage();
  }, [clearTimers, hardClearClientState, navigateToLoginWithExpiredMessage]);

  const performLogout = useCallback(async (broadcast: boolean) => {
    if (loggingOutRef.current) return;
    loggingOutRef.current = true;
    try {
      if (broadcast) {
        broadcastLogout(LOGOUT_REASON_INACTIVITY);
      }
      try {
        await logout();
      } catch {
        // ignore backend logout errors and still force local cleanup
      }
      finalizeLocalLogout();
    } finally {
      loggingOutRef.current = false;
    }
  }, [broadcastLogout, finalizeLocalLogout, logout]);

  const startCountdown = useCallback(() => {
    setSecondsLeft(Math.floor(WARNING_BEFORE_MS / 1000));
    if (countdownIntervalRef.current !== null) {
      window.clearInterval(countdownIntervalRef.current);
    }
    countdownIntervalRef.current = window.setInterval(() => {
      setSecondsLeft((current) => (current > 0 ? current - 1 : 0));
    }, 1000);
  }, []);

  const resetTimers = useCallback(() => {
    if (!shouldTrack || loggingOutRef.current) return;
    clearTimers();
    setShowWarning(false);
    setSecondsLeft(Math.floor(WARNING_BEFORE_MS / 1000));
    warningTimerRef.current = window.setTimeout(() => {
      setShowWarning(true);
      startCountdown();
    }, IDLE_TIMEOUT_MS - WARNING_BEFORE_MS);
    logoutTimerRef.current = window.setTimeout(() => {
      void performLogout(true);
    }, IDLE_TIMEOUT_MS);
  }, [clearTimers, performLogout, shouldTrack, startCountdown]);

  const handleKeepAlive = useCallback(async () => {
    try {
      await authService.keepAlive();
      resetTimers();
    } catch {
      await performLogout(true);
    }
  }, [performLogout, resetTimers]);

  useEffect(() => {
    if (!channelRef.current && typeof BroadcastChannel !== 'undefined') {
      channelRef.current = new BroadcastChannel(CHANNEL_NAME);
    }
    const channel = channelRef.current;
    if (!channel) return;
    const onMessage = (event: MessageEvent<SessionEventMessage>) => {
      if (event.data?.type === 'logout' && shouldTrack) {
        void performLogout(false);
      }
    };
    channel.addEventListener('message', onMessage);
    return () => channel.removeEventListener('message', onMessage);
  }, [performLogout, shouldTrack]);

  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key !== STORAGE_EVENT_KEY || !event.newValue || !shouldTrack) return;
      try {
        const parsed = JSON.parse(event.newValue) as SessionEventMessage;
        if (parsed.type === 'logout') {
          void performLogout(false);
        }
      } catch {
        // ignore malformed storage events
      }
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, [performLogout, shouldTrack]);

  useEffect(() => {
    if (!shouldTrack) {
      clearTimers();
      setShowWarning(false);
      return;
    }

    const events: Array<keyof WindowEventMap> = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart', 'click'];
    const onActivity = () => resetTimers();
    events.forEach((eventName) => window.addEventListener(eventName, onActivity, { passive: true }));
    resetTimers();

    return () => {
      events.forEach((eventName) => window.removeEventListener(eventName, onActivity));
      clearTimers();
    };
  }, [clearTimers, resetTimers, shouldTrack]);

  useEffect(() => {
    if (!shouldTrack) return;
    resetTimers();
  }, [location.pathname, location.search, resetTimers, shouldTrack]);

  useEffect(() => () => {
    clearTimers();
    channelRef.current?.close();
    channelRef.current = null;
  }, [clearTimers]);

  if (!showWarning || !shouldTrack) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 px-4">
      <div className="responsive-modal-panel w-full max-w-md rounded-2xl border border-slate-200 bg-white p-4 shadow-xl sm:p-6">
        <h2 className="text-lg font-semibold text-slate-900">Are you still there?</h2>
        <p className="mt-2 text-sm text-slate-600">
          You will be logged out in <span className="font-semibold text-slate-900">{secondsLeft}s</span> due to inactivity.
        </p>
        <div className="mt-5 flex flex-col gap-3 sm:flex-row">
          <Button type="button" onClick={() => void handleKeepAlive()} className="w-full sm:flex-1">
            Yes, keep me signed in
          </Button>
          <Button type="button" className="w-full bg-primary-600 hover:bg-primary-700 sm:flex-1" onClick={() => void performLogout(true)}>
            Logout now
          </Button>
        </div>
      </div>
    </div>
  );
};



