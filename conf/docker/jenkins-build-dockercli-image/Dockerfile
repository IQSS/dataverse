FROM docker:27.3.1-alpine3.20

RUN apk add openjdk8

RUN addgroup -g 1000 jenkins && \
    adduser --uid 1000 --ingroup jenkins --no-create-home --disabled-password jenkins

RUN addgroup jenkins root
RUN addgroup jenkins docker

ENTRYPOINT []
