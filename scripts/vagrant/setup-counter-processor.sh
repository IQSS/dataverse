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
COMMIT='7974dad259465ba196ef639f48dea007cae8f9ac'
UNZIPPED_DIR="counter-processor-$COMMIT"
if [ ! -e $UNZIPPED_DIR ]; then
  ZIP_FILE="${COMMIT}.zip"
  echo "Downloading and unzipping $ZIP_FILE"
  wget https://github.com/CDLUC3/counter-processor/archive/$ZIP_FILE
  unzip $ZIP_FILE
fi
cd $UNZIPPED_DIR
echo "Installation of the GeoLite2 country database for counter-processor can no longer be automated. See the Installation Guide for the manual installation process."
pip3 install -r requirements.txt
# For now, parsing sample_logs/counter_2018-05-08.log
for i in `echo {00..31}`; do
  # avoid errors like: No such file or directory: 'sample_logs/counter_2018-05-01.log'
  touch sample_logs/counter_2018-05-$i.log
done
#LOG_GLOB="sample_logs/counter_2018-05-*.log"
#START_DATE="2018-05-08"
#END_DATE="2018-05-09"
CONFIG_FILE=/dataverse/scripts/vagrant/counter-processor-config.yaml python3.6 main.py
