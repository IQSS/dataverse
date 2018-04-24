# build from repo root
FROM centos:6
RUN yum install -y epel-release
RUN yum install -y rsync openssh-clients jq curl
RUN useradd depositor
USER depositor
WORKDIR /home/depositor
