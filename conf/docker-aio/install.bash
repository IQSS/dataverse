#!/usr/bin/env bash
sudo -u postgres createuser --superuser dvnapp
#./entrypoint.bash &
unzip dvinstall.zip
cd dvinstall/
./install -admin_email=pameyer+dvinstall@crystal.harvard.edu -y -f > install.out 2> install.err

echo "installer complete"
cat install.err
