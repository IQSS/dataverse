#!/usr/bin/env python

import ConfigParser
import psycopg2
import sys
import io
import re
from database import (query_database, get_last_timestamp, record_datafile_status, get_datafile_status)
from storage import (open_dataverse_file)
from backup import (backup_file)
from email_notification import (send_notification)

def main():
    rrmode = False

    if (len(sys.argv) > 1 and sys.argv[1] == '--rerun'):
        rrmode = True

    if rrmode:
        time_stamp = None
    else:
        time_stamp = get_last_timestamp()

    if time_stamp is None:
        print "No time stamp! first run (or a full re-run)."
        records = query_database()
    else:
        print "last backup: "+time_stamp
        records = query_database(time_stamp)

    files_total=0
    files_success=0
    files_failed=0
    files_skipped=0

    for result in records:
        dataset_authority = result[0]
        dataset_identifier = result[1]
        storage_identifier = result[2]
        checksum_type = result[3]
        checksum_value = result[4]
        file_size = result[5]
        create_time = result[6]
        is_tabular_data = result[7]

        if (checksum_value is None):
            checksum_value = "MISSING"


        if (storage_identifier is not None and dataset_identifier is not None and dataset_authority is not None):
            files_total += 1
            print dataset_authority + "/" + dataset_identifier + "/" + storage_identifier + ", " + checksum_type + ": " + checksum_value

            file_input=None

            # if this is a re-run, we are only re-trying the files that have failed previously:
            if (rrmode and get_datafile_status(dataset_authority, dataset_identifier, storage_identifier) == 'OK'): 
                files_skipped += 1
                continue

            try: 
                file_input = open_dataverse_file(dataset_authority, dataset_identifier, storage_identifier, is_tabular_data)
            except:
                print "failed to open file "+storage_identifier
                file_input=None

            
            if (file_input is not None):
                try:
                    backup_file(file_input, dataset_authority, dataset_identifier, storage_identifier, checksum_type, checksum_value, file_size)
                    print "backed up file "+storage_identifier
                    record_datafile_status(dataset_authority, dataset_identifier, storage_identifier, 'OK', create_time)
                    files_success += 1
                except ValueError, ve:
                    exception_message = str(ve)
                    print "failed to back up file "+storage_identifier+": "+exception_message
                    if (re.match("^remote", exception_message) is not None):
                        record_datafile_status(dataset_authority, dataset_identifier, storage_identifier, 'FAIL_VERIFY', create_time)
                    else:
                        record_datafile_status(dataset_authority, dataset_identifier, storage_identifier, 'FAIL_WRITE', create_time)
                    files_failed += 1
                    #TODO: add a separate failure status 'FAIL_VERIFY' - for when it looked like we were able to copy the file 
                    # onto the remote storage system, but the checksum verification failed (?)
            else:
                record_datafile_status(dataset_authority, dataset_identifier, storage_identifier, 'FAIL_READ', create_time)
                files_failed += 1

    if (files_skipped > 0):
        report = ('backup script run report: %d files processed; %d skipped (already backed up), %d success, %d failed' % (files_total, files_skipped, files_success, files_failed))
    else:
        report = ('backup script run report: %d files processed; %d success, %d failed' % (files_total, files_success, files_failed))
    print report
    send_notification(report)

if __name__ == "__main__":
    main()



