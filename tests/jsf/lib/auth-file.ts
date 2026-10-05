/**
 * Derives a per-endpoint, per-browser auth storage-state file path from
 * BASE_URL and an optional browser name.
 *
 * Example:
 *   BASE_URL=https://dataverse-staging.rdmc.unc.edu, browser="firefox"
 *   → playwright/.auth/dataverse-staging-rdmc-unc-edu-firefox.json
 *
 * This ensures switching BASE_URL between instances, or running against
 * multiple browsers, never overwrites a previously saved session — each
 * endpoint + browser combination keeps its own cookie jar.
 */
export function authFilePath(browser?: string): string {
  const url = (process.env.BASE_URL ?? "default").trim();
  const slug = url
    .replace(/^https?:\/\//, "") // strip scheme
    .replace(/\/$/, "") // strip trailing slash
    .replace(/[^a-z0-9]/gi, "-") // any non-alphanumeric → dash
    .toLowerCase();
  const suffix = browser ? `-${browser}` : "";
  return `playwright/.auth/${slug}${suffix}.json`;
}
