-- replace any sequences of slashes and backslashes with a single slash:
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '[/\\][/\\]+', '/', 'g');
-- strip (and replace with a .) any characters that are no longer allowed in the directory labels:
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '\.\.+', '.', 'g');
-- now replace any sequences of .s with a single .:
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '\.\.+', '.', 'g');
-- get rid of any leading or trailing slashes, spaces, '-'s and '.'s: 
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '^[/ .\-]+', '', '');
UPDATE filemetadata SET directoryLabel = regexp_replace(directoryLabel, '[/ \.\-]+$', '', '');
