/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataFile; 
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 *
 * This is an experimental, JMS-based implementation of asynchronous 
 * ingest. (experimental is the key! it may go away!)
 * 
 * @author Leonid Andreev 
 */
@MessageDriven(mappedName = "jms/DataverseIngest", activationConfig =  {@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"), @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")})
public class IngestMessageBean implements MessageListener {
    private static final Logger logger = Logger.getLogger(IngestMessageBean.class.getCanonicalName());
    @EJB DatasetServiceBean datasetService;
    @EJB DataFileServiceBean datafileService;
    @EJB IngestServiceBean ingestService; 

   
    public IngestMessageBean() {
    }
    
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void onMessage(Message message) {
        IngestMessage ingestMessage = null;
        DatasetVersion sv = null;
        List successfulFiles = new ArrayList();
        List problemFiles = new ArrayList();
        
        try {           
            ObjectMessage om = (ObjectMessage) message;
            ingestMessage = (IngestMessage) om.getObject();
            
            Iterator iter = ingestMessage.getFiles().iterator();
            while (iter.hasNext()) {
                DataFile dataFile = (DataFile) iter.next();
                
                try {
                    logger.info("Start ingest job;");
                        // do the ingest thing: 
                    // parseXML( new DSBWrapper().ingest(fileBean) , fileBean.getFileMetadata() );
                    // successfulFiles.add(fileBean);
                    if (ingestService.ingestAsTabular(dataFile)) {
                        //Thread.sleep(60000);
                        logger.info("Finish ingest job;");
                    } else {
                        logger.info("Error occurred during ingest job!");
                        problemFiles.add(dataFile);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    problemFiles.add(dataFile);
                }
                
            }

            if (!successfulFiles.isEmpty()) {
                // We used to send emails when jobs were completed:
                //datasetFileService.addIngestedFiles(ingestMessage.getDatasetId(), ingestMessage.getVersionNote(), successfulFiles, ingestMessage.getIngestUserId());
            }
                    
            
        } catch (JMSException ex) {
            ex.printStackTrace(); // error in getting object from message; can't send e-mail
            
        } catch (Exception ex) { 
            ex.printStackTrace();
            // if a general exception is caught that means the entire upload failed
            //if (ingestMessage.sendErrorMessage()) {
            //    mailService.sendIngestCompletedNotification(ingestMessage, null, ingestMessage.getFileBeans());
            //}
            
        } finally {
            // when we're done, go ahead and remove the lock
            try {
                //datasetService.removeDatasetLock( ingestMessage.getDatasetId() );
            } catch (Exception ex) {
                ex.printStackTrace(); // application was unable to remove the datasetLock
            }
            // We used to send emails when jobs were completed:
            //if ( ingestMessage.sendInfoMessage() || ( problemFiles.size() >= 0 && ingestMessage.sendErrorMessage() ) ) {
            //        mailService.sendIngestCompletedNotification(ingestMessage, successfulFiles, problemFiles);
            //} 
        }
    }
 
    
}
