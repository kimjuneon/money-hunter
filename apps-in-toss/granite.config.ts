import { defineConfig } from '@apps-in-toss/web-framework/config';

export default defineConfig({
  appName: 'gold-hunter',
  brand: {
    displayName: '머니헌터',
    primaryColor: '#2563EB',
    icon: 'https://money-hunter-prod-4qddpaimyq-du.a.run.app/assets/app-logo.png',
  },
  web: {
    host: 'localhost',
    port: 5173,
    commands: {
      dev: 'vite dev',
      build: 'vite build',
    },
  },
  permissions: [],
  webViewProps: {
    type: 'game',
    allowsInlineMediaPlayback: true,
  },
  outdir: 'dist',
});
