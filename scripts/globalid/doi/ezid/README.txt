The script switch_to_public.sh was used in production Dataverse at
IQSS Harvard to change the status of a number of EZID DOIs identifiers
from "reserved" to "public". If your installation is using EZID, you
MAY OR MAY NOT have to do this on some of your reserved identifiers as
you migrate to DataCite.

The script talks DIRECTLY to EZID (i.e., it bypasses Dataverse completely). 

We found that the fastest (and simplest too!) way to make this change
was by relying on the python command line script provided by EZID
(https://ezid.cdlib.org/doc/ezid.py). Download it and place it in the
current directory, and make sure it is executable.

Authenticating on every request costs extra (about .4 sec. per
request), so we log in once and obtain the session id, as follows:

./ezid.py {username}:{password} login 

with your EZID credentials; this will output the session id that you 
pass to the registration script, below. 

Run the registration script as follows: 

./switch_to_public.sh {DOILISTFILE} {SESSIONID}

where {DOILISTFILE} is a file containing the EZID dois that need to be
changed; in the standard format (for example,
doi:10.7910/DVN/HGJ0TQ/GQ7DNI). And {SESSIONID} is the login session
obtained in the step above. 

The script sleeps for 1 second between calls (out of being
polite). Making the overall cost of this registration change 1.4
sec. per DOI. (your mileage may vary of course!)

IMPORTANT: The script is provided FOR REFERENCE ONLY! It may or may
not work on your system/satisfy your requirements. Use it as an
example, and keep in mind that you may need to make adjustments in 
order to work according to your local setup and needs. 
