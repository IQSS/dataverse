## Fast Redeploy Scripts for Container-Based Development

Three new shell scripts in `scripts/dev/` enable fast iterative development for Dataverse contributors working with the container-based development environment:

- **`dev-start-frd.sh`**: One-time setup to build and deploy an exploded WAR in the dev stack
- **`dev-frd.sh`**: Incremental recompile + redeploy (~12s vs. ~54s for full rebuilds, 4.5x faster)
- **`dev-down-frd.sh`**: Stop and remove dev containers

This command-line workflow provides a fast feedback loop for developers who prefer CLI-based development or use lightweight editors like VS Code or Vim, complementing the existing IDE-based hot reload options (IntelliJ Ultimate, NetBeans).

# New Scripts

- **`dev-start-frd.sh`**: Initial setup (full build → exploded WAR → start containers)
- **`dev-frd.sh`**: Incremental recompile + redeploy (~12s vs. ~54s for traditional full rebuild workflow, 4.5x faster)
- **`dev-down-frd.sh`**: Clean shutdown of dev environment

# New Files

- **`docker-compose.override.yml`**: Increases memory limits to 8GB (from the 2GB limit set for GitHub Actions CI) for local development. Automatically used by the fast-redeploy scripts.

# Key Features

- No infrastructure changes (works with existing docker-compose-dev.yml)
- Optional workflow (doesn't affect other development approaches)
- Completes in ~12 seconds instead of ~54 seconds after code changes (4.5x faster)

**Note:** Performance timings may vary depending on your hardware configuration.

### Typical Workflow

```bash
# One-time setup
./scripts/dev/dev-start-frd.sh

# Make code changes...

# Fast redeploy
./scripts/dev/dev-frd.sh

# Repeat as needed

# When finished
./scripts/dev/dev-down-frd.sh
```

### Documentation

See the [Fast Redeploy (Command-Line)](https://guides.dataverse.org/en/latest/container/dev-usage.html#dev-fast-redeploy) section in the Container Guide for complete usage instructions and limitations.
