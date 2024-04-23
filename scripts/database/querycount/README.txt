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

Then run the count script as follows.

1. The default, interactive mode:

cd /var/lib/pgsql/9.3/data/pg_log/
./count.pl <NAME OF THE LOG FILE>

you will see something like this:

# ./count.pl postgresql-Mon.log 
Current size: 3090929 bytes.
Execute the application task that you are testing/measuring (an API call, a page load, etc.), then press any key. The script will then attempt to parse the increment portion of the PostgresQL log.

Now go to your Dataverse and do whatever it is that you are
testing. Then press any key to tell the script that it's done. It will
then save the tail of the log file generated since you started the
script, parse it, count the queries and output the total and the
queries by type sorted by frequency:

Parsed and counted the queries. Total number:
22593

Queries, counted and sorted: 

   6248 SELECT ID, ASSIGNEEIDENTIFIER, PRIVATEURLANONYMIZEDACCESS, PRIVATEURLTOKEN, DEFINITIONPOINT_ID, ROLE_ID FROM ROLEASSIGNMENT
   6158 SELECT t1.ID, t1.DESCRIPTION, t1.DISPLAYNAME, t1.GROUPALIAS, t1.GROUPALIASINOWNER, t1.OWNER_ID FROM EXPLICITGROUP t0, explicitgroup_explicitgroup t2, EXPLICITGROUP t1
   4934 SELECT t0.ID, t0.DESCRIPTION, t0.DISPLAYNAME, t0.GROUPALIAS, t0.GROUPALIASINOWNER, t0.OWNER_ID FROM EXPLICITGROUP t0, ExplicitGroup_CONTAINEDROLEASSIGNEES t1
   2462 SELECT t1.ID, t1.DESCRIPTION, t1.DISPLAYNAME, t1.GROUPALIAS, t1.GROUPALIASINOWNER, t1.OWNER_ID FROM AUTHENTICATEDUSER t0, EXPLICITGROUP_AUTHENTICATEDUSER t2, EXPLICITGROUP t1
    647 SELECT ID, BACKGROUNDCOLOR, LINKCOLOR, LINKURL, LOGO, LOGOALIGNMENT, LOGOBACKGROUNDCOLOR, LOGOFORMAT, TAGLINE, TEXTCOLOR, dataverse_id FROM DATAVERSETHEME

   ... etc. 

The output above is also saved in the file "tail.counted" in the
pg_log directory. This of course only shows the queries counted by
type. I.e. the above shows that there were 6248 "SELECT ... FROM
ROLEASSIGNMENT" queries, but not which specific objects the
application was looking up. I.e., the queries are truncated at the
"WHERE" (or "SET" or "VALUES") part in order to give a compact, sorted
summary.

If you want to look at these details, they are saved in the file
called "tail.parsed". There you will find fully extended queries, for
example:

SELECT ID, ASSIGNEEIDENTIFIER, PRIVATEURLANONYMIZEDACCESS, PRIVATEURLTOKEN, DEFINITIONPOINT_ID, ROLE_ID FROM ROLEASSIGNMENT WHERE ((ASSIGNEEIDENTIFIER IN (':authenticated-users')) AND (DEFINITIONPOINT_ID IN ('1')))

This file is unsorted, the queries appear in the same order in which
they were executed.

Finally, there is the raw, unprocessed/unparsed saved portion of the
log file, in the file simply called "tail". It contains some extra
information (for example, the actual time when each query was
executed), but is much harder to read. For example, the entries for
the query above will appear as follows:

2021-11-01 22:41:38.198 UTC [5043] LOG:  execute S_25: SELECT ID, ASSIGNEEIDENTIFIER, PRIVATEURLANONYMIZEDACCESS, PRIVATEURLTOKEN, DEFINITIONPOINT_ID, ROLE_ID FROM ROLEASSIGNMENT WHERE ((ASSIGNEEIDENTIFIER IN ($1)) AND (DEFINITIONPOINT_ID IN ($2)))
2021-11-01 22:41:38.198 UTC [5043] DETAIL:  parameters: $1 = ':authenticated-users', $2 = '1'

If you want to keep any of the files above, save them somewhere before
running the script again, otherwise they will be overwritten.

2. Non-interactive mode:

cd /var/lib/pgsql/9.3/data/pg_log/
./count.pl --non-interactive <NAME OF THE LOG FILE>

Same as above, but will not wait for the keystroke from the user, and will attempt to parse the entire log file. This can be useful when executed from other scripts. The output will be saved in the files tail.counted and tail.parsed, as above. But there will be no file named "tail", since the entire log file is the increment being measured.





 
