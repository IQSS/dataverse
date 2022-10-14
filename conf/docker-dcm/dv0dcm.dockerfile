# dv0 assumed to be image name for docker-aio
FROM dv0
RUN yum install -y bind-utils
COPY configure_dcm.sh /opt/dv/
COPY configure_rsal.sh /opt/dv/
COPY rsal-workflow2.json site-primary.json site-remote.json /opt/dv/
VOLUME /hold
