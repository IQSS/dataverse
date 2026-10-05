import { defineConfig } from "@playwright/test";
import fs from "fs";
import path from "path";
const process = (globalThis as any).process;

// Derive the auth file path at config-load time so both the setup project
// and the suite project reference the same per-endpoint, per-browser file.
// We inline the slug logic here (rather than importing lib/auth-file.ts)
// because playwright.config.ts is evaluated before TypeScript compilation.
function authFilePath(browser: string): string {
  const url = (process.env.BASE_URL ?? "default").trim();
  const slug = url
    .replace(/^https?:\/\//, "")
    .replace(/\/$/, "")
    .replace(/[^a-z0-9]/gi, "-")
    .toLowerCase();
  return `playwright/.auth/${slug}-${browser}.json`;
}

const AUTH_CHROMIUM = authFilePath("chromium");
const AUTH_FIREFOX = authFilePath("firefox");
const AUTH_WEBKIT = authFilePath("webkit");

/**
 * Load environment variables from .env at the repository root.
 * dotenv is a transitive dependency of @playwright/test — no extra install needed.
 */
// eslint-disable-next-line @typescript-eslint/no-require-imports
require("dotenv").config({ path: path.resolve(__dirname, ".env") });

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./tests",
  timeout: 180000,
  /* All tests within a project run in strict sequence; projects themselves
     run one at a time because workers is 1. */
  workers: 1,
  fullyParallel: false,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters
   * - "list"   → prints per-test pass/fail status live to stdout (visible in CI logs)
   * - "html"   → generates the HTML report artifact uploaded at the end of the job
   */
  reporter: [["list"], ["html"]],
  /* Shared settings for all projects. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    headless: true,

    launchOptions: {
      slowMo: 2500,
    },

    // Sets the default viewport size for all tests
    viewport: { width: 1280, height: 720 },

    /* Collect trace, video and screenshots only on failure to reduce artifact size. */
    trace: "retain-on-failure",

    video: {
      mode: "retain-on-failure",
      size: { width: 1280, height: 720 },
    },

    screenshot: {
      mode: "only-on-failure",
      fullPage: true,
    },
  },

  /* Configure the output directory for test artifacts such as screenshots, videos, traces, etc. */
  outputDir: "test-results/",

  /* Configure projects
   *
   * Execution order (workers: 1 → strictly sequential):
   *
   *   preflight
   *     → setup-chromium → suite-chromium
   *       → setup-firefox → suite-firefox
   *         → setup-webkit → suite-webkit
   *
   * "suite-firefox" declares a dependency on "suite-chromium" and
   * "suite-webkit" declares a dependency on "suite-firefox", which
   * enforces the Chromium → Firefox → WebKit ordering even if workers
   * is ever increased.
   */
  projects: [
    /* =========================================================
       1. Preflight — Chromium, no auth required, @standard only.
          The test itself returns immediately when SKIP_PREFLIGHT=true.
       ========================================================= */
    {
      name: "preflight",
      testMatch: /suite\/01-preflight\.spec\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "chromium",
        baseURL: process.env.BASE_URL,
      },
    },

    /* =========================================================
       2a. Auth setup — Chromium
       ========================================================= */
    {
      name: "setup-chromium",
      testMatch: /suite\/auth\.setup\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "chromium",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_CHROMIUM) && {
          storageState: AUTH_CHROMIUM,
        }),
      },
      dependencies: [],
    },

    /* =========================================================
       2b. Auth setup — Firefox
       ========================================================= */
    {
      name: "setup-firefox",
      testMatch: /suite\/auth\.setup\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "firefox",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_FIREFOX) && {
          storageState: AUTH_FIREFOX,
        }),
      },
      dependencies: [],
    },

    /* =========================================================
       2c. Auth setup — WebKit (Safari)
       ========================================================= */
    {
      name: "setup-webkit",
      testMatch: /suite\/auth\.setup\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "webkit",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_WEBKIT) && {
          storageState: AUTH_WEBKIT,
        }),
      },
      dependencies: [],
    },

    /* =========================================================
       3a. Main test suite — Chromium
           Runs first. Firefox suite depends on this completing.
       ========================================================= */
    {
      name: "suite-chromium",
      testMatch: /suite\/(?!auth\.setup|01-preflight)\d+.*\.spec\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "chromium",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_CHROMIUM) && {
          storageState: AUTH_CHROMIUM,
        }),
      },
      dependencies: ["setup-chromium"],
    },

    /* =========================================================
       3b. Main test suite — Firefox
           Runs after Chromium suite completes.
       ========================================================= */
    {
      name: "suite-firefox",
      testMatch: /suite\/(?!auth\.setup|01-preflight)\d+.*\.spec\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "firefox",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_FIREFOX) && {
          storageState: AUTH_FIREFOX,
        }),
      },
      dependencies: ["setup-firefox", "suite-chromium"],
    },

    /* =========================================================
       3c. Main test suite — WebKit (Safari)
           Runs after Firefox suite completes.
       ========================================================= */
    {
      name: "suite-webkit",
      testMatch: /suite\/(?!auth\.setup|01-preflight)\d+.*\.spec\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "webkit",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_WEBKIT) && {
          storageState: AUTH_WEBKIT,
        }),
      },
      dependencies: ["setup-webkit", "suite-firefox"],
    },

    /* =========================================================
       4a. Regression setup — Chromium
       ========================================================= */
    {
      name: "regression-setup-chromium",
      testMatch: /suite\/auth\.setup\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "chromium",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_CHROMIUM) && {
          storageState: AUTH_CHROMIUM,
        }),
      },
      dependencies: [],
    },

    /* =========================================================
       4b. Regression setup — Firefox
       ========================================================= */
    {
      name: "regression-setup-firefox",
      testMatch: /suite\/auth\.setup\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "firefox",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_FIREFOX) && {
          storageState: AUTH_FIREFOX,
        }),
      },
      dependencies: [],
    },

    /* =========================================================
       4c. Regression setup — WebKit
       ========================================================= */
    {
      name: "regression-setup-webkit",
      testMatch: /suite\/auth\.setup\.ts/,
      tsconfig: "./tests/suite/tsconfig.json",
      use: {
        browserName: "webkit",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_WEBKIT) && {
          storageState: AUTH_WEBKIT,
        }),
      },
      dependencies: [],
    },

    /* =========================================================
       4d. Regression suite — Chromium
       ========================================================= */
    {
      name: "regression-chromium",
      testMatch: /regression\/.*\.spec\.ts/,
      tsconfig: "./tests/regression/tsconfig.json",
      use: {
        browserName: "chromium",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_CHROMIUM) && {
          storageState: AUTH_CHROMIUM,
        }),
      },
      dependencies: ["regression-setup-chromium"],
    },

    /* =========================================================
       4e. Regression suite — Firefox
       ========================================================= */
    {
      name: "regression-firefox",
      testMatch: /regression\/.*\.spec\.ts/,
      tsconfig: "./tests/regression/tsconfig.json",
      use: {
        browserName: "firefox",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_FIREFOX) && {
          storageState: AUTH_FIREFOX,
        }),
      },
      dependencies: ["regression-setup-firefox", "regression-chromium"],
    },

    /* =========================================================
       4f. Regression suite — WebKit
       ========================================================= */
    {
      name: "regression-webkit",
      testMatch: /regression\/.*\.spec\.ts/,
      tsconfig: "./tests/regression/tsconfig.json",
      use: {
        browserName: "webkit",
        baseURL: process.env.BASE_URL,
        ...(fs.existsSync(AUTH_WEBKIT) && {
          storageState: AUTH_WEBKIT,
        }),
      },
      dependencies: ["regression-setup-webkit", "regression-firefox"],
    },
  ],
});
