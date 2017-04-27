For non-interactive use, the installer can be run with the `./installer -y -f`.
Default configuration values will be read from the `default.config` file (containing key value pairs separated by a tab), if it exists (if not, values hard-coded in the installer script are used).

`./install -admin_email=foo+dvadmin@example.org -y -f > i0.out 2> i0.err`

