/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.custom.service.util;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for directly accessing storage locations
 * Supports file system and S3. 
 * (S3 has only been tested with AWS; non-standard auth may not be supported yet)
 * 
 * @author Leonid Andreev
 */
public class DirectAccessUtil implements java.io.Serializable {

    private S3Client s3 = null;
    
    public InputStream openDirectAccess(String storageLocation) {
        InputStream inputStream = null;

        if (storageLocation.startsWith("s3://")) {
            createOrReuseAwsClient();
            
            if (this.s3 == null) {
                return null; 
            }
            
            storageLocation = storageLocation.substring(5);

            String bucket = storageLocation.substring(0, storageLocation.indexOf('/'));
            String key = storageLocation.substring(storageLocation.indexOf('/') + 1);

            try {
                ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
                inputStream = s3Object;
            } catch (S3Exception se) {
                System.err.println("Cannot get S3 object " + key + " from bucket " + bucket);
            }
            
        } else if (storageLocation.startsWith("file://")) {
            storageLocation = storageLocation.substring(7);
            
            try {
                inputStream = new FileInputStream(new File(storageLocation));
            } catch (IOException ioex) {
                System.err.println("Cannot open file " + storageLocation);
            }
        }
        
        // Unsupported storage location - return null
        return inputStream;
    }
    
    private void createOrReuseAwsClient() {
        if (this.s3 == null) {
            try {
                this.s3 = S3Client.builder()
                        .region(Region.US_EAST_1) // You may want to make this configurable
                        .credentialsProvider(ProfileCredentialsProvider.create("default"))
                        .build();
            } catch (Exception e) {
                System.err.println("Cannot instantiate an S3 client: " + e.getMessage());
            }
        }
    }
}