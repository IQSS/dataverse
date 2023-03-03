package edu.harvard.iq.dataverse.harvest.client.oai;

import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.model.oaipmh.results.record.Header;
import io.gdcc.xoai.model.oaipmh.results.MetadataFormat;
import io.gdcc.xoai.model.oaipmh.results.Set;
import io.gdcc.xoai.serviceprovider.ServiceProvider;
import io.gdcc.xoai.serviceprovider.exceptions.BadArgumentException;
import io.gdcc.xoai.serviceprovider.exceptions.InvalidOAIResponse;
import io.gdcc.xoai.serviceprovider.exceptions.NoSetHierarchyException;
import io.gdcc.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import io.gdcc.xoai.serviceprovider.model.Context;
import io.gdcc.xoai.serviceprovider.parameters.ListIdentifiersParameters;
import edu.harvard.iq.dataverse.harvest.client.FastGetRecord;
import static edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean.DATAVERSE_PROPRIETARY_METADATA_API;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import io.gdcc.xoai.serviceprovider.client.JdkHttpOaiClient;
import java.io.IOException;
import java.io.Serializable;
import java.net.http.HttpClient;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import javax.xml.transform.TransformerException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Leonid Andreev
 */
public class OaiHandler implements Serializable {
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.oai.OaiHandler");
    
    public OaiHandler() {
        
    }
    
    public OaiHandler(String baseOaiUrl) {
        this.baseOaiUrl = baseOaiUrl; 
    }
    
    public OaiHandler(String baseOaiUrl, String metadataPrefix) {
        this.baseOaiUrl = baseOaiUrl;
        this.metadataPrefix = metadataPrefix; 
    }
      
    public OaiHandler(HarvestingClient harvestingClient) throws OaiHandlerException {
        this.baseOaiUrl = harvestingClient.getHarvestingUrl();
        this.metadataPrefix = harvestingClient.getMetadataPrefix();
        
        if (StringUtils.isEmpty(baseOaiUrl)) {
            throw new OaiHandlerException("Valid OAI url is needed to create a handler");
        }
        this.baseOaiUrl = harvestingClient.getHarvestingUrl();
        if (StringUtils.isEmpty(metadataPrefix)) {
            throw new OaiHandlerException("HarvestingClient must have a metadataPrefix to create a handler");
        }
        this.metadataPrefix = harvestingClient.getMetadataPrefix();
        
        if (!StringUtils.isEmpty(harvestingClient.getHarvestingSet())) {
            this.setName = harvestingClient.getHarvestingSet();
        }
        
        this.fromDate = harvestingClient.getLastNonEmptyHarvestTime();
        
        this.customHeaders = makeCustomHeaders(harvestingClient.getCustomHttpHeaders());
        
        this.harvestingClient = harvestingClient;
    }
    
    private String baseOaiUrl; 
    private String dataverseApiUrl; // if the remote server is a Dataverse and we access its native metadata
    private String metadataPrefix; 
    private String setName; 
    private Date   fromDate;
    private Boolean setListTruncated = false;
    private Map<String,String> customHeaders = null;
    
    private ServiceProvider serviceProvider; 
    
    private HarvestingClient harvestingClient; 
    
    public String getSetName() {
        return setName; 
    }
    
    public String getBaseOaiUrl() {
        return baseOaiUrl; 
    }
    
    public Date getFromDate() {
        return fromDate;
    }
    
    public String getMetadataPrefix() {
        return metadataPrefix; 
    }
    
    public HarvestingClient getHarvestingClient() {
        return this.harvestingClient;
    }
    
    public void withSetName(String setName) {
        this.setName = setName;
    }
        
    public void withMetadataPrefix(String metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }
    
