# build from repo root
FROM centos:8
RUN yum install -y epel-release
RUN yum install -y rsync openssh-clients jq curl wget lynx
RUN useradd depositor
USER depositor
WORKDIR /home/depositor
