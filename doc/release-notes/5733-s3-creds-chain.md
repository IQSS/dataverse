# Providing S3 Storage Credentials via MicroProfile Config

With this release, you may use two new options to pass an access key identifier and a secret access key for S3-based
storage definitions without creating the files used by the AWS CLI tools (`~/.aws/config` & `~/.aws/credentials`).

This has been added to ease setups using containers (Docker, Podman, Kubernetes, OpenShift) or testing and developing
installations. Find added [documentation and a word of warning in the installation guide](https://guides.dataverse.org/en/latest/installation/config.html#s3-mpconfig).