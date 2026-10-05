/**
 * Login Adapter Factory
 *
 * Returns the correct LoginAdapter implementation based on the
 * `LOGIN_ADAPTER` environment variable.
 *
 * Supported adapter names (case-insensitive):
 *
 *   shibboleth-direct        – Direct IdP selector embedded in the Dataverse
 *                              login page (e.g. #idpSelectSelector)
 *   incommon-seamlessaccess  – InCommon / SeamlessAccess waypoint flow
 *   builtin                  – Dataverse built-in username / password form
 *
 * Example .env entry:
 *
 *   LOGIN_ADAPTER=incommon-seamlessaccess
 *
 * Usage in a setup file:
 *
 *   import { getLoginAdapter } from "../../lib/login-adapters";
 *   const adapter = getLoginAdapter();
 *   await adapter.login(page, credentials);
 */

export type { LoginAdapter, LoginCredentials, LoginAdapterName } from "./types";
export { ShibbolethDirectAdapter } from "./shibboleth-direct";
export { InCommonSeamlessAccessAdapter } from "./incommon-seamlessaccess";
export { BuiltinAdapter } from "./builtin";

import type { LoginAdapter, LoginAdapterName } from "./types";
import { ShibbolethDirectAdapter } from "./shibboleth-direct";
import { InCommonSeamlessAccessAdapter } from "./incommon-seamlessaccess";
import { BuiltinAdapter } from "./builtin";

/**
 * Returns the LoginAdapter configured via the `LOGIN_ADAPTER` env var.
 *
 * @throws {Error}  When the env var is missing or the adapter name is unknown.
 */
export function getLoginAdapter(): LoginAdapter {
  const adapterName = process.env.LOGIN_ADAPTER;

  if (!adapterName) {
    throw new Error(
      `Missing required environment variable "LOGIN_ADAPTER". ` +
        `Set it to one of: shibboleth-direct, incommon-seamlessaccess, builtin.`,
    );
  }

  const normalized = adapterName.trim().toLowerCase() as LoginAdapterName;

  switch (normalized) {
    case "shibboleth-direct":
      return new ShibbolethDirectAdapter();

    case "incommon-seamlessaccess":
      return new InCommonSeamlessAccessAdapter();

    case "builtin":
      return new BuiltinAdapter();

    default:
      throw new Error(
        `Unknown login adapter "${adapterName}" specified in "LOGIN_ADAPTER". ` +
          `Valid options are: shibboleth-direct, incommon-seamlessaccess, builtin.`,
      );
  }
}
