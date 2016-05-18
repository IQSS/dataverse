/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client.oai;

import com.lyncode.xoai.model.oaipmh.Granularity;
import com.lyncode.xoai.model.oaipmh.MetadataFormat;
import com.lyncode.xoai.model.oaipmh.Set;
import com.lyncode.xoai.serviceprovider.ServiceProvider;
import com.lyncode.xoai.serviceprovider.client.HttpOAIClient;
import com.lyncode.xoai.serviceprovider.exceptions.InvalidOAIResponse;
import com.lyncode.xoai.serviceprovider.exceptions.NoSetHierarchyException;
import com.lyncode.xoai.serviceprovider.model.Context;
import com.lyncode.xoai.serviceprovider.parameters.ListIdentifiersParameters;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 * @author Leonid Andreev
 */
public class OaiHandler implements Serializable {
    
    public OaiHandler() {
        
    }
    
    public OaiHandler(String baseOaiUrl) {
        this.baseOaiUrl = baseOaiUrl; 
    }
    
    public OaiHandler(String baseOaiUrl, String setName) {
        this.baseOaiUrl = baseOaiUrl;
        this.setName = setName; 
    }
        
    private String baseOaiUrl; //= harvestingClient.getHarvestingUrl();
    private String metadataPrefix; // = harvestingClient.getMetadataPrefix();
    private String setName; 
    
    private ServiceProvider serviceProvider; 
    
    private ServiceProvider getServiceProvider() throws OaiHandlerException {
        if (serviceProvider == null) {
            if (baseOaiUrl == null) {
                throw new OaiHandlerException("Could not instantiate Service Provider, missing OAI server URL.");
            }
            Context context = new Context();

            context.withBaseUrl(baseOaiUrl);
            context.withGranularity(Granularity.Second);
            context.withOAIClient(new HttpOAIClient(baseOaiUrl));

            serviceProvider = new ServiceProvider(context);
        }
        
        return serviceProvider;
    }
    
    public List<String> runListSets() throws OaiHandlerException {
    
        ServiceProvider sp = getServiceProvider(); 
        
        Iterator<Set> setIter;
        
        try {
            setIter = sp.listSets();
        } catch (NoSetHierarchyException nshe) {
            return null; 
        } catch (InvalidOAIResponse ior) {
            throw new OaiHandlerException("No valid response received from the OAI server.");
        }
        
        List<String> sets = new ArrayList<>();

        while ( setIter.hasNext()) {
            Set set = setIter.next();
            String setSpec = set.getSpec();
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
                
        Iterator<MetadataFormat> setIter;
        
        try {
            setIter = sp.listMetadataFormats();
        } catch (InvalidOAIResponse ior) {
            throw new OaiHandlerException("No valid response received from the OAI server."); 
        }
        
        List<String> formats = new ArrayList<>();

        while ( setIter.hasNext()) {
            MetadataFormat format = setIter.next();
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
    
    public void runIdentify() {
        
    }
}
