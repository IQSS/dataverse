#! /bin/sh
# chkconfig: 2345 99 01
# description: Rserve, /etc/init.d/rserve

# Source function library.
. /etc/rc.d/init.d/functions


case "$1" in
  start)
        echo -n "Starting Rserve daemon: "
        R CMD Rserve >/dev/null 2>&1
        echo "."
        ;;
  stop)
        echo -n "Stopping Rserve daemon: "
        killall -s 9 Rserve
        echo "."
        ;;
  restart)
        echo -n "Stopping Rserve daemon: "
        killall -s 9 Rserve
        echo "."
        echo -n "Starting Rserve daemon: "
        daemon R CMD Rserve
        echo "."
        ;;
  *)
        echo "Usage: /etc/init.d/rserve {start|stop|restart}"
        exit 1
esac

exit 0
