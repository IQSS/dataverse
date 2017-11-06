#!/usr/bin/env python

import ConfigParser
import psycopg2
import sys
from database import (query_database)
from storage import (open_dataverse_file)
from backup import (backup_file)
import io

def main():

    records = query_database()

    for result in records:
        dataset_authority = result[0]
        dataset_identifier = result[1]
        storage_identifier = result[2]

        file_inpuit = open_dataverse_file(dataset_authority, dataset_identifier, storage_identifier)
        backup_file(file_input, dataset_authority, dataset_identifier, storage_identifier)

if __name__ == "__main__":
    main()



