This script counts queries *on the PostgresQL server side*. 

To use it, enable verbose logging on the postgres server: 

Edit your postgresql.conf (for example,
/var/lib/pgsql/9.3/data/postgresql.conf) and set "log_statement" to
"all", like this:

log_statement = 'all'	       		# none, ddl, mod, all

Then restart postgresql. 

Now you should have a fast-growing log file in your pg_log directory.
For example, /var/lib/pgsql/9.3/data/pg_log/postgresql-Tue.log. (The
name of the log file may vary on your system!)

Copy the 2 scripts, count.pl and parse.pl to the log directory. 

For example: 

cp scripts/database/querycount/*.pl  /var/lib/pgsql/9.3/data/pg_log/

Then run the count script as follows: 

cd /var/lib/pgsql/9.3/data/pg_log/
./count.pl <NAME OF THE LOG FILE>

you will see something like this:

# ./count.pl postgresql-Mon.log 
Current size: 3090929 bytes.
Press any key when ready.

Now go to your Dataverse and do whatever it is that you are
testing. Then press any key to tell the script that it's done. It will
then save the tail of the log file generated since you started the
script, parse it, count the queries and output the total and the
queries by type sorted by frequency:

Parsed and counted the queries. Total number:
22593

Queries, counted and sorted: 

   6248 SELECT ID, ASSIGNEEIDENTIFIER, PRIVATEURLTOKEN, DEFINITIONPOINT_ID, ROLE_ID FROM ROLEASSIGNMENT
   6158 SELECT t1.ID, t1.DESCRIPTION, t1.DISPLAYNAME, t1.GROUPALIAS, t1.GROUPALIASINOWNER, t1.OWNER_ID FROM EXPLICITGROUP t0, explicitgroup_explicitgroup t2, EXPLICITGROUP t1
   4934 SELECT t0.ID, t0.DESCRIPTION, t0.DISPLAYNAME, t0.GROUPALIAS, t0.GROUPALIASINOWNER, t0.OWNER_ID FROM EXPLICITGROUP t0, ExplicitGroup_CONTAINEDROLEASSIGNEES t1
   2462 SELECT t1.ID, t1.DESCRIPTION, t1.DISPLAYNAME, t1.GROUPALIAS, t1.GROUPALIASINOWNER, t1.OWNER_ID FROM AUTHENTICATEDUSER t0, EXPLICITGROUP_AUTHENTICATEDUSER t2, EXPLICITGROUP t1
    647 SELECT ID, BACKGROUNDCOLOR, LINKCOLOR, LINKURL, LOGO, LOGOALIGNMENT, LOGOBACKGROUNDCOLOR, LOGOFORMAT, TAGLINE, TEXTCOLOR, dataverse_id FROM DATAVERSETHEME

   ... etc. 

(the output is also saved in the file "tail.counted" in the pg_log directory)


 
