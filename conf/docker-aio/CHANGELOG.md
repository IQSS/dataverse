# Changelog

All notable changes to this section of the project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.1] - things needing to be fixed (2022.08.02)

- [ ] unzip:  cannot find or open dvinstall.zip (on build scripts); feel the install directory needs to be set differently in [/scripts/installer/Makefile](/scripts/installer/Makefile)
- [ ] need to resolve where `dvinstall.zip` is installed (the dv/deps directory should be used for the dvinstaller); `conf/docker-aio/setupIT.bash` is expecting the `dvinstall.zip` file under `/opt/dv`; `conf/docker-aio/install.bash` is expecting the `dvinstall.zip` file under `conf/docker-aio`;  `dataverse/conf/docker-aio/1prep.sh` seems to be trying to save the `dvinstall.zip` file to `../../conf/docker-aio/dv/install`; `/.gitignore` is expecting `dvinstall.zip` under `scripts/installer/dvinstall.zip`
- [ ] the references to specific versions of Maven (such as [https://downloads.apache.org/maven/maven-3/3.8.4/binaries/apache-maven-3.8.4-bin.tar.gz]) should probably be set in a configuration file and a script created that checks that versions of the files are accessible first thing to ensure all proper resources are available for the Docker build
- [ ] search for `dvinstall` and fix references in the documentation and other
- [x] dataverse/conf/docker-aio/ needs a CHANGELOG.md
