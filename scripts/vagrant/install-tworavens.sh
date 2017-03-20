#!/bin/bash
echo "This script is highly experimental and makes many assumptions about how Dataverse is running in Vagrant. Please consult the TwoRavens section of the Dataverse Installation Guide instead."
exit 1
cd /root
yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-6.noarch.rpm
yum install -y R R-devel
yum install -y /dataverse/doc/sphinx-guides/source/_static/installation/files/home/rpmbuild/rpmbuild/RPMS/x86_64/rapache-1.2.6-rpm0.x86_64.rpm
yum install -y gcc-gfortran # to build R packages
COMMIT=9b4430af6fb4dee4f7061c72b37c8e48ebff0ad2
if [ ! -f $COMMIT ]; then
  wget https://github.com/IQSS/TwoRavens/archive/9b4430af6fb4dee4f7061c72b37c8e48ebff0ad2.zip
  unzip 9b4430af6fb4dee4f7061c72b37c8e48ebff0ad2
  cd TwoRavens-9b4430af6fb4dee4f7061c72b37c8e48ebff0ad2/r-setup
  # FIXME: This commit is no good because there's an "exit 0" in r-setup.sh. For now, edit the script by hand and remove it.
  #./r-setup.sh # This is expected to take a while. Look for lines like "Package Zelig successfully installed" and "Successfully installed Dataverse R framework".
fi
curl -X PUT -d true http://localhost:8080/api/admin/settings/:TwoRavensTabularView
# Port 8888 because we're running in Vagrant. This gets a little weird. Had to mess with the app_ddi.js file...
curl -X PUT -d http://localhost:8888/dataexplore/gui.html http://localhost:8080/api/admin/settings/:TwoRavensUrl
cd /root
DIR=/var/www/html/dataexplore
if [ ! -d $DIR ]; then
  cp -r  TwoRavens-9b4430af6fb4dee4f7061c72b37c8e48ebff0ad2 $DIR
fi
cd $DIR
echo "Next, run ./install.pl after you cd to $DIR"
