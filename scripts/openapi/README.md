# OpenAPI Quality Checks

This directory contains the Dataverse vacuum ruleset used to check the generated
OpenAPI document while preserving known legacy API shapes.

## vacuum

vacuum is a command-line linter and quality checker for OpenAPI, AsyncAPI, and
JSON Schema documents. It is compatible with Spectral rulesets, so the Dataverse
ruleset can extend the built-in `vacuum:oas` recommended profile and disable
rules that are noisy for the current API surface.

Project and documentation:

- GitHub: https://github.com/daveshanley/vacuum
- Docs: https://quobix.com/vacuum

Install with Homebrew:

```bash
brew install --cask daveshanley/vacuum/vacuum
```

Install with npm:

```bash
npm i -g @quobix/vacuum
```

Install with curl:

```bash
curl -fsSL https://quobix.com/scripts/install_vacuum.sh | sh
```

## Checking Dataverse OpenAPI

Generate the OpenAPI document from the repository root:

```bash
mvn -q -DskipTests process-classes
```

The generated JSON is written to:

```text
target/classes/META-INF/openapi.json
```

Run the vacuum checks from the repository root:

```bash
vacuum lint -r scripts/openapi/vacuum-recommended.yaml target/classes/META-INF/openapi.json
```

For a fuller report:

```bash
vacuum report -r scripts/openapi/vacuum-recommended.yaml target/classes/META-INF/openapi.json
```

For the interactive dashboard:

```bash
vacuum dashboard -r scripts/openapi/vacuum-recommended.yaml target/classes/META-INF/openapi.json
```

The ruleset extends `vacuum:oas` recommended rules, with legacy/noisy checks
disabled where Dataverse intentionally keeps historical endpoint names or lacks
examples. Request-body findings remain enabled so unused body parameters on GET
endpoints are reported and can be removed from source.
