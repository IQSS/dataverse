-- let's try again and fix the existing directoryLabels:
-- (the script shipped with 4.12 was missing the most important line; bad copy-and-paste)
-- replace any sequences of slashes and backslashes with a single slash:
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '[/\\][/\\]+', '/', 'g');
-- strip (and replace with a .) any characters that are no longer allowed in the directory labels:
-- (this line was missing from the script released with 4.12!!)
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '[^A-Za-z0-9_ ./-]+', '.', 'g');
-- now replace any sequences of .s with a single .:
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '\.\.+', '.', 'g');
-- get rid of any leading or trailing slashes, spaces, '-'s and '.'s: 
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '^[/ .\-]+', '', '');
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '[/ \.\-]+$', '', '');
