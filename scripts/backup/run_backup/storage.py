import io
import re
import boto3
from config import (ConfigSectionMap)
from storage_filesystem import (open_storage_object_filesystem)
from storage_s3 import (open_storage_object_s3)


def open_dataverse_file(dataset_authority, dataset_identifier, storage_identifier, is_tabular_data):
    m = re.search('^([a-z0-9]*)://(.*)$', storage_identifier) 
    if m is None:
        # no storage identifier tag. (defaulting to filesystem storage)
        storageTag = 'file'
        objectLocation = storage_identifier;
    else:
        storageTag = m.group(1)
        objectLocation = m.group(2)

    if storageTag == 'file':
        byteStream = open_storage_object_filesystem(dataset_authority, dataset_identifier, objectLocation, is_tabular_data)
        return byteStream
    elif storageTag == 's3':
        byteStream = open_storage_object_s3(dataset_authority, dataset_identifier, objectLocation, is_tabular_data)
        return byteStream
    elif storageTag == 'swift':
        raise ValueError("backup of swift objects not supported yet")

    raise ValueError("Unknown or unsupported storage method: "+storage_identifier)
