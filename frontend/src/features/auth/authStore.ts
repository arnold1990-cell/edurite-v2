const ACCESS_TOKEN_KEY = 'edurite_access_token';
const REFRESH_TOKEN_KEY = 'edurite_refresh_token';
const USER_KEY = 'edurite_user';
const STORAGE_KEY_PREFIX = 'edurite_';
const warnedStorageKeys = new Set<string>();

const createMemoryStorage = (): Storage => {
  const memory = new Map<string, string>();
  return {
    get length() {
      return memory.size;
    },
    clear: () => memory.clear(),
    getItem: (key: string) => memory.get(key) ?? null,
    key: (index: number) => Array.from(memory.keys())[index] ?? null,
    removeItem: (key: string) => {
      memory.delete(key);
    },
    setItem: (key: string, value: string) => {
      memory.set(key, value);
    },
  };
};

const localFallbackStorage = createMemoryStorage();
const sessionFallbackStorage = createMemoryStorage();

const warnStorageFailure = (key: string, error: unknown) => {
  if (!import.meta.env.DEV || warnedStorageKeys.has(key)) {
    return;
  }
  warnedStorageKeys.add(key);
  console.warn(`[auth] ${key} is unavailable; falling back to in-memory session storage.`, error);
};

const resolveStorage = (key: 'localStorage' | 'sessionStorage'): Storage => {
  if (typeof window === 'undefined') {
    return key === 'localStorage' ? localFallbackStorage : sessionFallbackStorage;
  }
  try {
    return window[key];
  } catch (error) {
    warnStorageFailure(key, error);
    return key === 'localStorage' ? localFallbackStorage : sessionFallbackStorage;
  }
};

const getStorages = () => [resolveStorage('localStorage'), resolveStorage('sessionStorage')];

const withStorage = <T>(operation: () => T, fallback: T, label: string): T => {
  try {
    return operation();
  } catch (error) {
    warnStorageFailure(label, error);
    return fallback;
  }
};

const getFromStorage = (key: string) => getStorages()
  .map((storage, index) => withStorage(() => storage.getItem(key), null, index === 0 ? 'localStorage' : 'sessionStorage'))
  .find((value) => value !== null) ?? null;
const hasInStorage = (storage: Storage, key: string) => withStorage(() => storage.getItem(key) !== null, false, 'storage');
const removeManagedKeys = (storage: Storage) => {
  const keysToRemove: string[] = [];
  const length = withStorage(() => storage.length, 0, 'storage');
  for (let index = 0; index < length; index += 1) {
    const key = withStorage(() => storage.key(index), null, 'storage');
    if (key?.startsWith(STORAGE_KEY_PREFIX)) {
      keysToRemove.push(key);
    }
  }
  keysToRemove.forEach((key) => {
    withStorage(() => {
      storage.removeItem(key);
      return null;
    }, null, 'storage');
  });
};

export const authStore = {
  getAccessToken: () => getFromStorage(ACCESS_TOKEN_KEY),
  getRefreshToken: () => getFromStorage(REFRESH_TOKEN_KEY),
  shouldPersistSession: () => {
    const local = resolveStorage('localStorage');
    return hasInStorage(local, ACCESS_TOKEN_KEY) || hasInStorage(local, REFRESH_TOKEN_KEY) || hasInStorage(local, USER_KEY);
  },
  getUser: () => {
    const user = getFromStorage(USER_KEY);
    if (!user) return null;

    try {
      return JSON.parse(user);
    } catch {
      authStore.clearUser();
      return null;
    }
  },
  setTokens: (accessToken: string, refreshToken?: string, rememberMe = true) => {
    const local = resolveStorage('localStorage');
    const session = resolveStorage('sessionStorage');
    const primaryStorage = rememberMe ? local : session;
    const secondaryStorage = rememberMe ? session : local;
    withStorage(() => {
      primaryStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
      return null;
    }, null, rememberMe ? 'localStorage' : 'sessionStorage');
    withStorage(() => {
      secondaryStorage.removeItem(ACCESS_TOKEN_KEY);
      return null;
    }, null, rememberMe ? 'sessionStorage' : 'localStorage');
    if (refreshToken) {
      withStorage(() => {
        primaryStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
        return null;
      }, null, rememberMe ? 'localStorage' : 'sessionStorage');
      withStorage(() => {
        secondaryStorage.removeItem(REFRESH_TOKEN_KEY);
        return null;
      }, null, rememberMe ? 'sessionStorage' : 'localStorage');
    } else {
      getStorages().forEach((storage) => {
        withStorage(() => {
          storage.removeItem(REFRESH_TOKEN_KEY);
          return null;
        }, null, 'storage');
      });
    }
  },
  setUser: (user: unknown, rememberMe = true) => {
    const local = resolveStorage('localStorage');
    const session = resolveStorage('sessionStorage');
    const primaryStorage = rememberMe ? local : session;
    const secondaryStorage = rememberMe ? session : local;
    withStorage(() => {
      primaryStorage.setItem(USER_KEY, JSON.stringify(user));
      return null;
    }, null, rememberMe ? 'localStorage' : 'sessionStorage');
    withStorage(() => {
      secondaryStorage.removeItem(USER_KEY);
      return null;
    }, null, rememberMe ? 'sessionStorage' : 'localStorage');
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('edurite-auth-updated', { detail: { user } }));
    }
  },
  clearUser: () => {
    getStorages().forEach((storage) => {
      withStorage(() => {
        storage.removeItem(USER_KEY);
        return null;
      }, null, 'storage');
    });
  },
  debugSnapshot: () => {
    const local = resolveStorage('localStorage');
    const session = resolveStorage('sessionStorage');
    return {
      localStorageAuth: {
        accessToken: withStorage(() => local.getItem(ACCESS_TOKEN_KEY), null, 'localStorage'),
        refreshToken: withStorage(() => local.getItem(REFRESH_TOKEN_KEY), null, 'localStorage'),
        user: withStorage(() => local.getItem(USER_KEY), null, 'localStorage'),
      },
      sessionStorageAuth: {
        accessToken: withStorage(() => session.getItem(ACCESS_TOKEN_KEY), null, 'sessionStorage'),
        refreshToken: withStorage(() => session.getItem(REFRESH_TOKEN_KEY), null, 'sessionStorage'),
        user: withStorage(() => session.getItem(USER_KEY), null, 'sessionStorage'),
      },
    };
  },
  clear: () => {
    getStorages().forEach((storage) => {
      removeManagedKeys(storage);
    });
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new Event('edurite-auth-cleared'));
    }
  },
};
