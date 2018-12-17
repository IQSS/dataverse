# build from repo root
FROM centos:6
RUN yum install -y epel-release
ARG RPMFILE=dcm-0.5-0.noarch.rpm
COPY ${RPMFILE} /tmp/
COPY bashrc /root/.bashrc
COPY test_install.sh /root/
RUN yum localinstall -y /tmp/${RPMFILE}
RUN pip install -r /opt/dcm/requirements.txt
RUN pip install awscli==1.15.75
run export PATH=~/.local/bin:$PATH
RUN /root/test_install.sh
COPY rq-init-d /etc/init.d/rq
RUN useradd glassfish
COPY entrypoint-dcm.sh /
COPY healthcheck-dcm.sh /
EXPOSE 80
EXPOSE 22
VOLUME /hold
HEALTHCHECK CMD /healthcheck-dcm.sh
CMD ["/entrypoint-dcm.sh"]
