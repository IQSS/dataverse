#!/bin/bash
echo "Setting up counter-processor"
echo "Installing dependencies"
yum -y install unzip
yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
# EPEL provides Python 3.6.6, new enough (3.6.4 in .python-version)
yum -y install python36 jq
# "ensurepip" tip from https://stackoverflow.com/questions/50408941/recommended-way-to-install-pip3-on-centos7/52518512#52518512
python3.6 -m ensurepip
COUNTER_USER=counter
echo "Ensuring Unix user '$COUNTER_USER' exists"
useradd $COUNTER_USER || :
COMMIT='a73dbced06f0ac2f0d85231e4d9dd4f21bee8487'
UNZIPPED_DIR="counter-processor-$COMMIT"
if [ ! -e $UNZIPPED_DIR ]; then
  ZIP_FILE="${COMMIT}.zip"
  echo "Downloading and unzipping $ZIP_FILE"
  wget https://github.com/CDLUC3/counter-processor/archive/$ZIP_FILE
  unzip $ZIP_FILE
  # wget https://geolite.maxmind.com/download/geoip/database/GeoLite2-Country.tar.gz
  # tar xfz GeoLite2-Country.tar.gz
  # TODO: put GeoLite2-Country.mmdb in maxmind_geoip directory.
fi
cd $UNZIPPED_DIR
pip3 install -r requirements.txt
# trying to parse sample_logs/counter_2018-05-08.log
cp -r sample_logs log
# `touch` to avoid this error: No such file or directory: 'log/counter_2018-05-01.log'
for i in `echo {00..31}`; do
  touch log/counter_2018-05-$i.log
done
CONFIG_FILE=/dataverse/scripts/vagrant/counter-processor-config.yaml
LOG_GLOB="sample_logs/counter_2018-05-*.log"
START_DATE="2018-05-08"
END_DATE="2018-05-09"
python36 main.py
