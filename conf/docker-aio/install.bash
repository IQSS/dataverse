#!/usr/bin/env bash
sudo -u postgres createuser --superuser dvnapp
#./entrypoint.bash &
unzip dvinstall.zip
cd dvinstall/
echo "beginning installer"
./install -admin_email=dvAdmin@mailinator.com -y -f > install.out 2> install.err

echo "installer complete"
cat install.err
