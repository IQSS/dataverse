# get latest sphinx image with latext pdf
FROM sphinxdoc/sphinx-latexpdf:latest

RUN export DEBIAN_FRONTEND=noninteractive \
  && apt-get update && apt-get install --yes --no-install-recommends wget rsync git && \
    apt-get autoremove -y && \
    pip3 install --upgrade pip setuptools && \
    rm -r /root/.cache

WORKDIR /docs
ADD requirements.txt /docs
RUN pip3 install -r requirements.txt
