Introduction
============

The new installer script written in Python is intended to replace the
old installer, written in Perl.  It has been implemented to work
exactly as the old one, supporting all the same options, etc.

Requirements
============

The script should work with both Python v. 2 and 3. It was tested with
v. 2.7.16 and v. 3.7.16.  Version 3.7 or newer is strongly
recommended, since the version 2 has been officially EOL-ed as of
Jan. 1 2020. If you have to use Python 2, we intend to maintain
backward compatibility, for now, but please use version 2.7 or newer.


The extra module psycopg2 (PostgreSQL client) is required. The
installer has been tested with psycopg2 version 2.8.4. 

We recommend that you try installing the "binary", pre-built version of the package:

pip install psycopg2-binary

(or "pip3 install psycopg2-binary" if you intend to use python3 and it's
installed separately on your system)

If for whatever reason the binary psycopg2 package doesn't run
properly on your system, you can try the more complex process of
making pip build it from sources:

pip3 install psycopg2

This will require compilers to be properly installed on your system.
(On MacOS, you'll need XCode installed). 
In order to link with the PostgresQL libraries, pip will need to
execute pg_config, PostgreSQL configuration utility. Make sure it is
in your PATH. If you have multiple versions of PostgresQL installed,
make sure the version that you will be using with Dataverse is the
first on your PATH. For example,

   PATH=/usr/pgsql-9.6/bin:$PATH; export PATH

Certain libraries and source include files, both for PostgresQL and
Python, are also needed to compile the module. On
RedHat/CentOS/etc. you may need to install the -devel packages, *for
the specific versions* of PostgreSQL and Python you will be using. For
example:

	yum install postgresql96-devel
	yum install python37-devel
	etc. 

On MacOS, all the needed libraries and source files appear to be
included by default with standard distributions of PostgreSQL and
Python.


Howto
=====

Run the new installer script as 

python install.py

or 

python3 install.py
(if python3 is installed separately from the default version 2)

If you run into any problems, or have any suggestions, please let us
know. Once again, this is still a beta/experimental version.

And if getting it to work on your system ends up being too much
trouble, there's always an option of going back to the old, default
installer.

