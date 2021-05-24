package edu.harvard.iq.dataverse.globalid;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Http client for communicating with datacite via rest api
 * 
 * @author madryk
 * @see https://support.datacite.org/docs/api
 */
@ApplicationScoped
public class DataCiteRestApiClient {
    private static final Logger logger = LoggerFactory.getLogger(DataCiteRestApiClient.class);

    private SettingsServiceBean settingsService;

    private ObjectMapper objectMapper = new ObjectMapper();

    // -------------------- CONSTRUCTORS --------------------
    
    DataCiteRestApiClient() {
        // JEE requirement
    }
    
    @Inject
    public DataCiteRestApiClient(SettingsServiceBean settingsService) {
        this.settingsService = settingsService;
    }
    
    // -------------------- LOGIC --------------------

    @PostConstruct
    public void postConstruct() {
        objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Makes an GET request to DataCite api which obtains information
     * about provided doi.
     * 
     * @param doiAuthority - authority part of the doi
     * @param doiIdentifier - identifier part of the doi
     * @return response from DataCite parsed into an object
     * @throws IOException in case of in communication with DataCite
     * @see https://support.datacite.org/docs/api-get-doi - documentation of invoked api endpoint
     * @see https://api.test.datacite.org/dois/10.5438/0012 - example response 
     */
    public DataCiteFindDoiResponse findDoi(String doiAuthority, String doiIdentifier) throws IOException {
        
        URI findDoiUri = URI.create(settingsService.getValueForKey(Key.DoiDataCiteRestApiUrl) + "/dois/" + doiAuthority + "/" + doiIdentifier);
        
        try {
            return Request.Get(findDoiUri)
                    .execute()
                    .handleResponse(new DataCiteApiResponseHandler(objectMapper));
        } catch (HttpResponseException e) {
            logger.warn("Non 200 response code on find doi - url: {}, statusCode: {}, reason: {}",
                    findDoiUri.toString(), e.getStatusCode(), e.getReasonPhrase());
            throw e;
        }
    }
    
    // -------------------- INNER CLASSES --------------------
    
    private static class DataCiteApiResponseHandler implements ResponseHandler<DataCiteFindDoiResponse> {

        private ObjectMapper objectMapper;
        
        public DataCiteApiResponseHandler(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }
        
        @Override
        public DataCiteFindDoiResponse handleResponse(HttpResponse response) throws IOException {
            StatusLine statusLine = response.getStatusLine();
            
            if (statusLine.getStatusCode() != 200) {
                throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new ClientProtocolException("Response contains no content");
            }
            
            try (InputStream inputStream = entity.getContent()) {
                return objectMapper.readValue(inputStream, DataCiteFindDoiResponse.class);
            }
        }
        
    }

}
