/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.usage;

import edu.harvard.iq.dataverse.util.SystemConfig;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author luopc, Peking University
 */
@Stateless
public class UsageIndexServiceBean {
    
    private static final Logger logger = Logger.getLogger(
            UsageIndexServiceBean.class.getCanonicalName());
    private static final org.apache.log4j.Logger usageLogger = 
            org.apache.log4j.Logger.getLogger(
                    UsageIndexServiceBean.class.getCanonicalName());
    
    @EJB
    private SystemConfig systemConfig;
    private JestClient client;
    
    @PostConstruct
    public void init(){
        JestClientFactory factory = new JestClientFactory();
        String elasticSearchUrl = "http://" + systemConfig.getElasticSearchHostColonPort();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(elasticSearchUrl)
                .multiThreaded(true)
                .build());
        client = factory.getObject();
    }
    
    @PreDestroy
    public void close(){
        if(client != null){
            client.shutdownClient();
            client = null;
        }
    }
    
    public void index(Event event){
        usageLogger.info("[usage_msg]:"+event.toJson());
        Index index = new Index.Builder(event)
                .index(UsageConstant.INDEX_NAME)
                .type(UsageConstant.INDEX_TYPE).build();
        try {
            client.execute(index);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
}
