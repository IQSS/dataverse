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

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataFile; 
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;

import java.util.Iterator;
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

        Long datafile_id = null; 
        
        try {
            ObjectMessage om = (ObjectMessage) message;
            ingestMessage = (IngestMessage) om.getObject();

            Iterator iter = ingestMessage.getFileIds().iterator();
            datafile_id = null; 
            
            while (iter.hasNext()) {
                datafile_id = (Long) iter.next();

                logger.fine("Start ingest job;");
                try {
                    if (ingestService.ingestAsTabular(datafile_id)) {
                        //Thread.sleep(10000);
                        logger.fine("Finished ingest job;");
                    } else {
                        logger.warning("Error occurred during ingest job for file id " + datafile_id + "!");
                    }
                } catch (Exception ex) {
                    //ex.printStackTrace();
                    // TODO: 
                    // this solution is working - but it would be cleaner to instead
                    // make sure that all the exceptions are interrupted and appropriate
                    // action taken still on the ingest service side. 
                    // -- L.A. Aug. 13 2014; 
                    logger.info("Unknown exception occurred  during ingest (supressed stack trace); re-setting ingest status.");
                    if (datafile_id != null) {
                        logger.fine("looking up datafile for id " + datafile_id);
                        DataFile datafile = datafileService.find(datafile_id);
                        if (datafile != null) {
                            datafile.SetIngestProblem();
                            IngestReport errorReport = new IngestReport();
                            errorReport.setFailure();
                            if (ex.getMessage() != null) {
                                errorReport.setReport("Ingest succeeded, but failed to save the ingested tabular data in the database: " + ex.getMessage());
                            } else {
                                errorReport.setReport("Ingest succeeded, but failed to save the ingested tabular data in the database; no further information is available");
                            }
                            errorReport.setDataFile(datafile);
                            datafile.setIngestReport(errorReport);
                            datafile.setDataTables(null);

                            logger.info("trying to save datafile and the failed ingest report, id=" + datafile_id);
                            datafile = datafileService.save(datafile);

                            Dataset dataset = datafile.getOwner();
                            if (dataset != null && dataset.getId() != null) {
                                //logger.info("attempting to remove dataset lock for dataset " + dataset.getId());
                                //datasetService.removeDatasetLock(dataset.getId());
                                ingestService.sendFailNotification(dataset.getId());
                            }
                        }
                    }
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
                        datasetService.removeDatasetLocks(dataset, DatasetLock.Reason.Ingest);
                    }
                } 
            } 

        } catch (JMSException ex) {
            ex.printStackTrace(); // error in getting object from message; can't send e-mail

        } finally {
            // when we're done, go ahead and remove the lock (not yet)
            try {
                //datasetService.removeDatasetLock( ingestMessage.getDatasetId() );
            } catch (Exception ex) {
                ex.printStackTrace(); // application was unable to remove the datasetLock
            }
        }
    }
 
    
}
