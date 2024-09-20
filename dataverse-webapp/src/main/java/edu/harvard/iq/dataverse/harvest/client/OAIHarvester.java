package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.api.imports.HarvestImporterType;
import edu.harvard.iq.dataverse.api.imports.HarvestImporterTypeResolver;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandler;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandlerException;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import org.apache.commons.io.FileUtils;
import org.dspace.xoai.model.oaipmh.Header;
import org.dspace.xoai.serviceprovider.exceptions.HarvestException;
import org.dspace.xoai.serviceprovider.exceptions.IdDoesNotExistException;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Harvester for OAI clients.
 */
@Stateless
@LocalBean
public class OAIHarvester implements Harvester<HarvesterParams.EmptyHarvesterParams> {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    ImportServiceBean importService;

    @EJB
    private HarvestImporterTypeResolver harvestImporterTypeResolver;

    // -------------------- LOGIC --------------------

    @Override
    public HarvestType harvestType() {
        return HarvestType.OAI;
    }

    @Override
    public Class<HarvesterParams.EmptyHarvesterParams> getParamsClass() {
        return HarvesterParams.EmptyHarvesterParams.class;
    }

    /**
     * @param harvestingClient     the harvesting client object
     * @param hdLogger             custom logger (specific to this harvesting run)
     */
    @Override
    public HarvesterResult harvest(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, HarvesterParams.EmptyHarvesterParams params) throws ImportException {
        logBeginOaiHarvest(hdLogger, harvestingClient);

        HarvesterResult result = new HarvesterResult();
        OaiHandler oaiHandler;

        try {
            oaiHandler = new OaiHandler(harvestingClient)
                    .withFetchedMetadataFormat();
        } catch (OaiHandlerException | IdDoesNotExistException ohe) {
            String errorMessage = "Failed to create OaiHandler for harvesting client "
                    + harvestingClient.getName()
                    + "; "
                    + ohe.getMessage();
            hdLogger.log(Level.SEVERE, errorMessage);
            throw new ImportException(errorMessage, ohe);
        }

        try {
            for (Iterator<Header> idIter = oaiHandler.runListIdentifiers(); idIter.hasNext(); ) {

                Header h = idIter.next();
                String identifier = h.getIdentifier();

                hdLogger.info("processing identifier: " + identifier);

                // Retrieve and process this record with a separate GetRecord call:
                processRecord(result, dataverseRequest, hdLogger, oaiHandler, identifier);

                hdLogger.info("Total content processed so far: " + result.getNumHarvested());
            }
        } catch (OaiHandlerException e) {
            throw new ImportException("Failed to run ListIdentifiers: " + e.getMessage(), e);
        }

        logCompletedOaiHarvest(hdLogger, harvestingClient);

        return result;

    }

    // -------------------- PRIVATE --------------------

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private void processRecord(HarvesterResult result, DataverseRequest dataverseRequest, Logger hdLogger, OaiHandler oaiHandler, String identifier) {
        logGetRecord(hdLogger, oaiHandler, identifier);
        File tempFile = null;

        try {
            FastGetRecord record = oaiHandler.runGetRecord(identifier);
            String errMessage = record.getErrorMessage();
            if (errMessage != null) {
                throw new HarvestException("Error calling GetRecord - " + errMessage);
            }

            if (record.isDeleted()) {
                hdLogger.info("Deleting harvesting dataset for " + identifier + ", per the OAI server's instructions.");

                importService.doDeleteHarvestedDataset(dataverseRequest, oaiHandler.getHarvestingClient(), identifier);
                result.incrementDeleted();

            } else {
                hdLogger.info("Successfully retrieved GetRecord response.");
                HarvestImporterType importType = harvestImporterTypeResolver.resolveImporterType(oaiHandler.getMetadataFormat())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unsupported import metadata format: " + oaiHandler.getMetadataFormat().getMetadataPrefix()));

                tempFile = record.getMetadataFile();
                String metadataFileContents = new String(Files.readAllBytes(tempFile.toPath()));

                hdLogger.info("importing " + importType + ": " + tempFile.getAbsolutePath());
                importService.doImportHarvestedDataset(dataverseRequest,
                        oaiHandler.getHarvestingClient(),
                        identifier,
                        importType,
                        metadataFileContents);

                result.incrementHarvested();

                hdLogger.fine("Harvest Successful for identifier " + identifier);
                hdLogger.fine("Size of this record: " + record.getMetadataFile().length());
            }
        } catch (Throwable e) {
            result.incrementFailed();
            logGetRecordException(hdLogger, oaiHandler, identifier, e);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    private void logBeginOaiHarvest(Logger hdLogger, HarvestingClient harvestingClient) {
        hdLogger.log(Level.INFO, "BEGIN HARVEST, oaiUrl="
                + harvestingClient.getHarvestingUrl()
                + ",set="
                + harvestingClient.getHarvestingSet()
                + ", metadataPrefix="
                + harvestingClient.getMetadataPrefix()
                + harvestingClient.getLastNonEmptyHarvestTime() == null ? "" : "from=" + harvestingClient.getLastNonEmptyHarvestTime());
    }

    private void logCompletedOaiHarvest(Logger hdLogger, HarvestingClient harvestingClient) {
        hdLogger.log(Level.INFO, "COMPLETED HARVEST, oaiUrl="
                + harvestingClient.getHarvestingUrl()
                + ",set="
                + harvestingClient.getHarvestingSet()
                + ", metadataPrefix="
                + harvestingClient.getMetadataPrefix()
                + harvestingClient.getLastNonEmptyHarvestTime() == null ? "" : "from=" + harvestingClient.getLastNonEmptyHarvestTime());
    }

    private void logGetRecord(Logger hdLogger, OaiHandler oaiHandler, String identifier) {
        hdLogger.log(Level.FINE, "Calling GetRecord: oaiUrl ="
                + oaiHandler.getBaseOaiUrl()
                + "?verb=GetRecord&identifier="
                + identifier
                + "&metadataPrefix=" + oaiHandler.getMetadataPrefix());
    }

    private void logGetRecordException(Logger hdLogger, OaiHandler oaiHandler, String identifier, Throwable e) {
        String errMessage = "Exception processing getRecord(), oaiUrl="
                + oaiHandler.getBaseOaiUrl()
                + ",identifier="
                + identifier
                + " "
                + e.getClass().getName()
                //+" (exception message suppressed)";
                + " "
                + e.getMessage();

        hdLogger.log(Level.SEVERE, errMessage);

        // temporary:
        e.printStackTrace();
    }

}
