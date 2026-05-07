#!/usr/bin/env bash
# https://stackoverflow.com/questions/53619901/auto-create-s3-buckets-on-localstack
awslocal s3 mb s3://mybucket

# CORS for browser-direct uploads/downloads (upload-redirect=true /
# download-redirect=true). The browser PUTs/GETs presigned URLs from
# whatever origin the JSF page or SPA is served from; without these
# rules the OPTIONS preflight fails before the actual request fires.
#
# `x-amz-tagging` is required because Dataverse signs that header into
# direct-upload URLs by default. Drop the bucket's `x-amz-tagging`
# requirement (and the AllowedHeaders entry) only if your storage
# backend actually rejects tagging — that's the IBM Cloud Object
# Storage corner case the SDK PR #403 addresses.
awslocal s3api put-bucket-cors --bucket mybucket --cors-configuration '{
  "CORSRules": [
    {
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag", "x-amz-version-id"],
      "MaxAgeSeconds": 3000
    }
  ]
}'
