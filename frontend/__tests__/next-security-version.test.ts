/**
 * Security regression guard for CVE-2025-29927
 * (Authorization Bypass in Next.js Middleware).
 *
 * This application relies on Next.js middleware (`src/middleware.ts`) to
 * enforce authentication on protected routes. CVE-2025-29927 allows that
 * middleware to be bypassed via a crafted `x-middleware-subrequest` header on
 * Next.js versions before 14.2.25. This test pins a security floor so the
 * dependency can never be regressed back to a vulnerable release.
 *
 * Fixed in the 14.2.x line: 14.2.25 (and later hardening, e.g. 14.2.35 for the
 * December 2025 RSC advisories).
 */
import fs from 'fs';
import path from 'path';

/** Minimum non-vulnerable version on the 14.2.x line. */
const MIN_MAJOR = 14;
const MIN_MINOR = 2;
const MIN_PATCH = 25;

function parseVersion(range: string): [number, number, number] {
  const cleaned = range.replace(/^[^\d]*/, '');
  const [major, minor, patch] = cleaned.split('.').map((n) => parseInt(n, 10));
  return [major, minor, patch];
}

function isAtLeastFloor([major, minor, patch]: [number, number, number]): boolean {
  if (major !== MIN_MAJOR) return major > MIN_MAJOR;
  if (minor !== MIN_MINOR) return minor > MIN_MINOR;
  return patch >= MIN_PATCH;
}

describe('Next.js security version floor (CVE-2025-29927)', () => {
  const pkg = JSON.parse(
    fs.readFileSync(path.join(__dirname, '..', 'package.json'), 'utf-8'),
  );

  it('pins next to a patched version (>= 14.2.25)', () => {
    const version = parseVersion(pkg.dependencies.next);
    expect(isAtLeastFloor(version)).toBe(true);
  });

  it('keeps eslint-config-next aligned with the patched next version', () => {
    const version = parseVersion(pkg.devDependencies['eslint-config-next']);
    expect(isAtLeastFloor(version)).toBe(true);
  });
});
