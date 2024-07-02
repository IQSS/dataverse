If your S3 store does not support tagging and gives an error if you configure direct uploads, you can disable the tagging by using the ``dataverse.files.<id>.disable-tagging`` JVM option. For more details see https://dataverse-guide--10029.org.readthedocs.build/en/10029/developers/big-data-support.html#s3-tags #10022 and #10029.

## New config options

- dataverse.files.<id>.disable-tagging
