import io
import re
import swiftclient
from config import (ConfigSectionMap)

def backup_file_swift (file_input, dataset_authority, dataset_identifier, storage_identifier):
    auth_url = ConfigSectionMap("Backup")['swiftauthurl']
    auth_version = ConfigSectionMap("Backup")['swiftauthversion']
    user = ConfigSectionMap("Backup")['swiftuser']
    tenant = ConfigSectionMap("Backup")['swifttenant']
    key = ConfigSectionMap("Backup")['swiftkey']

    conn = swiftclient.Connection(
        authurl=auth_url,
        user=user,
        key=key,
        tenant_name=tenant,
        auth_version=auth_version
    )

    container_name = dataset_authority + ":" + dataset_identifier
    conn.put(container_name)
    
    conn.put_object(container_name, storage_identifier, file_input)
    
