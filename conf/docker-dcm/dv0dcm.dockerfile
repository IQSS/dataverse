# dv0 assumed to be image name for docker-aio
FROM dv0
COPY configure_dcm.sh /opt/dv/
VOLUME /hold
