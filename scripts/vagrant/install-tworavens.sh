#!/bin/bash
echo "This script is highly experimental and makes many assumptions about how Dataverse is running in Vagrant. Please consult the TwoRavens section of the Dataverse Installation Guide instead."
exit 1
cd /root
yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
yum install -y R R-devel
# FIXME: /dataverse is mounted in Vagrant but not other places
yum install -y /dataverse/doc/sphinx-guides/source/_static/installation/files/home/rpmbuild/rpmbuild/RPMS/x86_64/rapache-1.2.6-rpm0.x86_64.rpm
yum install -y gcc-gfortran # to build R packages
COMMIT=a6869eb28693d6df529e7cb3888c40de5f302b66
UNZIPPED=TwoRavens-$COMMIT
if [ ! -f $COMMIT ]; then
  wget https://github.com/IQSS/TwoRavens/archive/$COMMIT.zip
  unzip $COMMIT
  cd $UNZIPPED/r-setup
  ./r-setup.sh # This is expected to take a while. Look for lines like "Package Zelig successfully installed" and "Successfully installed Dataverse R framework".
fi
# FIXME: copy preprocess.R into Glassfish while running and overwrite it
# FIXME: enable TwoRavens by POSTing twoRavens.json. See note below about port 8888 vs 8080.
# TODO: programatically edit twoRavens.json to change "toolUrl" to "http://localhost:8888/dataexplore/gui.html"
curl -X POST -H 'Content-type: application/json' --upload-file /dataverse/doc/sphinx-guides/source/_static/installation/files/root/external-tools/twoRavens.json http://localhost:8080/api/admin/externalTools
# Port 8888 because we're running in Vagrant. On the dev1 server we use https://dev1.dataverse.org/dataexplore/gui.html
cd /root
DIR=/var/www/html/dataexplore
if [ ! -d $DIR ]; then
  cp -r $UNZIPPED $DIR
fi
cd $DIR
# The plan is to remove this hack of dropping preprocess.R into a deployed war file directory. See https://github.com/IQSS/dataverse/issues/3372
# FIXME: don't assume version 4.6.1
#diff /var/www/html/dataexplore/rook/preprocess/preprocess.R /usr/local/glassfish4/glassfish/domains/domain1/applications/dataverse-4.6.1/WEB-INF/classes/edu/harvard/iq/dataverse/rserve/scripts/preprocess.R
# FIXME: If `diff` shows a difference, which is likely, copy the version from TwoRavens to the Glassfish directory.
#cp /var/www/html/dataexplore/rook/preprocess/preprocess.R /usr/local/glassfish4/glassfish/domains/domain1/applications/dataverse-4.6.1/WEB-INF/classes/edu/harvard/iq/dataverse/rserve/scripts/preprocess.R
# FIXME: restart Glassfish if you had to update preprocess.R above.
# FIXME: Vagrant with it's weird 8888 port forwarding isn't working. On the dev1 server, TwoRavens works fine if you supply "https://dev1.dataverse.org" for both URLs.
echo "Next, run ./install.pl after you cd to $DIR"