    public void withFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }
    
    public void setHarvestingClient(HarvestingClient harvestingClient) {
        this.harvestingClient = harvestingClient; 
    }
    
    public boolean isSetListTruncated() {
        return setListTruncated;
    }
    
    public Map<String,String> getCustomHeaders() {
        return this.customHeaders;
    }
    
    public void setCustomHeaders(Map<String,String> customHeaders) {
       this.customHeaders = customHeaders;
    }
    
    public ServiceProvider getServiceProvider() throws OaiHandlerException {
        if (serviceProvider == null) {
            if (baseOaiUrl == null) {
                throw new OaiHandlerException("Could not instantiate Service Provider, missing OAI server URL.");
            }
            Context context = new Context();

            context.withBaseUrl(baseOaiUrl);
            context.withGranularity(Granularity.Second);
            
            JdkHttpOaiClient.Builder xoaiClientBuilder = JdkHttpOaiClient.newBuilder().withBaseUrl(getBaseOaiUrl());
            if (getCustomHeaders() != null) {
                for (String headerName : getCustomHeaders().keySet()) {
                    logger.fine("adding custom header; name: "+headerName+", value: "+getCustomHeaders().get(headerName));
                }   
                xoaiClientBuilder = xoaiClientBuilder.withCustomHeaders(getCustomHeaders());
            }
            context.withOAIClient(xoaiClientBuilder.build());
            serviceProvider = new ServiceProvider(context);
        }
        
        return serviceProvider;
    }
    
    public ArrayList<String> runListSets() throws OaiHandlerException {
    
        ServiceProvider sp = getServiceProvider(); 
        
        Iterator<Set> setIter;
        
        long startMilSec = new Date().getTime();
        
        try {
            setIter = sp.listSets();
        } catch (NoSetHierarchyException nshe) {
            return null; 
        } catch (InvalidOAIResponse ior) {
            throw new OaiHandlerException("No valid response received from the OAI server.");
        }
        
        ArrayList<String> sets = new ArrayList<>();

        int count = 0;
        
        while ( setIter.hasNext()) {
            count++;
            Set set = setIter.next();
            String setSpec = set.getSpec();
            /*
            if (set.getDescriptions() != null && !set.getDescriptions().isEmpty()) {
                Description description = set.getDescriptions().get(0);
                
            }
            */
            
            if (count >= 100) {
                // Have we been waiting more than 30 seconds?
                if (new Date().getTime() - startMilSec > 30000) {
                    setListTruncated = true;
                    break; 
                }
            }
                         
            if (!StringUtils.isEmpty(setSpec)) {
                sets.add(setSpec);
            }
        }

        if (sets.size() < 1) {
            return null;
        }
        return sets; 
        
    }
    
    public List<String> runListMetadataFormats() throws OaiHandlerException {
        ServiceProvider sp = getServiceProvider();
                
        Iterator<MetadataFormat> mfIter;
        
        try {
            mfIter = sp.listMetadataFormats();
        } catch (IdDoesNotExistException idnee) {
            // TODO: 
            // not sure why this exception is now thrown by List Metadata Formats (?)
            // but looks like it was added in xoai 4.2. 
            // It appears that the answer is, they added it because you can 
            // call ListMetadataFormats on a specific identifier, optionally, 
            // and therefore it is possible to get back that response. Of course 
            // it will never be the case when calling it on an entire repository. 
            // But it's ok. 
            throw new OaiHandlerException("Id does not exist exception");
        } catch (InvalidOAIResponse ior) {
            throw new OaiHandlerException("No valid response received from the OAI server."); 
        }
        
        List<String> formats = new ArrayList<>();

        while ( mfIter.hasNext()) {
            MetadataFormat format = mfIter.next();
            String formatName = format.getMetadataPrefix();
            if (!StringUtils.isEmpty(formatName)) {
                formats.add(formatName);
            }
        }

        if (formats.size() < 1) {
            return null; 
        }
        
        return formats;
    }
    
    public Iterator<Header> runListIdentifiers() throws OaiHandlerException {
        ListIdentifiersParameters parameters = buildListIdentifiersParams();
        try {
            return getServiceProvider().listIdentifiers(parameters);
        } catch (BadArgumentException bae) {
            throw new OaiHandlerException("BadArgumentException thrown when attempted to run ListIdentifiers");
        }
                
    }
    
    public FastGetRecord runGetRecord(String identifier, HttpClient httpClient) throws OaiHandlerException { 
        if (StringUtils.isEmpty(this.baseOaiUrl)) {
            throw new OaiHandlerException("Attempted to execute GetRecord without server URL specified.");
        }
        if (StringUtils.isEmpty(this.metadataPrefix)) {
            throw new OaiHandlerException("Attempted to execute GetRecord without metadataPrefix specified");
        }
        
        try {
            return new FastGetRecord(this, identifier, httpClient);
        } catch (ParserConfigurationException pce) {
            throw new OaiHandlerException("ParserConfigurationException executing GetRecord: "+pce.getMessage());
        } catch (SAXException se) {
            throw new OaiHandlerException("SAXException executing GetRecord: "+se.getMessage());
        } catch (TransformerException te) {
            throw new OaiHandlerException("TransformerException executing GetRecord: "+te.getMessage());
        } catch (IOException ioe) {
            throw new OaiHandlerException("IOException executing GetRecord: "+ioe.getMessage());
        }
    }
    
    
    private ListIdentifiersParameters buildListIdentifiersParams() throws OaiHandlerException {
        ListIdentifiersParameters mip = ListIdentifiersParameters.request();
        
        if (StringUtils.isEmpty(this.metadataPrefix)) {
            throw new OaiHandlerException("Attempted to create a ListIdentifiers request without metadataPrefix specified");
        }
        mip.withMetadataPrefix(metadataPrefix);

        if (this.fromDate != null) {
            mip.withFrom(this.fromDate.toInstant());
        }

        if (!StringUtils.isEmpty(this.setName)) {
            mip.withSetSpec(this.setName);
        }
        
        return mip;
    }
    
    public String getProprietaryDataverseMetadataURL(String identifier) {

        if (dataverseApiUrl == null) {
            dataverseApiUrl = baseOaiUrl.replaceFirst("/oai", "");
        }
        
        StringBuilder requestURL =  new StringBuilder(dataverseApiUrl);
        requestURL.append(DATAVERSE_PROPRIETARY_METADATA_API).append(identifier);

        return requestURL.toString();
    }
    
    public void runIdentify() {
        // not implemented yet
        // (we will need it, both for validating the remote server,
        // and to learn about its extended capabilities)
    }
    
    public Map<String,String> makeCustomHeaders(String headersString) {
        if (headersString != null) {
            String[] parts = headersString.split("\\\\n");
            HashMap<String,String> ret = new HashMap<>();
            logger.info("found "+parts.length+" parts");
            int count = 0;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].indexOf(':') > 0) {
                    String headerName = parts[i].substring(0, parts[i].indexOf(':'));
                    String headerValue = parts[i].substring(parts[i].indexOf(':')+1).strip();
                    
                    ret.put(headerName, headerValue);
                    count++;
                } 
                // simply skipping it if malformed; or we could throw an exception - ?
            }
            if (ret.size() > 0) {
                logger.info("returning the array with "+ret.size()+" name/value pairs");
                return ret;
            }
        }
        return null; 
    }
}
