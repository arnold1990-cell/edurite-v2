import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';

const parseEnvBoolean = (value: string | undefined): boolean => {
  if (!value) return false;
  return ['true', '1', 'yes', 'on'].includes(value.trim().toLowerCase());
};

const sanitizeEnvValue = (value: string | undefined): string => {
  if (!value) return '';
  return value.trim().replace(/^['"]|['"]$/g, '');
};

export default defineConfig(({ mode }) => {
  const frontendViteEnv = loadEnv(mode, process.cwd(), 'VITE_');
  const workspaceViteEnv = loadEnv(mode, resolve(process.cwd(), '..'), 'VITE_');
  const mergedViteEnv = {
    ...workspaceViteEnv,
    ...frontendViteEnv,
    ...Object.fromEntries(
      Object.entries(process.env).filter(([key]) => key.startsWith('VITE_')),
    ),
  };
  console.log('Loaded VITE vars:', mergedViteEnv);

  const frontendEnv = loadEnv(mode, process.cwd(), '');
  const workspaceEnv = loadEnv(mode, resolve(process.cwd(), '..'), '');

  const devServerPortRaw =
      frontendEnv.VITE_DEV_SERVER_PORT ||
      workspaceEnv.VITE_DEV_SERVER_PORT ||
      '';

  const parsedDevServerPort = parseInt(devServerPortRaw, 10);

  const devServerPort =
      Number.isFinite(parsedDevServerPort) && parsedDevServerPort > 0
          ? parsedDevServerPort
          : 5173;

  const googleClientId = sanitizeEnvValue(
      frontendEnv.VITE_GOOGLE_CLIENT_ID
      || frontendEnv.VITE_GOOGLE_OAUTH_CLIENT_ID
      || workspaceEnv.VITE_GOOGLE_CLIENT_ID
      || workspaceEnv.VITE_GOOGLE_OAUTH_CLIENT_ID,
  );
  const googleSignInEnabled = parseEnvBoolean(
      frontendEnv.VITE_GOOGLE_SIGNIN_ENABLED
      || frontendEnv.VITE_GOOGLE_OAUTH_ENABLED
      || workspaceEnv.VITE_GOOGLE_SIGNIN_ENABLED
      || workspaceEnv.VITE_GOOGLE_OAUTH_ENABLED,
  ) || Boolean(googleClientId);

  const allowedHosts = [
    'edurite.org',
    'www.edurite.org',
    'edurite.net',
    'www.edurite.net',
    'localhost',
    '127.0.0.1',
  ];

  const viteDefineEntries = Object.fromEntries(
    Object.entries(mergedViteEnv).map(([key, value]) => [
      `import.meta.env.${key}`,
      JSON.stringify(value),
    ]),
  );

  return {
    plugins: [react()],
    define: {
      ...viteDefineEntries,
      'import.meta.env.VITE_GOOGLE_CLIENT_ID': JSON.stringify(googleClientId),
      'import.meta.env.VITE_GOOGLE_OAUTH_CLIENT_ID': JSON.stringify(googleClientId),
      'import.meta.env.VITE_GOOGLE_SIGNIN_ENABLED': JSON.stringify(String(googleSignInEnabled)),
      'import.meta.env.VITE_GOOGLE_OAUTH_ENABLED': JSON.stringify(String(googleSignInEnabled)),
    },
    resolve: {
      alias: {
        '@': resolve(__dirname, './src'),
      },
    },
    server: {
      host: '0.0.0.0',
      port: devServerPort,
      allowedHosts,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          timeout: 120000,
          proxyTimeout: 120000,
        },
        '/oauth2': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          timeout: 120000,
          proxyTimeout: 120000,
        },
        '/login/oauth2': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          timeout: 120000,
          proxyTimeout: 120000,
        },
      },
    },
    preview: {
      host: '0.0.0.0',
      port: 5173,
      allowedHosts,
    },
  };
});
