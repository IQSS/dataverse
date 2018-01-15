import io
import re
#import backup_swift #TODO
from backup_ssh import (backup_file_ssh)
from config import (ConfigSectionMap)

def backup_file (file_input, dataset_authority, dataset_identifier, storage_identifier, checksum_type, checksum_value, file_size):
    storage_type = ConfigSectionMap("Backup")['storagetype']

    if storage_type == 'swift':
        #backup_file_swift(file_input, dataset_authority, dataset_identifier, storage_identifier, checksum_type, checksum_value, file_size)
        raise NotImplementedError('no backup_swift yet')
    elif storage_type == 'ssh':
        backup_file_ssh(file_input, dataset_authority, dataset_identifier, storage_identifier, checksum_type, checksum_value, file_size)
    else:
        raise ValueError("only ssh/sftp and swift are supported as backup storage media")
    
