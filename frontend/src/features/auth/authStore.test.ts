import { afterEach, describe, expect, it, vi } from 'vitest';

type MockStorageState = {
  entries: Map<string, string>;
  storage: Storage;
};

const createMockStorage = (initial: Record<string, string> = {}): MockStorageState => {
  const entries = new Map(Object.entries(initial));
  const storage: Storage = {
    get length() {
      return entries.size;
    },
    clear: () => entries.clear(),
    getItem: (key: string) => entries.get(key) ?? null,
    key: (index: number) => Array.from(entries.keys())[index] ?? null,
    removeItem: (key: string) => {
      entries.delete(key);
    },
    setItem: (key: string, value: string) => {
      entries.set(key, value);
    },
  };
  return { entries, storage };
};

const setWindow = (localStorageFactory: () => Storage, sessionStorageFactory: () => Storage) => {
  const windowMock = { dispatchEvent: vi.fn() } as unknown as Window;
  Object.defineProperty(windowMock, 'localStorage', { configurable: true, get: localStorageFactory });
  Object.defineProperty(windowMock, 'sessionStorage', { configurable: true, get: sessionStorageFactory });
  Object.defineProperty(globalThis, 'window', { configurable: true, value: windowMock });
};

describe('authStore', () => {
  afterEach(() => {
    vi.resetModules();
    vi.unstubAllGlobals();
  });

  it('does not crash when browser storage access throws', async () => {
    setWindow(
      () => {
        throw new Error('SecurityError');
      },
      () => createMockStorage().storage,
    );

    const { authStore } = await import('@/features/auth/authStore');

    expect(authStore.getAccessToken()).toBeNull();
    expect(authStore.getRefreshToken()).toBeNull();
    expect(authStore.getUser()).toBeNull();
    expect(authStore.shouldPersistSession()).toBe(false);
  });

  it('clears only malformed stored user data and preserves valid tokens for recovery', async () => {
    const local = createMockStorage({
      edurite_access_token: 'access-token',
      edurite_user: '{broken-json',
    });
    const session = createMockStorage();
    setWindow(() => local.storage, () => session.storage);

    const { authStore } = await import('@/features/auth/authStore');

    expect(authStore.getUser()).toBeNull();
    expect(authStore.getAccessToken()).toBe('access-token');
    expect(local.entries.has('edurite_user')).toBe(false);
  });
});
