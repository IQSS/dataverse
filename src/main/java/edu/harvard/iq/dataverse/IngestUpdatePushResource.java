/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.faces.application.FacesMessage;

import org.primefaces.push.EventBus;
import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
import org.primefaces.push.annotation.PathParam;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.annotation.Singleton;
import org.primefaces.push.impl.JSONEncoder;
import java.util.logging.Logger;
 
 

/**
 *
 * @author Leonid Andreev
 */
@PushEndpoint("/ingest/dataset/{datasetId}")
@Singleton
public class IngestUpdatePushResource {
    private final Logger logger = Logger.getLogger(IngestUpdatePushResource.class.getCanonicalName());
    
    @PathParam("datasetId")
    private String datasetId;
    /*
    @OnOpen
    public void onOpen(RemoteEndpoint r, EventBus eventBus) {
        logger.info("OnOpen {"+datasetId+"} "+r.toString()); 
    }
 
    @OnClose
    public void onClose(RemoteEndpoint r, EventBus eventBus) {
        logger.info("OnClose {"+datasetId+"} "+r.toString());
    }
    */

    
    @OnMessage(encoders = {JSONEncoder.class})
    public FacesMessage onMessage(FacesMessage message) {
        logger.fine("OnMessage {"+datasetId+"}: "+message.getDetail());
        return message;
    }
        
}
