package edu.harvard.iq.dataverse.pidproviders.doi.crossref;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrossRefRESTfullClient implements Closeable {

    private static final Logger logger = Logger.getLogger(CrossRefRESTfullClient.class.getCanonicalName());

    private final String url;
    private final String apiUrl;
    private final String username;
    private final String password;
    private final CloseableHttpClient httpClient;
    private final HttpClientContext context;
    private final String encoding = "utf-8";

    public CrossRefRESTfullClient(String url, String apiUrl, String username, String password) {
        this.url = url;
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        try {
            context = HttpClientContext.create();
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(null, -1),
                    new UsernamePasswordCredentials(username, password));
            context.setCredentialsProvider(credsProvider);

            httpClient = HttpClients.createDefault();
        } catch (Exception ioe) {
            close();
            logger.log(Level.SEVERE,"Fail to init Client",ioe);
            throw new RuntimeException("Fail to init Client", ioe);
        }
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

    public String getMetadata(String doi) {
        HttpGet httpGet = new HttpGet(this.apiUrl + "/works/" + doi);
        httpGet.setHeader("Accept", "application/json");
        try {
            HttpResponse response = httpClient.execute(httpGet);
            String data = EntityUtils.toString(response.getEntity(), encoding);
            if (response.getStatusLine().getStatusCode() != 200) {
                String errMsg = "Response from getMetadata: " + response.getStatusLine().getStatusCode() + ", " + data;
                logger.warning(errMsg);
                throw new RuntimeException(errMsg);
            }
            return data;
        } catch (IOException ioe) {
            logger.warning("IOException when get metadata");
            throw new RuntimeException("IOException when get metadata", ioe);
        }
    }

    public String postMetadata(String xml) throws IOException {
        HttpEntity entity = MultipartEntityBuilder.create()
                .addTextBody("operation", "doMDUpload")
                .addTextBody("login_id", username)
                .addTextBody("login_passwd", password)
                .addBinaryBody("fname", xml.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_XML, "metadata.xml")
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .build();

        HttpPost httpPost = new HttpPost(url + "/servlet/deposit");
        httpPost.setHeader("Accept", "*/*");
        httpPost.setEntity(entity);
        HttpResponse response = httpClient.execute(httpPost);

        String data = EntityUtils.toString(response.getEntity(), encoding);
        if (response.getStatusLine().getStatusCode() != 200) {
            String errMsg = "Response from postMetadata: " + response.getStatusLine().getStatusCode() + ", " + data;
            logger.warning(errMsg);
            throw new IOException(errMsg);
        }
        return data;
    }

    public boolean testDOIExists(String doi) throws IOException {
        HttpGet httpGet = new HttpGet(this.apiUrl + "/works/" + doi);
        httpGet.setHeader("Accept", "application/json");
        HttpResponse response = httpClient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() != 200) {
            EntityUtils.consumeQuietly(response.getEntity());
            return false;
        }
        EntityUtils.consumeQuietly(response.getEntity());
        return true;
    }
}
