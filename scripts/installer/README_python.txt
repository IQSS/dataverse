Introduction
============

The new installer script written in Python is intended to replace the
old installer, written in Perl.  It has been implemented to work
exactly as the old one, supporting all the same options, etc.

Requirements
============

The script should work with both Python v. 2 and 3. It was tested with
v. 2.7.16 and v. 3.7.16.  Version 3 is strongly recommended, since the
version 2 has been officially EOL-ed as of Jan. 1 2020. But we intend
to maintain backward compatibility, at least for now.


The extra module psycopg2 (PostgreSQL client) is required. The
installer has been tested with psycopg2 version 2.8.4.

Install the module with 

pip install psycopg2 

(or "pip3 install psycopg2" if you intend to use python3 and it's installed separately on your system)

Howto
=====

Run the new installer script as 

python install.py

or 

python3 install.py
(if python3 is installed separately from the default version 2)



