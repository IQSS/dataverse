#!/usr/bin/env python

import ConfigParser
import psycopg2
import sys
from database import (query_database, get_last_timestamp, record_datafile_status)
from storage import (open_dataverse_file)
from backup import (backup_file)
import io

def main():

    time_stamp = get_last_timestamp()
    if time_stamp is None:
        print "No time stamp! first run."
        records = query_database()
    else:
        print "last backup: "+time_stamp
        records = query_database(time_stamp)

    for result in records:
        dataset_authority = result[0]
        dataset_identifier = result[1]
        storage_identifier = result[2]
        checksum_type = result[3]
        checksum_value = result[4]
        file_size = result[5]
        create_time = result[6]

        if (checksum_value is None):
            checksum_value = "MISSING"


        if (storage_identifier is not None and dataset_identifier is not None and dataset_authority is not None):
            print dataset_authority + "/" + dataset_identifier + "/" + storage_identifier + ", " + checksum_type + ": " + checksum_value

            file_input=None

            try: 
                file_input = open_dataverse_file(dataset_authority, dataset_identifier, storage_identifier)
            except:
                print "failed to open file "+storage_identifier
                file_input=None

            
            if (file_input is not None):
                try:
                    backup_file(file_input, dataset_authority, dataset_identifier, storage_identifier, checksum_type, checksum_value, file_size)
                    print "backed up file "+storage_identifier
                    record_datafile_status(dataset_authority, dataset_identifier, storage_identifier, 'OK', create_time)
                except ValueError:
                    print "failed to back up file "+storage_identifier

if __name__ == "__main__":
    main()



