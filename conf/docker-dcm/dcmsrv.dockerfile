# build from repo root
FROM centos:6
RUN yum install -y epel-release
COPY dcm-0.1-1.noarch.rpm /tmp/
COPY bashrc /root/.bashrc
COPY test_install.sh /root/
RUN yum localinstall -y /tmp/dcm-0.1-1.noarch.rpm
RUN pip install -r /opt/dcm/requirements.txt
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
