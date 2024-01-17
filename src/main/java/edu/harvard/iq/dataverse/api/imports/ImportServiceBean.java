/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.imports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestedDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ConstraintViolationUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import static jakarta.ejb.TransactionAttributeType.REQUIRES_NEW;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ellenk
 */
@Stateless
public class ImportServiceBean {
    @PersistenceContext(unitName="VDCNet-ejbPU")
    private EntityManager em;
    
    private static final Logger logger = Logger.getLogger(ImportServiceBean.class.getCanonicalName());

    @EJB
    protected EjbDataverseEngine engineSvc;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetFieldServiceBean datasetfieldService;

    @EJB
    MetadataBlockServiceBean metadataBlockService;
    @EJB
    SettingsServiceBean settingsService;
    
    @EJB 
    ImportDDIServiceBean importDDIService;
    @EJB
    ImportGenericServiceBean importGenericService;
    
    @EJB
    IndexServiceBean indexService;
    @EJB
    LicenseServiceBean licenseService;
    /**
     * This is just a convenience method, for testing migration.  It creates 
     * a dummy dataverse with the directory name as dataverse name & alias.
     * @param dvName
     * @param dataverseRequest
     * @return
     * @throws ImportException 
     */
    @TransactionAttribute(REQUIRES_NEW)
    public Dataverse createDataverse(String dvName, DataverseRequest dataverseRequest) throws ImportException {
        Dataverse d = new Dataverse();
        Dataverse root = dataverseService.findByAlias("root");
        d.setOwner(root);
        d.setAlias(dvName);
        d.setName(dvName);
        d.setAffiliation("affiliation");
        d.setPermissionRoot(false);
        d.setDescription("description");
        d.setDataverseType(Dataverse.DataverseType.RESEARCHERS);
        DataverseContact dc = new DataverseContact();
        dc.setContactEmail("pete@mailinator.com");
        ArrayList<DataverseContact> dcList = new ArrayList<>();
        dcList.add(dc);
        d.setDataverseContacts(dcList);
        try {
            d = engineSvc.submit(new CreateDataverseCommand(d, dataverseRequest, null, null));
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append("Error creating dataverse.");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                if (cause instanceof ConstraintViolationException) {
                    sb.append(ConstraintViolationUtil.getErrorStringForConstraintViolations(cause));
                }
            }
            logger.log(Level.SEVERE, sb.toString());
            System.out.println("Error creating dataverse: " + sb.toString());
            throw new ImportException(sb.toString());
        } catch (CommandException e) {
            throw new ImportException(e.getMessage());
        }
        return d;

    }

    @TransactionAttribute(REQUIRES_NEW)
    public JsonObjectBuilder handleFile(DataverseRequest dataverseRequest, Dataverse owner, File file, ImportType importType, PrintWriter validationLog, PrintWriter cleanupLog) throws ImportException, IOException {

        System.out.println("handling file: " + file.getAbsolutePath());
        String ddiXMLToParse;
        try {
            ddiXMLToParse = new String(Files.readAllBytes(file.toPath()));
            JsonObjectBuilder status = doImport(dataverseRequest, owner, ddiXMLToParse,file.getParentFile().getName() + "/" + file.getName(), importType, cleanupLog);
            status.add("file", file.getName());
            logger.log(Level.INFO, "completed doImport {0}/{1}", new Object[]{file.getParentFile().getName(), file.getName()});
            return status;
        } catch (ImportException ex) {
            String msg = "Import Exception processing file " + file.getParentFile().getName() + "/" + file.getName() + ", msg:" + ex.getMessage();
            logger.info(msg);
            if (validationLog!=null) {
                validationLog.println(msg);
            }
            return Json.createObjectBuilder().add("message", "Import Exception processing file " + file.getParentFile().getName() + "/" + file.getName() + ", msg:" + ex.getMessage());
        } catch (IOException e) {
            Throwable causedBy =e.getCause();
            while (causedBy != null && causedBy.getCause()!=null) {
                causedBy = causedBy.getCause();
            }
            String stackLine = "";
            if (causedBy != null && causedBy.getStackTrace() != null && causedBy.getStackTrace().length > 0) {
                stackLine = causedBy.getStackTrace()[0].toString();
            }
                 String msg = "Unexpected Error in handleFile(), file:" + file.getParentFile().getName() + "/" + file.getName();
                 if (e.getMessage()!=null) {
                     msg+= "message: " +e.getMessage(); 
                 }
                 msg += ", caused by: " +causedBy;
                 if (causedBy != null && causedBy.getMessage()!=null) {
                     msg+=", caused by message: "+ causedBy.getMessage();
                 }
                 msg += " at line: "+ stackLine;
                 
       
            
            validationLog.println(msg);
            e.printStackTrace();

            return Json.createObjectBuilder().add("message", "Unexpected Exception processing file " + file.getParentFile().getName() + "/" + file.getName() + ", msg:" + e.getMessage());

        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Dataset doImportHarvestedDataset(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, String harvestIdentifier, String metadataFormat, File metadataFile, Date oaiDateStamp, PrintWriter cleanupLog) throws ImportException, IOException {
        if (harvestingClient == null || harvestingClient.getDataverse() == null) {
            throw new ImportException("importHarvestedDataset called wiht a null harvestingClient, or an invalid harvestingClient.");
        }
        Dataverse owner = harvestingClient.getDataverse();
        Dataset importedDataset = null;

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
                logger.fine("importing DDI "+metadataFile.getAbsolutePath());
                dsDTO = importDDIService.doImport(ImportType.HARVEST, xmlToParse);
            } catch (IOException | XMLStreamException | ImportException e) {
                throw new ImportException("Failed to process DDI XML record: "+ e.getClass() + " (" + e.getMessage() + ")");
            }
        } else if ("dc".equalsIgnoreCase(metadataFormat) || "oai_dc".equals(metadataFormat)) {
            logger.fine("importing DC "+metadataFile.getAbsolutePath());
            try {
                String xmlToParse = new String(Files.readAllBytes(metadataFile.toPath()));
                dsDTO = importGenericService.processOAIDCxml(xmlToParse);
            } catch (IOException | XMLStreamException e) {
                throw new ImportException("Failed to process Dublin Core XML record: "+ e.getClass() + " (" + e.getMessage() + ")");
            }
        } else if ("dataverse_json".equals(metadataFormat)) {
            // This is Dataverse metadata already formatted in JSON. 
            // Simply read it into a string, and pass to the final import further down:
            logger.fine("Attempting to import custom dataverse metadata from file "+metadataFile.getAbsolutePath());
            json = new String(Files.readAllBytes(metadataFile.toPath())); 
        } else {
            throw new ImportException("Unsupported import metadata format: " + metadataFormat);
        }

        if (json == null) {
            if (dsDTO != null ) {
                // convert DTO to Json, 
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                json = gson.toJson(dsDTO);
                logger.fine("JSON produced for the metadata harvested: "+json);
            } else {
                throw new ImportException("Failed to transform XML metadata format "+metadataFormat+" into a DatasetDTO");
            }
        }

        JsonObject obj = JsonUtil.getJsonObject(json);
        //and call parse Json to read it into a dataset   
        try {
            JsonParser parser = new JsonParser(datasetfieldService, metadataBlockService, settingsService, licenseService, harvestingClient);
            parser.setLenient(true);
            Dataset ds = parser.parseDataset(obj);

            // For ImportType.NEW, if the metadata contains a global identifier, and it's not a protocol
            // we support, it should be rejected.
            // (TODO: ! - add some way of keeping track of supported protocols!)
            //if (ds.getGlobalId() != null && !ds.getProtocol().equals(settingsService.getValueForKey(SettingsServiceBean.Key.Protocol, ""))) {
            //    throw new ImportException("Could not register id " + ds.getGlobalId() + ", protocol not supported");
            //}
            ds.setOwner(owner);
            ds.getLatestVersion().setDatasetFields(ds.getLatestVersion().initDatasetFields());

            if (ds.getVersions().get(0).getReleaseTime() == null) {
                ds.getVersions().get(0).setReleaseTime(oaiDateStamp);
            }
            
            // Check data against required contraints
            List<ConstraintViolation<DatasetField>> violations = ds.getVersions().get(0).validateRequired();
            if (!violations.isEmpty()) {
                // For migration and harvest, add NA for missing required values
                for (ConstraintViolation<DatasetField> v : violations) {
                    DatasetField f =  v.getRootBean();
                    f.setSingleValue(DatasetField.NA_VALUE);
                }
            }

            // Check data against validation constraints
            // If we are migrating and "scrub migration data" is true we attempt to fix invalid data
            // if the fix fails stop processing of this file by throwing exception
            Set<ConstraintViolation> invalidViolations = ds.getVersions().get(0).validate();
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            if (!invalidViolations.isEmpty()) {
                for (ConstraintViolation<DatasetFieldValue> v : invalidViolations) {
                    DatasetFieldValue f = v.getRootBean();
                    boolean fixed = false;
                    boolean converted = false;
                    // TODO: Is this scrubbing something we want to continue doing? 
                    if (settingsService.isTrueForKey(SettingsServiceBean.Key.ScrubMigrationData, false)) {
                        fixed = processMigrationValidationError(f, cleanupLog, metadataFile.getName());
                        converted = true;
                        if (fixed) {
                            Set<ConstraintViolation<DatasetFieldValue>> scrubbedViolations = validator.validate(f);
                            if (!scrubbedViolations.isEmpty()) {
                                fixed = false;
                            }
                        }
                    }
                    if (!fixed) {
                        String msg = "Data modified - File: " + metadataFile.getName() + "; Field: " + f.getDatasetField().getDatasetFieldType().getDisplayName() + "; "
                                + "Invalid value:  '" + f.getValue() + "'" + " Converted Value:'" + DatasetField.NA_VALUE + "'";
                        cleanupLog.println(msg);
                        f.setValue(DatasetField.NA_VALUE);

                    }
                }
            }
            
            // A Global ID is required, in order for us to be able to harvest and import
            // this dataset:
            if (StringUtils.isEmpty(ds.getGlobalId().asString())) {
                throw new ImportException("The harvested metadata record with the OAI server identifier "+harvestIdentifier+" does not contain a global unique identifier that we could recognize, skipping.");
            }

            ds.setHarvestedFrom(harvestingClient);
            ds.setHarvestIdentifier(harvestIdentifier);
            
            Dataset existingDs = datasetService.findByGlobalId(ds.getGlobalId().asString());

            if (existingDs != null) {
                // If this dataset already exists IN ANOTHER DATAVERSE
                // we are just going to skip it!
                if (existingDs.getOwner() != null && !owner.getId().equals(existingDs.getOwner().getId())) {
                    throw new ImportException("The dataset with the global id "+ds.getGlobalId().asString()+" already exists, in the dataverse "+existingDs.getOwner().getAlias()+", skipping.");
                }
                // And if we already have a dataset with this same id, in this same
                // dataverse, but it is  LOCAL dataset (can happen!), we're going to 
                // skip it also: 
                if (!existingDs.isHarvested()) {
                    throw new ImportException("A LOCAL dataset with the global id "+ds.getGlobalId().asString()+" already exists in this dataverse; skipping.");
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
                    harvestedFile = null; 
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
            FileOutputStream savedJsonFileStream = new FileOutputStream(new File(metadataFile.getAbsolutePath() + ".json"));
            byte[] jsonBytes = json.getBytes();
            int i = 0;
            while (i < jsonBytes.length) {
                int chunkSize = i + 8192 <= jsonBytes.length ? 8192 : jsonBytes.length - i;
                savedJsonFileStream.write(jsonBytes, i, chunkSize);
                i += chunkSize;
                savedJsonFileStream.flush();
            }
            savedJsonFileStream.close();
            logger.info("JSON produced saved in " + metadataFile.getAbsolutePath() + ".json");
            throw new ImportException("Failed to import harvested dataset: " + ex.getClass() + " (" + ex.getMessage() + ")", ex);
        }
        return importedDataset;
    }

    public JsonObject ddiToJson(String xmlToParse) throws ImportException, XMLStreamException {
        DatasetDTO dsDTO = importDDIService.doImport(ImportType.IMPORT, xmlToParse);
        // convert DTO to Json,
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(dsDTO);

        return JsonUtil.getJsonObject(json);
    }
    
    public JsonObjectBuilder doImport(DataverseRequest dataverseRequest, Dataverse owner, String xmlToParse, String fileName, ImportType importType, PrintWriter cleanupLog) throws ImportException, IOException {

        String status = "";
        Long createdId = null;
        DatasetDTO dsDTO = null;
        try {
           
            dsDTO = importDDIService.doImport(importType, xmlToParse);
        } catch (XMLStreamException e) {
            throw new ImportException("XMLStreamException" + e);
        }
        // convert DTO to Json, 
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(dsDTO);
        JsonObject obj = JsonUtil.getJsonObject(json);
        //and call parse Json to read it into a dataset   
        try {
            JsonParser parser = new JsonParser(datasetfieldService, metadataBlockService, settingsService, licenseService);
            parser.setLenient(!importType.equals(ImportType.NEW));
            Dataset ds = parser.parseDataset(obj);

            // For ImportType.NEW, if the user supplies a global identifier, and it's not a protocol
            // we support, it will be rejected.
            if (importType.equals(ImportType.NEW)) {
                if (ds.getGlobalId().asString() != null && !ds.getProtocol().equals(settingsService.getValueForKey(SettingsServiceBean.Key.Protocol, ""))) {
                    throw new ImportException("Could not register id " + ds.getGlobalId().asString() + ", protocol not supported");
                }
            }

            ds.setOwner(owner);
            ds.getLatestVersion().setDatasetFields(ds.getLatestVersion().initDatasetFields());

            // Check data against required contraints
            List<ConstraintViolation<DatasetField>> violations = ds.getVersions().get(0).validateRequired();
            if (!violations.isEmpty()) {
                if ( importType.equals(ImportType.HARVEST) ) {
                    // For migration and harvest, add NA for missing required values
                    for (ConstraintViolation<DatasetField> v : violations) {
                        DatasetField f = v.getRootBean();
                         f.setSingleValue(DatasetField.NA_VALUE);
                    }
                } else {
                    // when importing a new dataset, the import will fail
                    // if required values are missing.
                    String errMsg = "Error importing data:";
                    for (ConstraintViolation<DatasetField> v : violations) {
                        errMsg += " " + v.getMessage();
                    }
                    throw new ImportException(errMsg);
                }
            }

            // Check data against validation constraints
            // If we are migrating and "scrub migration data" is true we attempt to fix invalid data
            // if the fix fails stop processing of this file by throwing exception
            Set<ConstraintViolation> invalidViolations = ds.getVersions().get(0).validate();
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            if (!invalidViolations.isEmpty()) {
                for (ConstraintViolation<DatasetFieldValue> v : invalidViolations) {
                    DatasetFieldValue f = v.getRootBean();
                    boolean fixed = false;
                    boolean converted = false;
                    if ( importType.equals(ImportType.HARVEST) && 
                         settingsService.isTrueForKey(SettingsServiceBean.Key.ScrubMigrationData, false)) {
                        fixed = processMigrationValidationError(f, cleanupLog, fileName);
                        converted = true;
                        if (fixed) {
                            Set<ConstraintViolation<DatasetFieldValue>> scrubbedViolations = validator.validate(f);
                            if (!scrubbedViolations.isEmpty()) {
                                fixed = false;
                            }
                        }
                    }
                    if (!fixed) {
                        if (importType.equals(ImportType.HARVEST)) {
                            String msg = "Data modified - File: " + fileName + "; Field: " + f.getDatasetField().getDatasetFieldType().getDisplayName() + "; "
                                    + "Invalid value:  '" + f.getValue() + "'" + " Converted Value:'" + DatasetField.NA_VALUE + "'";
                            cleanupLog.println(msg);
                            f.setValue(DatasetField.NA_VALUE);

                        } else {
                            String msg = " Validation error for ";
                            if (converted) {
                                msg += "converted ";
                            }
                            msg += "value: " + f.getValue() + ", " + f.getValidationMessage();
                            throw new ImportException(msg);
                        }
                    }
                }
            }


            Dataset existingDs = datasetService.findByGlobalId(ds.getGlobalId().asString());

            if (existingDs != null) {
                if (importType.equals(ImportType.HARVEST)) {
                    // For harvested datasets, there should always only be one version.
                    // We will replace the current version with the imported version.
                    if (existingDs.getVersions().size() != 1) {
                        throw new ImportException("Error importing Harvested Dataset, existing dataset has " + existingDs.getVersions().size() + " versions");
                    }
                    // harvested datasets don't have physical files - so no need to worry about that.
                    engineSvc.submit(new DestroyDatasetCommand(existingDs, dataverseRequest));
                    Dataset managedDs = engineSvc.submit(new CreateHarvestedDatasetCommand(ds, dataverseRequest));
                    status = " updated dataset, id=" + managedDs.getId() + ".";
                    
                } else {
                    // If we are adding a new version to an existing dataset,
                    // check that the version number isn't already in the dataset
                    for (DatasetVersion dsv : existingDs.getVersions()) {
                        if (dsv.getVersionNumber().equals(ds.getLatestVersion().getVersionNumber())) {
                            throw new ImportException("VersionNumber " + ds.getLatestVersion().getVersionNumber() + " already exists in dataset " + existingDs.getGlobalId().asString());
                        }
                    }
                    DatasetVersion dsv = engineSvc.submit(new CreateDatasetVersionCommand(dataverseRequest, existingDs, ds.getVersions().get(0)));
                    status = " created datasetVersion, for dataset "+ dsv.getDataset().getGlobalId().asString();
                    createdId = dsv.getId();
                }

            } else {
                Dataset managedDs = engineSvc.submit(new CreateNewDatasetCommand(ds, dataverseRequest));
                status = " created dataset, id=" + managedDs.getId() + ".";
                createdId = managedDs.getId();
            }

        } catch (JsonParseException ex) {
            logger.log(Level.INFO, "Error parsing datasetVersion: {0}", ex.getMessage());
            throw new ImportException("Error parsing datasetVersion: " + ex.getMessage(), ex);
        } catch (CommandException ex) {
            logger.log(Level.INFO, "Error excuting Create dataset command: {0}", ex.getMessage());
            throw new ImportException("Error excuting dataverse command: " + ex.getMessage(), ex);
        }
        return Json.createObjectBuilder().add("message", status);
    }
    
    private boolean processMigrationValidationError(DatasetFieldValue f, PrintWriter cleanupLog, String fileName) {
        if (f.getDatasetField().getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
            //Try to convert it based on the errors we've seen
            String convertedVal = convertInvalidEmail(f.getValue());
            if (!(convertedVal == null)) {
                String msg = "Data modified - File: " + fileName + "; Field: " + f.getDatasetField().getDatasetFieldType().getDisplayName() + "; "
                     + "Invalid value:  '" + f.getValue() + "'"   + " Converted Value:'" + convertedVal + "'"; 
                cleanupLog.println(msg);
                f.setValue(convertedVal);
                return true;
            }
            //if conversion fails set to NA
            String msg = "Data modified - File: " + fileName + "; Field: Dataset Contact Email; " +  "Invalid value: '" + f.getValue() + "'"  + " Converted Value: 'NA'"; 
            cleanupLog.println(msg);
            f.setValue(DatasetField.NA_VALUE);
            return true;
        }
        if (f.getDatasetField().getDatasetFieldType().getName().equals(DatasetFieldConstant.producerURL)) {
            if (f.getValue().equals("PRODUCER URL")) {
                String msg = "Data modified - File: " + fileName + "; Field: Producer URL; "  +  "Invalid value: '" + f.getValue() + "'"  + " Converted Value: 'NA'"; 
                cleanupLog.println(msg);
                f.setValue(DatasetField.NA_VALUE);
                return true;
            }
        }
        if (f.getDatasetField().getDatasetFieldType().getFieldType().equals(DatasetFieldType.FieldType.DATE)) {
            if(f.getValue().toUpperCase().equals("YYYY-MM-DD")){
                String msg = "Data modified - File: " + fileName + "; Field:" +  f.getDatasetField().getDatasetFieldType().getDisplayName() + "; "
                     +  "Invalid value: '" + f.getValue() + "'"  + " Converted Value: 'NA'"; 
                cleanupLog.println(msg);
                f.setValue(DatasetField.NA_VALUE);
                return true;
            }
            String convertedVal = convertInvalidDateString(f.getValue());
            if(!(convertedVal == null)) {
                String msg = "Data modified - File: " + fileName + "; Field: " +  f.getDatasetField().getDatasetFieldType().getDisplayName() + ""
                        + " Converted Value:" + convertedVal + "; Invalid value:  '" + f.getValue() + "'";
                cleanupLog.println(msg);
                f.setValue(convertedVal); 
                return true;
            }           
        }
        return false;
    }
    
    private String convertInvalidEmail(String inString){  
        //First we'll see if the invalid email is a comma delimited list of email addresses
        //if so we'll return the first one - maybe try to get them all at some point?
        if (inString.contains(",")){
            String[] addresses = inString.split("\\,"); 
            return addresses[0];
        }        

        //This works on the specific error we've seen where the user has put in a link for the email address
        //as in '<a href="IFPRI-Data@cgiar.org" > IFPRI-Data@cgiar.org</a>'
        //this returns the string between the first > and the second <
        if (inString.indexOf("<a", 0) > -1){
           try {
               String eMailAddress = inString.substring(inString.indexOf(">", 0) + 1, inString.indexOf("</a>", inString.indexOf(">", 0)));
               return eMailAddress.trim();               
           } catch (Exception e){
               return null;
           }          
        }
        return null;
    }
    
    private String convertInvalidDateString(String inString){
        
        //converts XXXX0000 to XXXX for date purposes
        if (inString.trim().length() == 8){
            if (inString.trim().endsWith("0000")){
                return inString.replace("0000", "").trim();
            }
        }
        
        //convert question marks to dashes and add brackets
        
        if (inString.contains("?")) {
            String testval = inString.replace("?", " ").replace("[", " ").replace("]", " ");
            if (StringUtils.isNumeric(testval.trim())) {
                switch (testval.trim().length()) {
                    case 1:
                        return "[" + testval.trim() + "---?]";
                    case 2:
                        return "[" + testval.trim() + "--?]";
                    case 3:
                        return "[" + testval.trim() + "-?]";
                    case 4:
                        return "[" + testval.trim() + "?]";
                    case 8:
                        if(testval.trim().contains("0000")){
                            return "[" + testval.trim().replace("0000", "") + "?]";
                        }
                }
            }
        }        
        
        //Convert string months to numeric    
        
        
        if (inString.toUpperCase().contains("JANUARY")){
            return inString.toUpperCase().replace("JANUARY", "").replace(",", "").trim() + "-01";            
        }
        
        if (inString.toUpperCase().contains("FEBRUARY")){
            return inString.toUpperCase().replace("FEBRUARY", "").replace(",", "").trim() + "-02";            
        }
        
        if (inString.toUpperCase().contains("MARCH")){
            return inString.toUpperCase().replace("MARCH", "").replace(",", "").trim() + "-03";            
        }
        
        if (inString.toUpperCase().contains("APRIL")){
            return inString.toUpperCase().replace("APRIL", "").replace(",", "").trim() + "-04";            
        }
        
        if (inString.toUpperCase().contains("MAY")){
            return inString.toUpperCase().replace("MAY", "").replace(",", "").trim() + "-05";            
        }
        
        if (inString.toUpperCase().contains("JUNE")){
            return inString.toUpperCase().replace("JUNE", "").replace(",", "").trim() + "-06";            
        }
        
        if (inString.toUpperCase().contains("JULY")){
            return inString.toUpperCase().replace("JULY", "").replace(",", "").trim() + "-07";            
        }
        
        if (inString.toUpperCase().contains("AUGUST")){
            return inString.toUpperCase().replace("AUGUST", "").replace(",", "").trim() + "-08";            
        }
        
        if (inString.toUpperCase().contains("SEPTEMBER")){
            return inString.toUpperCase().replace("SEPTEMBER", "").replace(",", "").trim() + "-09";            
        }
        
        if (inString.toUpperCase().contains("OCTOBER")){
            return inString.toUpperCase().replace("OCTOBER", "").replace(",", "").trim() + "-10";            
        }
        
        if (inString.toUpperCase().contains("NOVEMBER")){
            return inString.toUpperCase().replace("NOVEMBER", "").replace(",", "").trim() + "-11";            
        }
        
        if (inString.toUpperCase().contains("DECEMBER")){
            return inString.toUpperCase().replace("DECEMBER", "").replace(",", "").trim() + "-12";            
        }

        return null;
    }


    private static class MyCustomFormatter extends Formatter {

        @Override

        public String format(LogRecord record) {

            StringBuffer sb = new StringBuffer();

            sb.append("Prefixn");

            sb.append(record.getMessage());

            sb.append("Suffixn");

            sb.append("n");

            return sb.toString();

        }

    }

}
