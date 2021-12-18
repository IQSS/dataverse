/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.globalid;


import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Http client for communicating with datacite via mds api
 *
 *
 * @author luopc
 * @see https://support.datacite.org/docs/mds-api-guide
 */
public class DataCiteMdsApiClient implements Closeable {

    private static final Logger logger = Logger.getLogger(DataCiteMdsApiClient.class.getCanonicalName());

    private String url;
    private CloseableHttpClient httpClient;
    private HttpClientContext context;
    private String encoding = "utf-8";

    public DataCiteMdsApiClient(String url, String username, String password) throws IOException {
        this.url = url;
        try {
            context = HttpClientContext.create();
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(null, -1),
                                         new UsernamePasswordCredentials(username, password));
            context.setCredentialsProvider(credsProvider);

            httpClient = HttpClients.createDefault();
        } catch (Exception ioe) {
            close();
            logger.log(Level.SEVERE, "Fail to init Client", ioe);
            throw new RuntimeException("Fail to init Client", ioe);
        }
    }

    public void close() {
        if (this.httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException io) {
                logger.warning("IOException closing hhtpClient: " + io.getMessage());
            }
        }
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
            HttpResponse response = httpClient.execute(httpGet, context);
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Response code: " + response.getStatusLine().getStatusCode() + ", " + data);
            }
            return data;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException when get url", ioe);
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
    public String postUrl(String doi, String url) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(this.url + "/doi");
        httpPost.setHeader("Content-Type", "text/plain;charset=UTF-8");
        httpPost.setEntity(new StringEntity("doi=" + doi + "\nurl=" + url, "utf-8"));

        try {
            HttpResponse response = httpClient.execute(httpPost, context);
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 201) {
                String errMsg = "Response code: " + response.getStatusLine().getStatusCode() + ", " + data;
                logger.log(Level.SEVERE, errMsg);
                throw new RuntimeException(errMsg);
            }
            return data;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException when post url");
            throw new RuntimeException("IOException when post url", ioe);
        }
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
            HttpResponse response = httpClient.execute(httpGet, context);
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 200) {
                String errMsg = "Response code: " + response.getStatusLine().getStatusCode() + ", " + data;
                logger.log(Level.SEVERE, errMsg);
                throw new RuntimeException(errMsg);
            }
            return data;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException when get metadata");
            throw new RuntimeException("IOException when get metadata", ioe);
        }
    }

    /**
     * testDOIExists
     *
     * @param doi
     * @return boolean true if identifier already exists on DataCite site
     */
    public boolean testDOIExists(String doi) {
        HttpGet httpGet = new HttpGet(this.url + "/metadata/" + doi);
        httpGet.setHeader("Accept", "application/xml");
        try {
            HttpResponse response = httpClient.execute(httpGet, context);
            if (response.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consumeQuietly(response.getEntity());
                return false;
            }
            EntityUtils.consumeQuietly(response.getEntity());
            return true;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException when get metadata");
            throw new RuntimeException("IOException when get metadata", ioe);
        }
    }

    /**
     * postMetadata
     *
     * @param metadata
     * @return
     */
    public String postMetadata(String metadata) {
        HttpPost httpPost = new HttpPost(this.url + "/metadata");
        httpPost.setHeader("Content-Type", "application/xml;charset=UTF-8");
        try {
            httpPost.setEntity(new StringEntity(metadata, "utf-8"));
            HttpResponse response = httpClient.execute(httpPost, context);

            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 201) {
                String errMsg = "Response code: " + response.getStatusLine().getStatusCode() + ", " + data;
                logger.log(Level.SEVERE, errMsg);
                throw new RuntimeException(errMsg);
            }
            return data;

        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException when post metadata");
            throw new RuntimeException("IOException when post metadata", ioe);
        }
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
            HttpResponse response = httpClient.execute(httpDelete, context);
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 200) {
                String errMsg = "Response code: " + response.getStatusLine().getStatusCode() + ", " + data;
                logger.log(Level.SEVERE, errMsg);
                throw new RuntimeException(errMsg);
            }
            return data;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException when inactive dataset");
            throw new RuntimeException("IOException when inactive dataset", ioe);
        }
    }

    public String deleteDoi(String doi) {
        HttpDelete httpDelete = new HttpDelete(this.url + "/doi/" + doi);
        try {
            HttpResponse response = httpClient.execute(httpDelete, context);
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 200) {
                String errMsg = "Response code: " + response.getStatusLine().getStatusCode() + ", " + data;
                logger.log(Level.SEVERE, errMsg);
                throw new RuntimeException(errMsg);
            }
            return data;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException when inactive dataset");
            throw new RuntimeException("IOException when inactive dataset", ioe);
        }
    }
}
