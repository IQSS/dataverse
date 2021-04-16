#!/bin/sh

r_rq=`/etc/init.d/rq status`
if [ "rq_worker running" != "$r_rq" ]; then
	echo "rq failed"
	exit 1
fi
r_www=`/etc/init.d/lighttpd status`
e_www=$?
if [ 0 -ne $e_www ]; then
	echo "lighttpd failed"
	exit 2
fi

