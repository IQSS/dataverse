-- replace any sequences of slashes and backslashes with a single slash:
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '[/\\][/\\]+', '/', 'g');
-- replace any ampersands with ' and ':
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '&', ' and ', 'g');
-- strip (and replace with a whitespace) any characters that are no longer allowed in the directory labels:
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '[^A-Za-z0-9_ ./-]+', ' ', 'g');
-- now replace any sequences of whitespaces with a single white space:
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '  +', ' ', 'g');
-- get rid of any leading or trailing slashes, spaces, '-'s and '.'s: 
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '^[/ .\-]+', '', '');
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '[/ \.\-]+$', '', '');
