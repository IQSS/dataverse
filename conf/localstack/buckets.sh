#!/usr/bin/env bash
# https://stackoverflow.com/questions/53619901/auto-create-s3-buckets-on-localstack
awslocal s3 mb s3://mybucket
awslocal s3 mb s3://mybucket-noredirect
# Allow direct uploads/downloads from browsers (e.g. the SPA) via presigned URLs
awslocal s3api put-bucket-cors \
  --bucket mybucket \
  --cors-configuration '{
    "CORSRules": [
      {
        "AllowedHeaders": ["*"],
        "AllowedMethods": ["GET", "HEAD", "PUT", "POST", "DELETE"],
        "AllowedOrigins": ["*"],
        "ExposeHeaders": ["ETag"]
      }
    ]
  }'
