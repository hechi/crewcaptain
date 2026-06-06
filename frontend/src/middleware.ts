export { auth as middleware } from '@/auth';

export const config = {
  matcher: [
    '/people/:path*',
    '/dashboard/:path*',
    '/triage/:path*',
    '/quick-notes/:path*',
    '/search/:path*',
    '/settings/:path*',
    '/notifications/:path*',
    '/audit-log/:path*',
    '/workspaces/:path*',
  ],
};
