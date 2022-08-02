# Changelog

All notable changes to this section of the project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.1] - things needing to be fixed

- [ ] the dv/deps directory should be used for the dvinstaller
- [ ] unzip:  cannot find or open dvinstall.zip (on build scripts); feel like the install directory needs to be set differently in https://github.com/IQSS/dataverse/blob/3c616d2c386a63e7b60e585a29f11c9f14478d0b/scripts/installer/Makefile
- [ ] conf/docker-aio/setupIT.bash is expecting the `dvinstall.zip` file under /opt/dv; the conf/docker-aio/install.bash is expecting the `dvinstall.zip` file under `conf/docker-aio`
- [x] dataverse/conf/docker-aio/ needs a CHANGELOG.md
