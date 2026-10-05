import type { Page } from "@playwright/test";

export interface LoginCredentials {
  username: string;
  password: string;
}

/**
 * A LoginAdapter encapsulates one complete end-to-end authentication flow,
 * starting from the Dataverse home page (after the "Log In" button has been
 * confirmed visible) and finishing with the browser landed on an authenticated
 * Dataverse page.
 */
export interface LoginAdapter {
  login(page: Page, credentials: LoginCredentials): Promise<void>;
}

export type LoginAdapterName =
  | "shibboleth-direct"
  | "incommon-seamlessaccess"
  | "builtin";
