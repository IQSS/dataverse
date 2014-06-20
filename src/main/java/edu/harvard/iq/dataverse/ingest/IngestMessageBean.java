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
import edu.harvard.iq.dataverse.Dataset;
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

        try {
            ObjectMessage om = (ObjectMessage) message;
            ingestMessage = (IngestMessage) om.getObject();

            Iterator iter = ingestMessage.getFileIds().iterator();
            Long datafile_id = null; 
            while (iter.hasNext()) {
                datafile_id = (Long) iter.next();

                logger.info("Start ingest job;");
                if (ingestService.ingestAsTabular(datafile_id)) {
                    //Thread.sleep(10000);
                    logger.info("Finish ingest job;");
                } else {
                    logger.info("Error occurred during ingest job!");
                }
            }
            
            // Remove the dataset lock: 
            // (note that the assumption here is that all of the datafiles
            // packed into this IngestMessage belong to the same dataset) 
            if (datafile_id != null) {
                DataFile datafile = datafileService.find(datafile_id);
                if (datafile != null) {
                    Dataset dataset = datafile.getOwner();
                    if (dataset != null && dataset.getId() != null) {
                        datasetService.removeDatasetLock(dataset.getId());
                    }
                } 
            } 

        } catch (JMSException ex) {
            ex.printStackTrace(); // error in getting object from message; can't send e-mail

        } catch (Exception ex) {
            ex.printStackTrace();
            // if a general exception is caught that means the entire upload failed
            // some form of a notification - ?

        } finally {
            // when we're done, go ahead and remove the lock (not yet)
            try {
                //datasetService.removeDatasetLock( ingestMessage.getDatasetId() );
            } catch (Exception ex) {
                ex.printStackTrace(); // application was unable to remove the datasetLock
            }
            // some form of a notification - ?
        }
    }
 
    
}
