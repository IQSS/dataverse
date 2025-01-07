/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.pidproviders.doi.datacite;


import java.io.Closeable;
import java.io.IOException;

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

/**
 * DataCiteRESTfullClient
 *
 * @author luopc
 *
 */
public class DataCiteRESTfullClient implements Closeable {
    
    private static final Logger logger = Logger.getLogger(DataCiteRESTfullClient.class.getCanonicalName());

    private String url;
    private CloseableHttpClient httpClient;
    private HttpClientContext context;
    private String encoding = "utf-8";
    
    public DataCiteRESTfullClient(String url, String username, String password) {
        this.url = url;
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
            HttpResponse response = httpClient.execute(httpGet,context);
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

        HttpResponse response = httpClient.execute(httpPost, context);
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
            HttpResponse response = httpClient.execute(httpGet,context);
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 200) {
                String errMsg = "Response from getMetadata: " + response.getStatusLine().getStatusCode() + ", " + data;
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
    public boolean testDOIExists(String doi) throws IOException {
        HttpGet httpGet = new HttpGet(this.url + "/metadata/" + doi);
        httpGet.setHeader("Accept", "application/xml");
        HttpResponse response = httpClient.execute(httpGet, context);
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
        HttpResponse response = httpClient.execute(httpPost, context);
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
            HttpResponse response = httpClient.execute(httpDelete,context);
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

    public static void main(String[] args) throws Exception {
        String doi = "10.5072/DVN/274533";
        DataCiteRESTfullClient client = new DataCiteRESTfullClient("https://mds.test.datacite.org", "DATACITE_TEST_USERNAME", "DATACITE_TEST_PASSWORD");
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
