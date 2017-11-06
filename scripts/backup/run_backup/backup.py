import io
import re
import backup_swift
from config import (ConfigSectionMap)

def backup_file (file_input, dataset_authority, dataset_identifier, storage_identifier):
    storage_type = ConfigSectionMap("Backup")['storage_type']

    if storage_type == 'swift':
        backup_file_swift(file_input, dataset_authority, dataset_identifier, storage_identifier)
    else:
        raise ValueError("only swift is supported as backup storage medium")
    
