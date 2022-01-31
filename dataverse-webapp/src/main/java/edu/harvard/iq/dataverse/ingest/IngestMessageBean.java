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

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestReport;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an experimental, JMS-based implementation of asynchronous
 * ingest. (experimental is the key! it may go away!)
 *
 * @author Leonid Andreev
 */
@MessageDriven(name = "IngestMessageBean", mappedName = "jms/DataverseIngest", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
})
public class IngestMessageBean implements MessageListener {
    private static final Logger logger = Logger.getLogger(IngestMessageBean.class.getCanonicalName());

    @EJB
    private DatasetDao datasetDao;
    @EJB
    private DataFileServiceBean datafileService;
    @EJB
    private IngestServiceBean ingestService;

    // -------------------- CONSTRUCTORS --------------------

    public IngestMessageBean() { }

    // -------------------- LOGIC --------------------

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void onMessage(Message message) {
        IngestMessage ingestMessage;

        Long datafile_id;

        try {
            ObjectMessage om = (ObjectMessage) message;
            ingestMessage = (IngestMessage) om.getObject();

            Iterator<Long> iter = ingestMessage.getFileIds().iterator();
            datafile_id = null;

            while (iter.hasNext()) {
                datafile_id = iter.next();

                logger.fine("Start ingest job;");
                try {
                    if (ingestService.ingestAsTabular(datafile_id)) {
                        logger.fine("Finished ingest job;");
                    } else {
                        logger.warning("Error occurred during ingest job for file id " + datafile_id + "!");
                    }
                } catch (Exception ex) {
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
                            errorReport.setErrorKey(IngestError.DB_FAIL);
                            errorReport.setDataFile(datafile);
                            datafile.setIngestReport(errorReport);
                            datafile.setDataTable(null);

                            logger.info("trying to save datafile and the failed ingest report, id=" + datafile_id);
                            datafileService.save(datafile);
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
                        datasetDao.removeDatasetLocks(dataset, DatasetLock.Reason.Ingest);
                    }
                }
            }
        } catch (JMSException je) {
            logger.log(Level.WARNING, "Error in getting object from message – can't send e-mail", je);
        }
    }
}
