package edu.harvard.iq.dataverse.api.imports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestedDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.validation.DatasetFieldValidationService;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * @author ellenk
 */
@Stateless
public class ImportServiceBean {
    private static final Logger logger = Logger.getLogger(ImportServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    protected EjbDataverseEngine engineSvc;
    @EJB
    private DatasetDao datasetDao;
    @EJB
    private ImportDDIServiceBean importDDIService;
    @EJB
    private ImportGenericServiceBean importGenericService;
    @EJB
    private IndexServiceBean indexService;
    @Inject
    private HarvestedJsonParser harvestedJsonParser;
    @Inject
    private DatasetFieldValidationService fieldValidationService;

    // -------------------- LOGIC --------------------

    @TransactionAttribute(REQUIRES_NEW)
    public Dataset doImportHarvestedDataset(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, String harvestIdentifier, String metadataFormat, File metadataFile, PrintWriter cleanupLog) throws ImportException, IOException {
        if (harvestingClient == null || harvestingClient.getDataverse() == null) {
            throw new ImportException("importHarvestedDataset called wiht a null harvestingClient, or an invalid harvestingClient.");
        }
        Dataverse owner = harvestingClient.getDataverse();
        Dataset importedDataset;

        DatasetDTO dsDTO = null;
        String json = null;

        // TODO:
        // At the moment (4.5; the first official "export/harvest release"), there
        // are 3 supported metadata formats: DDI, DC and native Dataverse metadata
        // encoded in JSON. The 2 XML formats are handled by custom implementations;
        // each of the 2 implementations uses its own parsing approach. (see the
        // ImportDDIServiceBean and ImportGenerciServiceBean for details).
        // TODO: Need to create a system of standardized import plugins - similar to Stephen
        // Kraffmiller's export modules; replace the logic below with clean
        // programmatic lookup of the import plugin needed.

        if ("ddi".equalsIgnoreCase(metadataFormat) || "oai_ddi".equals(metadataFormat)
                || metadataFormat.toLowerCase().matches("^oai_ddi.*")) {
            try {
                String xmlToParse = new String(Files.readAllBytes(metadataFile.toPath()));
                // TODO:
                // import type should be configurable - it should be possible to
                // select whether you want to harvest with or without files,
                // ImportType.HARVEST vs. ImportType.HARVEST_WITH_FILES
                logger.fine("importing DDI " + metadataFile.getAbsolutePath());
                dsDTO = importDDIService.doImport(ImportType.HARVEST, xmlToParse);
            } catch (IOException | XMLStreamException | ImportException e) {
                throw new ImportException("Failed to process DDI XML record: " + e.getClass() + " (" + e.getMessage() + ")");
            }
        } else if ("dc".equalsIgnoreCase(metadataFormat) || "oai_dc".equals(metadataFormat)) {
            logger.fine("importing DC " + metadataFile.getAbsolutePath());
            try {
                String xmlToParse = new String(Files.readAllBytes(metadataFile.toPath()));
                dsDTO = importGenericService.processOAIDCxml(xmlToParse);
            } catch (IOException | XMLStreamException e) {
                throw new ImportException("Failed to process Dublin Core XML record: " + e.getClass() + " (" + e.getMessage() + ")");
            }
        } else if ("dataverse_json".equals(metadataFormat)) {
            // This is Dataverse metadata already formatted in JSON.
            // Simply read it into a string, and pass to the final import further down:
            logger.fine("Attempting to import custom dataverse metadata from file " + metadataFile.getAbsolutePath());
            json = new String(Files.readAllBytes(metadataFile.toPath()));
        } else {
            throw new ImportException("Unsupported import metadata format: " + metadataFormat);
        }

        if (json == null) {
            if (dsDTO != null) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                json = gson.toJson(dsDTO);
                logger.fine("JSON produced for the metadata harvested: " + json);
            } else {
                throw new ImportException("Failed to transform XML metadata format " + metadataFormat + " into a DatasetDTO");
            }
        }

        try {
            Dataset ds = harvestedJsonParser.parseDataset(json);

            ds.setOwner(owner);
            ds.getLatestVersion().setDatasetFields(ds.getLatestVersion().initDatasetFields());

            // Check data against validation contraints
            DatasetVersion versionToValidate = ds.getVersions().get(0);
            List<ValidationResult> validationResults = fieldValidationService.validateFieldsOfDatasetVersion(versionToValidate);
            if (!validationResults.isEmpty()) {
                // For migration and harvest, add NA for missing required values
                validationResults.forEach(r -> r.getField().setFieldValue(DatasetField.NA_VALUE));
            }

            // A Global ID is required, in order for us to be able to harvest and import
            // this dataset:
            if (StringUtils.isEmpty(ds.getGlobalIdString())) {
                throw new ImportException("The harvested metadata record with the OAI server identifier " + harvestIdentifier
                        + " does not contain a global unique identifier that we could recognize, skipping.");
            }

            ds.setHarvestedFrom(harvestingClient);
            ds.setHarvestIdentifier(harvestIdentifier);

            Dataset existingDs = datasetDao.findByGlobalId(ds.getGlobalIdString());

            if (existingDs != null) {
                // If this dataset already exists IN ANOTHER DATAVERSE
                // we are just going to skip it!
                if (existingDs.getOwner() != null && !owner.getId().equals(existingDs.getOwner().getId())) {
                    throw new ImportException("The dataset with the global id " + ds.getGlobalIdString() + " already exists, in the dataverse "
                            + existingDs.getOwner().getAlias() + ", skipping.");
                }
                // And if we already have a dataset with this same id, in this same
                // dataverse, but it is  LOCAL dataset (can happen!), we're going to
                // skip it also:
                if (!existingDs.isHarvested()) {
                    throw new ImportException("A LOCAL dataset with the global id " + ds.getGlobalIdString() + " already exists in this dataverse; skipping.");
                }
                // For harvested datasets, there should always only be one version.
                // We will replace the current version with the imported version.
                if (existingDs.getVersions().size() != 1) {
                    throw new ImportException("Error importing Harvested Dataset, existing dataset has " + existingDs.getVersions().size() + " versions");
                }
                // Purge all the SOLR documents associated with this client from the
                // index server:
                indexService.deleteHarvestedDocuments(existingDs);
                // files from harvested datasets are removed unceremoniously,
                // directly in the database. no need to bother calling the
                // DeleteFileCommand on them.
                for (DataFile harvestedFile : existingDs.getFiles()) {
                    DataFile merged = em.merge(harvestedFile);
                    em.remove(merged);
                }
                // TODO:
                // Verify what happens with the indexed files in SOLR?
                // are they going to be overwritten by the reindexing of the dataset?
                existingDs.setFiles(null);
                Dataset merged = em.merge(existingDs);
                // harvested datasets don't have physical files - so no need to worry about that.
                engineSvc.submit(new DestroyDatasetCommand(merged, dataverseRequest));
            }

            importedDataset = engineSvc.submit(new CreateHarvestedDatasetCommand(ds, dataverseRequest));

        } catch (JsonParseException | ImportException | CommandException ex) {
            logger.fine("Failed to import harvested dataset: " + ex.getClass() + ": " + ex.getMessage());
            File jsonDumpFile = new File(metadataFile.getAbsolutePath() + ".json");
            FileUtils.writeByteArrayToFile(jsonDumpFile, json.getBytes());

            logger.info("JSON produced saved in " + jsonDumpFile.getAbsolutePath() + ".json");
            throw new ImportException(
                    String.format("Failed to import harvested dataset: %s (%s)", ex.getClass(), ex.getMessage()), ex);
        }
        return importedDataset;
    }

    public JsonObject ddiToJson(String xmlToParse) throws ImportException {
        DatasetDTO dsDTO;

        try {
            dsDTO = importDDIService.doImport(ImportType.IMPORT, xmlToParse);
        } catch (XMLStreamException e) {
            throw new ImportException("XMLStreamException" + e);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(dsDTO);
        JsonReader jsonReader = Json.createReader(new StringReader(json));

        return jsonReader.readObject();
    }
}
