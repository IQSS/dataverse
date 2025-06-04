### Ability to pin to a specific Dataverse version in Docker (and more)

Container image management has been enhanced to provide better support for multiple Dataverse releases and improved maintenance workflows.

**Versioned Image Tags**: Application ("dataverse") and Config Baker [images on Docker Hub](https://hub.docker.com/u/gdcc) now have versioned tags, supporting the latest three Dataverse software releases. This enables users to pin to specific versions (e.g. 6.7), providing better stability for production deployments. Previously, the "alpha" tag could be used, but it was always overwritten by the latest release. Now, you can choose the 6.7 tag, for example, to stay on that version.

**Backport Support**: Application and Config Baker image builds now support including code backports for past releases, enabling the delivery of security fixes and critical updates to older (supported) versions.

**Enhanced Documentation**: Container image [documentation](https://dataverse-guide--11477.org.readthedocs.build/en/11477/container/index.html) has been updated to reflect the new versioning scheme and maintenance processes.

**Config Baker Base Image Change**: The Config Baker image has been migrated from Alpine to Ubuntu as its base operating system, aligning with other container images in the project for consistency and better compatibility. The past releases have not been migrated, only future releases (6.7+) will use Ubuntu.

**Workflow Responsibility Split**: GitHub Actions workflows for containers have been reorganized with a clear separation of concerns:

- `container_maintenance.yml` handles all release-time and maintenance activities
- Other workflows focus solely on preview images for development merges and pull requests

These improvements provide more robust container image lifecycle management, better security update delivery, and clearer operational procedures for both development and production environments.
See also the [Container Guide](https://dataverse-guide--11477.org.readthedocs.build/en/11477/container/index.html), #10618, and #11477.

## Notes for documentation writers

Sphinx has been upgraded to 7.4.0 and new dependencies been added, including semver. Please re-run the `pip install -r requirements.txt` setup [step](https://guides.dataverse.org/en/6.7/contributor/documentation.html#installing-sphinx) to upgrade your environment. Otherwise you might see an error like `ModuleNotFoundError: No module named 'semver'`.
