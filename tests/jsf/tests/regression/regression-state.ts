/**
 * Tiny persistence helpers for the regression suite.
 *
 * Template name and custom terms are written to local temp files that are
 * git-ignored. This lets each step in the test write once and read anywhere.
 */
import fs from "fs";
import path from "path";

const TEMPLATE_NAME_FILE = path.resolve(process.cwd(), ".regression-template-name");
const CUSTOM_TERMS_FILE  = path.resolve(process.cwd(), ".regression-custom-terms");

// ── Template name ────────────────────────────────────────────────────────────

export function writeTemplateName(name: string): void {
  fs.writeFileSync(TEMPLATE_NAME_FILE, name, "utf-8");
}

export function readTemplateName(): string {
  if (!fs.existsSync(TEMPLATE_NAME_FILE)) {
    throw new Error(
      `Regression template name file not found at:\n  ${TEMPLATE_NAME_FILE}\n` +
        `Run the regression test from the beginning to generate it.`,
    );
  }
  return fs.readFileSync(TEMPLATE_NAME_FILE, "utf-8").trim();
}

// ── Custom terms of use ───────────────────────────────────────────────────────

export function writeCustomTerms(terms: string): void {
  fs.writeFileSync(CUSTOM_TERMS_FILE, terms, "utf-8");
}

export function readCustomTerms(): string {
  if (!fs.existsSync(CUSTOM_TERMS_FILE)) {
    throw new Error(
      `Regression custom terms file not found at:\n  ${CUSTOM_TERMS_FILE}\n` +
        `Run the regression test from the beginning to generate it.`,
    );
  }
  return fs.readFileSync(CUSTOM_TERMS_FILE, "utf-8").trim();
}

// ── Locally FAIR dataverse identifier ────────────────────────────────────────

const FAIR_DATAVERSE_ID_FILE = path.resolve(
  process.cwd(),
  ".regression-fair-dataverse-id",
);

export function writeFairDataverseId(id: string): void {
  fs.writeFileSync(FAIR_DATAVERSE_ID_FILE, id, "utf-8");
}

export function readFairDataverseId(): string {
  if (!fs.existsSync(FAIR_DATAVERSE_ID_FILE)) {
    throw new Error(
      `Regression FAIR dataverse ID file not found at:\n  ${FAIR_DATAVERSE_ID_FILE}\n` +
        `Run the regression test from the beginning to generate it.`,
    );
  }
  return fs.readFileSync(FAIR_DATAVERSE_ID_FILE, "utf-8").trim();
}
