# Security and Compatibility Fixes to the Container Base Image

- Switch "wait-for" to "wait4x", aligned with the Configbaker Image
- Update "jattach" to v2.2
- Install AMD64 / ARM64 versions of tools as necessary
- Run base image as unprivileged user by default instead of `root` - this was an oversight from OpenShift changes
- Linux User, Payara Admin and Domain Master passwords:
  - Print hints about default, public knowledge passwords in place for
  - Enable replacing these passwords at container boot time
- Enable building with updates Temurin JRE image based on Ubuntu 24.04 LTS
- Fix entrypoint script troubles with pre- and postboot script files
- Unify location of files at CONFIG_DIR=/opt/payara/config, avoid writing to other places