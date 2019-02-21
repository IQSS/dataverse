#!/bin/sh

echo
echo "Configuring Rserve."
echo

sleep 10


echo 
echo "checking if rserve user already exists:" 

RSERVEDIR=/tmp/Rserv

/usr/sbin/groupadd -g 97 -o -r rserve >/dev/null 2>/dev/null || :
/usr/sbin/useradd -g rserve -o -r -d $RSERVEDIR -s /bin/bash \
        -c "Rserve User" -u 97 rserve 2>/dev/null || :

echo

if [ ! -f /etc/Rserv.conf ]
then
    echo "installing Rserv configuration file."
    install -o rserve -g rserve Rserv.conf /etc/Rserv.conf
    echo 
else
    echo "Rserve configuration file (/etc/Rserv.conf) already exists."
fi

if [ ! -f /etc/Rserv.pwd ]
then
    echo "Installing Rserve password file."
    echo "Please change the default password in /etc/Rserv.pwd"
    echo "(and make sure this password is set correctly as a"
    echo "JVM option in the glassfish configuration of your DVN)"
    install -m 0600 -o rserve -g rserve Rserv.pwd /etc/Rserv.pwd
    echo
else 
        echo "Rserve password file (/etc/Rserv.pwd) already exists."
fi

if [ ! -f /etc/init.d/rserve ]
then
    echo "Installing Rserve startup file."
    install rserve-startup.sh /etc/init.d/rserve
    chkconfig rserve on
    echo "You can start Rserve daemon by executing"
    echo "  service rserve start"
    echo 
    echo "If this is a RedHat/CentOS 7 system, you may want to use the systemctl file rserve.service instead (provided in this directory)"
else 
    echo "Rserve startup file already in place."
fi

echo 
echo "Successfully installed Dataverse Rserve framework."
echo


service rserve start

exit 0
