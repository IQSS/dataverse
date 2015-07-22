#!/bin/sh

echo
echo "Installing additional R packages."
echo
echo "PLEASE NOTE: this may take a while!"
echo ""
echo "Also: "
echo "Compiling these modules will generate a VERY large amount of output."
echo "All these messages that you'll see on screen will also be saved"
echo "in several .LOG files in this directory".
echo "If anything goes wrong during this installation, please send these"
echo "files to the Dataverse support team"
echo ""

sleep 10

# Use an alternative CRAN repository mirror, if r-project.org
# is not available or slow to access from where you are.
CRANREPO="http://cran.r-project.org"; export CRANREPO
# Set this to your local R Library directory, if different:
RLIBDIR=/usr/lib64/R/library; export RLIBDIR



for RPACK in R2HTML Rserve VGAM AER dplyr quantreg geepack maxLik Amelia Rook jsonlite rjson devtools DescTools
  do
  LOG="RINSTALL.$RPACK.LOG"
  echo
  echo "Installing package ${RPACK} (from CRAN):"
  echo 'install.packages("'${RPACK}'", INSTALL_opts=c("--no-test-load"), repos="'${CRANREPO}'",dependencies=T)' | (unset DISPLAY; R --vanilla --slave ) 2>&1 | tee ${LOG}
  echo
  echo
  echo
  echo "FINISHED INSTALLING" ${RPACK}
  echo 
done

echo
echo "installing package Zelig (from local GitHub):"

wget -O /tmp/master.zip 'https://github.com/IQSS/Zelig/archive/master.zip'
(cd /tmp; unzip master.zip)

LOG="RINSTALL.Zelig.LOG"

echo 'setwd("/tmp"); library(devtools); install("Zelig-master")' | (unset DISPLAY; R --vanilla --slave ) 2>&1 | tee ${LOG}

echo "FINISHED INSTALLING Zelig"

echo 
echo -n "Checking if R Library directory exists..."
if [ "x"$RLIBDIR != "x" ] && [ -d $RLIBDIR ]
then
    echo "ok"
else
    echo "Could not find library directory!"
    if [ "x"$RLIBDIR != "x" ]
	then
	echo "directory $RLIBDIR does not exist."
    else
	echo "R is not installed (?)"
    fi
    exit 1
fi

echo 
echo "checking Rserve configuration:" 

/usr/sbin/groupadd -g 97 -o -r rserve >/dev/null 2>/dev/null || :
/usr/sbin/useradd -g rserve -o -r -d $RLIBDIR -s /bin/bash \
        -c "Rserve User" -u 97 rserve 2>/dev/null || :

echo

if [ ! -f /etc/Rserv.conf ]
then
    echo "installing Rserv configuration file."
    install -o rserve -g rserve Rserv.conf /etc/Rserv.conf
    echo 
fi

if [ ! -f /etc/Rserv.pwd ]
then
    echo "Installing Rserve password file."
    echo "Please change the default password in /etc/Rserv.pwd"
    echo "(and make sure this password is set correctly as a"
    echo "JVM option in the glassfish configuration of your DVN)"
    install -m 0600 -o rserve -g rserve Rserv.pwd /etc/Rserv.pwd
    echo
fi

if [ ! -f /etc/init.d/rserve ]
then
    echo "Installing Rserve startup file."
    install rserve-startup.sh /etc/init.d/rserve
    chkconfig rserve on
    echo "You can start Rserve daemon by executing"
    echo "  service rserve start"
fi

echo 
echo "Successfully installed Dataverse R framework."
echo


exit 0
