import io
import re
from config import (ConfigSectionMap)

def open_storage_object_filesystem(dataset_authority, dataset_identifier, object_location, is_tabular_data):
    filesystem_directory = ConfigSectionMap("Repository")['filesystemdirectory']
    if (is_tabular_data is not None):
        object_location += ".orig"
    file_path = filesystem_directory+"/"+dataset_authority+"/"+dataset_identifier+"/"+object_location
    byte_stream = io.open(file_path, "rb")
    return byte_stream
