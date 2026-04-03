/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.pidproviders.doi.datacite;


import java.io.Closeable;
import java.io.IOException;
import java.util.Base64;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonObject;

/**
 * DataCiteRESTfullClient
 *
 * @author luopc
 *
 */
public class DataCiteRESTfullClient implements Closeable {
    
    private static final Logger logger = Logger.getLogger(DataCiteRESTfullClient.class.getCanonicalName());

    // Constants for retry mechanism
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 10000; // 10 seconds
    
    private String url;
    private String restApiUrl;
    private CloseableHttpClient httpClient;
    private HttpClientContext context;
    private String encoding = "utf-8";

    public DataCiteRESTfullClient(String url, String restApiUrl, String username, String password) {
        this.url = url;
        this.restApiUrl = restApiUrl;
        context = HttpClientContext.create();
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(username, password));
        context.setCredentialsProvider(credsProvider);

        httpClient = HttpClients.createDefault();
    }

    public void close() {
        if (this.httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException io) {
                logger.warning("IOException closing httpClient: " + io.getMessage());
            }
        }
    }
    
    /**
     * Execute HTTP request with retry mechanism for specific status codes
     * 
     * @param request The HTTP request to execute
     * @param operationName Name of the operation for logging
     * @return HttpResponse The response from the server
     * @throws IOException If an error occurs during the request
     */
    private HttpResponse executeWithRetry(org.apache.http.client.methods.HttpRequestBase request, String operationName) throws IOException {
        int attempts = 0;
        IOException lastException = null;
        
        while (attempts < MAX_RETRIES) {
            try {
                HttpResponse response = httpClient.execute(request, context);
                int statusCode = response.getStatusLine().getStatusCode();
                
                // If we get a retry status code, try again after delay
                if (statusCode == 429 || statusCode == 503 || statusCode == 504) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    attempts++;
                    
                    if (attempts < MAX_RETRIES) {
                        logger.warning("DataCite API returned status " + statusCode + 
                                       " for " + operationName + ". Retrying in " + 
                                       (RETRY_DELAY_MS / 1000) + " seconds (attempt " + attempts + " of " + MAX_RETRIES + ")");
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Retry interrupted", ie);
                        }
                    } else {
                        logger.severe("DataCite API failed with status " + statusCode + 
                                      " for " + operationName + " after " + MAX_RETRIES + " attempts");
                        return response; // Return the last failed response
                    }
                } else {
                    // Success or non-retry error code
                    return response;
                }
            } catch (IOException ioe) {
                lastException = ioe;
                attempts++;
                
                if (attempts < MAX_RETRIES) {
                    logger.warning("IOException during " + operationName + ": " + ioe.getMessage() + 
                                   ". Retrying in " + (RETRY_DELAY_MS / 1000) + " seconds (attempt " + 
                                   attempts + " of " + MAX_RETRIES + ")");
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }
                } else {
                    logger.severe("DataCite API failed for " + operationName + " after " + 
                                  MAX_RETRIES + " attempts due to: " + ioe.getMessage());
                    throw lastException;
                }
            }
        }
        
        // This should never happen, but just in case
        throw new IOException("Failed to execute request after " + MAX_RETRIES + " attempts");
    }

    /**
     * getUrl
     *
     * @param doi
     * @return
     */
    public String getUrl(String doi) {
        HttpGet httpGet = new HttpGet(this.url + "/doi/" + doi);
        try {
            HttpResponse response = executeWithRetry(httpGet, "getUrl");
            HttpEntity entity = response.getEntity();
            String data = null;

            if(entity != null) {
                data = EntityUtils.toString(entity, encoding);
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Response code: " + response.getStatusLine().getStatusCode() + ", " + data);
            }
            return data;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE,"IOException when get url",ioe);
            throw new RuntimeException("IOException when get url", ioe);
        }
    }

    /**
     * postUrl
     *
     * @param doi
     * @param url
     * @return
     */
    public String postUrl(String doi, String url) throws IOException {
        HttpPost httpPost = new HttpPost(this.url + "/doi");
        httpPost.setHeader("Content-Type", "text/plain;charset=UTF-8");
        httpPost.setEntity(new StringEntity("doi=" + doi + "\nurl=" + url, "utf-8"));

        HttpResponse response = executeWithRetry(httpPost, "postUrl");
        String data = EntityUtils.toString(response.getEntity(), encoding);
        if (response.getStatusLine().getStatusCode() != 201) {
            String errMsg = "Response from postUrl: " + response.getStatusLine().getStatusCode() + ", " + data;
            logger.log(Level.SEVERE, errMsg);
            throw new IOException(errMsg);
        }
        return data;
    }

    /**
     * getMetadata
     *
     * @param doi
     * @return
     */
    public String getMetadata(String doi) {
        HttpGet httpGet = new HttpGet(this.url + "/metadata/" + doi);
        httpGet.setHeader("Accept", "application/xml");
        try {
            HttpResponse response = executeWithRetry(httpGet, "getMetadata");
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 200) {
                String errMsg = "Response from getMetadata: " + response.getStatusLine().getStatusCode() + ", " + data;
                logger.log(Level.SEVERE, errMsg);
                throw new RuntimeException(errMsg);
            }
            return data;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException when get metadata", ioe);
            throw new RuntimeException("IOException when get metadata", ioe);
        }
    }
    
    /**
     * getMetadataViaRestApi
     * a temporary/dev. version of the method utilizing REST API instead of MDS
     *
     * @param doi
     * @return
     */
    public String getMetadataViaRestApi(String doi) {
        HttpGet httpGet = new HttpGet(this.restApiUrl + "/dois/" + doi);

        try {
            HttpResponse response = executeWithRetry(httpGet, "getMetadataViaRestApi");
            String restApiRawData = EntityUtils.toString(response.getEntity(), encoding);
            
            logger.fine("REST API raw data: " + restApiRawData);
            
            if (response.getStatusLine().getStatusCode() != 200) {
                String errMsg = "getMetadataViaRestApi, Response: " + response.getStatusLine().getStatusCode() + ", " + restApiRawData;
                logger.log(Level.SEVERE, errMsg);
                throw new RuntimeException(errMsg);
            }

            JsonObject restApiJson = JsonUtil.getJsonObject(restApiRawData);
            String xmlEncoded = null; 
            
            JsonObject restApiJsonData = restApiJson.getJsonObject("data");
            if (restApiJsonData != null) {
                JsonObject restApiJsonAttributes = restApiJsonData.getJsonObject("attributes");
                if (restApiJsonAttributes != null) {
                    xmlEncoded = restApiJsonAttributes.getString("xml");
                }
            }
            logger.fine("encoded XML entry: " + xmlEncoded);
            
            String metadata = null; // what we want to return, registration metadata in the XML format

            if (xmlEncoded != null) {
                // Stripping any newlines below may be unnecessary - it is likely
                // always returned as a continuous string; but shouldn't hurt 
                // either. 
                metadata = new String(Base64.getDecoder().decode(xmlEncoded.replaceAll("[\\r\\n]", "")), encoding);
            }
            
            logger.fine("decoded XML metadata: " + metadata);
            return metadata;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException in getMetadataViaRestApi", ioe);
            throw new RuntimeException("IOException in getMetadataViaRestAPi", ioe);
        }
    }
    
    /**
     * testDOIExists
     *
     * @param doi
     * @return boolean true if identifier already exists on DataCite site
     */
    public boolean testDOIExists(String doi) throws IOException {
        HttpGet httpGet = new HttpGet(this.url + "/metadata/" + doi);
        httpGet.setHeader("Accept", "application/xml");
        HttpResponse response = executeWithRetry(httpGet, "testDOIExists");
        if (response.getStatusLine().getStatusCode() != 200) {
            EntityUtils.consumeQuietly(response.getEntity());
            return false;
        }
        EntityUtils.consumeQuietly(response.getEntity());
        return true;
    }

    /**
     * postMetadata
     *
     * @param metadata
     * @return
     */
    public String postMetadata(String metadata) throws IOException {
        HttpPost httpPost = new HttpPost(this.url + "/metadata");
        httpPost.setHeader("Content-Type", "application/xml;charset=UTF-8");
        httpPost.setEntity(new StringEntity(metadata, "utf-8"));
        HttpResponse response = executeWithRetry(httpPost, "postMetadata");
        String data = EntityUtils.toString(response.getEntity(), encoding);
        if (response.getStatusLine().getStatusCode() != 201) {
            String errMsg = "Response from postMetadata: " + response.getStatusLine().getStatusCode() + ", " + data;
            logger.log(Level.SEVERE, errMsg);
            throw new IOException(errMsg);
        }
        return data;
    }

    /**
     * inactiveDataset
     *
     * @param doi
     * @return
     */
    public String inactiveDataset(String doi) {
        HttpDelete httpDelete = new HttpDelete(this.url + "/metadata/" + doi);
        try {
            HttpResponse response = executeWithRetry(httpDelete, "inactiveDataset");
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 200) {
                String errMsg = "Response code: " + response.getStatusLine().getStatusCode() + ", " + data;
                logger.log(Level.SEVERE, errMsg);
                throw new RuntimeException(errMsg);
            }
            return data;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException when inactive dataset", ioe);
            throw new RuntimeException("IOException when inactive dataset", ioe);
        }
    }

    /**
     * The main() method can be used to test the functionality on the command 
     * line outside of Dataverse. 
     * Un-comment out and modify the code below as needed. 
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        String doi = "10.5072/DVN/274533";
        DataCiteRESTfullClient client = new DataCiteRESTfullClient("https://mds.test.datacite.org", "https://api.test.datacite.org", "DATACITE_TEST_USERNAME", "DATACITE_TEST_PASSWORD");
//		System.out.println(client.getUrl(doi));
//		System.out.println(client.getMetadata(doi));
//        System.out.println(client.postMetadata(readAndClose("C:/Users/luopc/Desktop/datacite.xml", "utf-8")));
//        System.out.println(client.postUrl("10.5072/000000001", "http://opendata.pku.edu.cn/dvn/dv/DAIM/faces/study/StudyPage.xhtml?globalId=hdl:TEST/10027&studyListingIndex=1_1acc4e9f23fa10b3cc0500d9eb5e"));
//        client.close();
//		String doi2 = "10.1/1.0003";
//		SimpleRESTfullClient client2 = new SimpleRESTfullClient("https://162.105.140.119:8443/mds", "PKULIB.IR", "luopengcheng","localhost.keystore");
//		System.out.println(client2.getUrl("10.1/1.0002"));
//		System.out.println(client2.getUrl("10.1/1.0002"));
//		System.out.println(client2.getMetadata(doi2));
//		client2.postUrl("10.1/1.0003", "http://ir.pku.edu.cn");
//		System.out.println(client2.postUrl("10.1/1.0008", "http://ir.pku.edu.cn"));
//		System.out.println(client2.postMetadata(FileUtil.loadAsString(new File("C:/Users/luopc/Desktop/test/datacite-example-ResourceTypeGeneral_Collection-v3.0.xml"), "utf-8")));
//		System.out.println(client2.getMetadata("10.1/1.0007"));
//		System.out.println(client2.inactiveDataSet("10.1/1.0007"));
//		client2.close();
}

    
//    private static String readAndClose(String file, String encoding) throws IOException{
//        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),encoding));
//        StringBuilder str = new StringBuilder();
//        String line;
//        while((line = in.readLine()) != null){
//            str.append(line);
//        }
//        in.close();
//        return str.toString();
//    }

}
