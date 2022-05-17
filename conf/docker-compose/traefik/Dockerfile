# https://hub.docker.com/_/traefik
FROM traefik:v2.6

# Add certs to OS truststore
COPY *.crt /usr/local/share/ca-certificates/
# private key
COPY *.key /usr/local/share/ca-certificates/

# use HTTP connection for adding the needed ca-certificates package
# install it and then swap back to HTTPS
RUN sed -i 's,https,http,g' /etc/apk/repositories && \
    apk add --no-cache ca-certificates && \
    update-ca-certificates && \
    sed -i 's,http,https,g' /etc/apk/repositories && \
    apk update && apk upgrade && apk add --no-cache bash curl

RUN mkdir -p /configuration/
COPY certificates.yaml /configuration/certificates.yaml
