The following needs to be added to the release notes and/or upgrade instructions: 

In making this fix we discovered that notifications created prior to 2018 may have been invalidated.  With this release we advise that these older notifications are deleted from the database. The following query can be used for this purpose:

delete from usernotification where date_part('year', senddate) < 2018;

