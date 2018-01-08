import io
import re
import boto3

def open_storage_object_s3(dataset_authority, dataset_identifier, object_location, is_tabular_data):
    s3 = boto3.resource('s3')
    bucket_name,object_name = object_location.split(":",1)
    key = dataset_authority + "/" + dataset_identifier + "/" + object_name; 
    if (is_tabular_data is not None):
        key += ".orig"
    s3_obj = s3.Object(bucket_name=bucket_name, key=key)
    # "Body" is a byte stream associated with the object:
    return s3_obj.get()['Body']
