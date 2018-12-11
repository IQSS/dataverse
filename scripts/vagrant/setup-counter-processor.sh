#!/bin/bash
echo "Setting up counter-processor"
echo "Installing dependencies"
yum -y install unzip vim-enhanced
yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
# EPEL provides Python 3.6.6, new enough (3.6.4 in .python-version)
yum -y install python36 jq
# "ensurepip" tip from https://stackoverflow.com/questions/50408941/recommended-way-to-install-pip3-on-centos7/52518512#52518512
python3.6 -m ensurepip
# FIXME: actually use this dedicated "counter" user.
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
fi
cd $UNZIPPED_DIR
GEOIP_DIR='maxmind_geoip'
GEOIP_FILE='GeoLite2-Country.mmdb'
GEOIP_PATH_TO_FILE="$GEOIP_DIR/$GEOIP_FILE"
if [ ! -e $GEOIP_PATH_TO_FILE ]; then
  echo "let's do this thing"
  TARBALL='GeoLite2-Country.tar.gz'
  wget https://geolite.maxmind.com/download/geoip/database/$TARBALL
  tar xfz GeoLite2-Country.tar.gz
  # Glob (*) below because of directories like "GeoLite2-Country_20181204".
  GEOIP_UNTARRED_DIR='GeoLite2-Country_*'
  mv $GEOIP_UNTARRED_DIR/$GEOIP_FILE $GEOIP_PATH_TO_FILE
  rm -rf $TARBALL $GEOIP_UNTARRED_DIR
fi
pip3 install -r requirements.txt
# For now, parsing sample_logs/counter_2018-05-08.log
for i in `echo {00..31}`; do
  # avoid errors like: No such file or directory: 'sample_logs/counter_2018-05-01.log'
  touch sample_logs/counter_2018-05-$i.log
done
#LOG_GLOB="sample_logs/counter_2018-05-*.log"
#START_DATE="2018-05-08"
#END_DATE="2018-05-09"
CONFIG_FILE=/dataverse/scripts/vagrant/counter-processor-config.yaml python36 main.py
