const ACCESS_TOKEN_KEY = 'edurite_access_token';
const REFRESH_TOKEN_KEY = 'edurite_refresh_token';
const USER_KEY = 'edurite_user';
const STORAGE_KEY_PREFIX = 'edurite_';

const storages = [localStorage, sessionStorage];

const getFromStorage = (key: string) => storages.map((storage) => storage.getItem(key)).find((value) => value !== null) ?? null;
const hasInStorage = (storage: Storage, key: string) => storage.getItem(key) !== null;
const removeManagedKeys = (storage: Storage) => {
  const keysToRemove: string[] = [];
  for (let index = 0; index < storage.length; index += 1) {
    const key = storage.key(index);
    if (key?.startsWith(STORAGE_KEY_PREFIX)) {
      keysToRemove.push(key);
    }
  }
  keysToRemove.forEach((key) => storage.removeItem(key));
};

export const authStore = {
  getAccessToken: () => getFromStorage(ACCESS_TOKEN_KEY),
  getRefreshToken: () => getFromStorage(REFRESH_TOKEN_KEY),
  shouldPersistSession: () => hasInStorage(localStorage, ACCESS_TOKEN_KEY) || hasInStorage(localStorage, REFRESH_TOKEN_KEY) || hasInStorage(localStorage, USER_KEY),
  getUser: () => {
    const user = getFromStorage(USER_KEY);
    if (!user) return null;

    try {
      return JSON.parse(user);
    } catch {
      storages.forEach((storage) => storage.removeItem(USER_KEY));
      return null;
    }
  },
  setTokens: (accessToken: string, refreshToken?: string, rememberMe = true) => {
    const primaryStorage = rememberMe ? localStorage : sessionStorage;
    const secondaryStorage = rememberMe ? sessionStorage : localStorage;
    primaryStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    secondaryStorage.removeItem(ACCESS_TOKEN_KEY);
    if (refreshToken) {
      primaryStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
      secondaryStorage.removeItem(REFRESH_TOKEN_KEY);
    } else {
      storages.forEach((storage) => storage.removeItem(REFRESH_TOKEN_KEY));
    }
  },
  setUser: (user: unknown, rememberMe = true) => {
    const primaryStorage = rememberMe ? localStorage : sessionStorage;
    const secondaryStorage = rememberMe ? sessionStorage : localStorage;
    primaryStorage.setItem(USER_KEY, JSON.stringify(user));
    secondaryStorage.removeItem(USER_KEY);
    window.dispatchEvent(new CustomEvent('edurite-auth-updated', { detail: { user } }));
  },
  clear: () => {
    storages.forEach((storage) => {
      removeManagedKeys(storage);
    });
    window.dispatchEvent(new Event('edurite-auth-cleared'));
  },
};
