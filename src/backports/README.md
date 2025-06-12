# Backports Directory

This directory contains patch files for backporting changes across different Dataverse releases.
These changes can be features, build enhancements, security patches, and more.

## Directory Structure

The backports directory is organized by "release version" (or "release tag") folders.
Each folder name corresponds to a [Dataverse release](https://github.com/IQSS/dataverse/releases) and [its associated Git tag](https://github.com/IQSS/dataverse/tags):

```
src/backports/
├── v6.4/ # Patches for Dataverse 6.4
├── v6.5/ # Patches for Dataverse 6.5 
├── v6.6/ # Patches for Dataverse 6.6
└── ...
```

Each version folder contains numbered patch files that modify specific components.

For example:
- `001-parent-pom.xml.patch` -> Modifications to the parent POM configuration
- `002-pom.xml.patch` -> Changes to the main POM file
- Additional patches as needed.

## Intended Usage

The patch files in each release folder are designed to be applied in numerical order.

Currently, they are primarily used by the container maintenance workflows (see `.github/workflows/container_maintenance.yml`) to ensure maintenance script compatibility across different Dataverse versions.
See also the "three releases back" support promise at https://guides.dataverse.org/en/latest/container/app-image.html#tags-for-production-use.

*Note: The backport patches mechanism is by no means limited to usage in a container context.*
*They can be applied manually or from some other automated release process to backports changes to older releases.*

## Creating Patch Files

To create a new patch file using `git diff`, follow these steps:

### Example: Creating a POM patch

1. **Make your changes** to the target file(s) on the appropriate branch (usually your feature branch).
2. **Head to the root** of the Git repository.
3. **Generate the patch** using `git diff`:
   ```bash
   # Create a patch for changes to pom.xml, comparing with the pom.xml contained in the v6.5 tag:
   git --no-pager diff v6.5 pom.xml > src/backports/v6.5/002-pom.xml.patch
   
   # Or create a patch for multiple files:
   git --no-pager diff v6.5 modules/dataverse-parent/pom.xml pom.xml > src/backports/v6.5/003-multi-pom.patch
   
   # Create a patch from staged changes
   git diff --cached > src/backports/v6.5/004-staged-changes.patch
   ```

4. **Review the patch** to ensure it contains only the intended changes:
   ```bash
   cat src/backports/v6.5/002-pom.xml.patch
   ```
   
5. **Repeat for other tags** as necessary.

### Patch Naming Convention

Use the following naming pattern:
- `001-` prefix with three-digit numbering for ordering
- Descriptive name indicating what is being patched
- `.patch` file extension

Examples:
- `001-parent-pom.xml.patch`
- `002-pom.xml.patch`
- `003-dockerfile-updates.patch`

## Integration with CI/CD

These patches are automatically applied during the container maintenance workflows to ensure that older release versions can be built with updated dependencies and configurations while maintaining compatibility.
The patches support the multi-version container image strategy that builds and maintains Docker images for the current development branch plus the last three released versions.
See also the "three releases back" support promise at https://guides.dataverse.org/en/latest/container/app-image.html#tags-for-production-use.

In the future, other automations may pick up the patches to release updated WAR files or similar.