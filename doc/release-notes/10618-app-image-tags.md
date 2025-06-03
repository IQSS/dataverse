### Container Image Versioning and Maintenance Improvements

Container image management has been enhanced to provide better support for multiple Dataverse releases and improved maintenance workflows.

**Versioned Image Tags**: Application and Config Baker images now follow the Bitnami pattern with versioned tags; supporting the latest three Dataverse software releases. This enables users to pin to specific versions and especially provides better stability for production deployments.

**Workflow Responsibility Split**: GitHub Actions workflows for containers have been reorganized with a clear separation of concerns:
- `container_maintenance.yml` handles all release-time and maintenance activities
- Other workflows focus solely on preview images for development merges and pull requests

**Backport Support**: Application and Config Baker image builds now support including code backports for past releases, enabling the delivery of security fixes and critical updates to older (supported) versions.

**Config Baker Base Image Change**: The Config Baker image has been migrated from Alpine to Ubuntu as its base operating system, aligning with other container images in the project for consistency and better compatibility. The past releases have not been migrated, only future releases (6.7+) will use Ubuntu.

**Enhanced Documentation**: Container image documentation has been updated to reflect the new versioning scheme and maintenance processes.

These improvements provide more robust container image lifecycle management, better security update delivery, and clearer operational procedures for both development and production environments.
