package edu.harvard.iq.dataverse.dataaccess;

import com.amazonaws.AmazonClientException;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.lang3.StringUtils;

public class S3ClientFactory {

    /**
     * Pass in a URL pointing to your S3 compatible storage.
     * For possible values see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/client/builder/AwsClientBuilder.EndpointConfiguration.html
     */
    private String s3CEUrl = System.getProperty("dataverse.files.s3-custom-endpoint-url", "");
    
    /**
     * Pass in a region to use for SigV4 signing of requests.
     * Defaults to "dataverse" as it is not relevant for custom S3 implementations.
     */
    private String s3CERegion = System.getProperty("dataverse.files.s3-custom-endpoint-region", "dataverse");
    
    /**
     * Pass in a boolean value if path style access should be used within the S3 client.
     * Anything but case-insensitive "true" will lead to value of false, which is default value, too.
     */
    private boolean s3pathStyleAccess = Boolean.parseBoolean(System.getProperty("dataverse.files.s3-path-style-access", "false"));

    private String s3DefaultBucketName = System.getProperty("dataverse.files.s3-bucket-name");

    
    private AmazonS3 s3Client = null;
    
    // -------------------- LOGIC --------------------
    
    /**
     * Returns {@link AmazonS3} configured using system properties:
     * <Ul>
     *   <li>dataverse.files.s3-custom-endpoint-url</li>
     *   <li>dataverse.files.s3-custom-endpoint-region</li>
     *   <li>dataverse.files.s3-path-style-access</li>
     * </Ul>
     * Method reuses built s3 client if it was created before.
     * <br/>
     * Note that method will try to build new client if basic operation
     * ({@link AmazonS3#doesBucketExist(String)}) on client will be a success.
     * This helps in situations when s3 server is not up when obtaining
     * s3 client (s3 server can be made available online after first
     * invocation of method)
     */
    public AmazonS3 getClient() {
        if (s3Client != null) {
            return s3Client;
        }
        AmazonS3 s3Client = buildClient();
        if (StringUtils.isEmpty(s3DefaultBucketName) || !s3Client.doesBucketExist(s3DefaultBucketName)) {
            throw new AmazonClientException("ERROR: You must create and configure a bucket before creating datasets.");
        }
        s3Client.listBuckets();
        this.s3Client = s3Client;

        return s3Client;
    }

    public String getDefaultBucketName() {
        return s3DefaultBucketName;
    }

    // -------------------- PRIVATE --------------------
    
    private AmazonS3 buildClient() {
        try {
            // get a standard client, using the standard way of configuration the credentials, etc.
            AmazonS3ClientBuilder s3CB = AmazonS3ClientBuilder.standard();
            // if the admin has set a system property (see below) we use this endpoint URL instead of the standard ones.
            if (!s3CEUrl.isEmpty()) {
                s3CB.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3CEUrl, s3CERegion));
            }
            // some custom S3 implementations require "PathStyleAccess" as they us a path, not a subdomain. default = false
            s3CB.withPathStyleAccessEnabled(s3pathStyleAccess);
            // let's build the client :-)
            return s3CB.build();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot instantiate a S3 client; check your AWS credentials and region",
                    e);
        }
    }
}
