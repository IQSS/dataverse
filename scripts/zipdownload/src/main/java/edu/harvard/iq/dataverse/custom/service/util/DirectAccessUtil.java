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

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
public class DirectAccessUtil implements java.io.Serializable  {

    private AmazonS3 s3 = null;
    
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

            //System.out.println("bucket: "+bucket);
            //System.out.println("key: "+key);
            
            /* commented-out code below is for looking up S3 metatadata
               properties, such as size, etc. prior to making an access call:
            ObjectMetadata objectMetadata = null;
            long fileSize = 0L;
            try {
                objectMetadata = s3.getObjectMetadata(bucket, key);
                fileSize = objectMetadata.getContentLength();
                //System.out.println("byte size: "+objectMetadata.getContentLength());
            } catch (SdkClientException sce) {
                System.err.println("Cannot get S3 object metadata " + key + " from bucket " + bucket);
            }*/

            try {
                inputStream = s3.getObject(new GetObjectRequest(bucket, key)).getObjectContent();
            } catch (SdkClientException sce) {
                System.err.println("Cannot get S3 object " + key + " from bucket " + bucket);
            }
            
        } else if (storageLocation.startsWith("file://")) {
            // This could be a static method; since no reusable client/maintainable
            // state is required
            
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
                AmazonS3ClientBuilder s3CB = AmazonS3ClientBuilder.standard();
                s3CB.setCredentials(new ProfileCredentialsProvider("default"));
                this.s3 = s3CB.build();

            } catch (Exception e) {
                System.err.println("cannot instantiate an S3 client");
            }
        }
    }

}