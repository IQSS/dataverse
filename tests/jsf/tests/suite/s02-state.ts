/**
 * Tiny persistence helper for the Section 2 dataverse identifier.
 *
 * The identifier is written to a local temp file that is git-ignored.
 * This lets the create-dataverse test write the identifier once and all
 * subsequent Section 2 tests (publish, theme, etc.) consume it even when
 * run independently.
 */
import fs from "fs";
import path from "path";

const STATE_FILE = path.resolve(process.cwd(), ".s2-dataverse-id");

/**
 * Persist the dataverse identifier to disk so later tests can consume it.
 * Called once at the end of 05-create-dataverse.
 */
export function writeDataverseId(id: string): void {
  fs.writeFileSync(STATE_FILE, id, "utf-8");
}

/**
 * Read the dataverse identifier written by 05-create-dataverse.
 * Throws a helpful error if the file doesn't exist (i.e. the create test
 * has never run in this workspace).
 */
export function readDataverseId(): string {
  if (!fs.existsSync(STATE_FILE)) {
    throw new Error(
      `Dataverse identifier state file not found at:\n  ${STATE_FILE}\n` +
        `Run 05-create-dataverse.spec.ts first to generate it.`,
    );
  }
  return fs.readFileSync(STATE_FILE, "utf-8").trim();
}
