/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.imports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.dataset.DatasetTypeServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestedDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestedDatasetCommand;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ConstraintViolationUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.util.DatasetFieldUtil;

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

    @EJB
    DatasetTypeServiceBean datasetTypeService;

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
    public Dataset doImportHarvestedDataset(DataverseRequest dataverseRequest, 
            HarvestingClient harvestingClient, 
            String harvestIdentifier, 
            String metadataFormat, 
            File metadataFile, 
            Date oaiDateStamp, 
            PrintWriter cleanupLog) throws ImportException, IOException {
        
        logger.fine("importing " + metadataFormat + " saved in " + metadataFile.getAbsolutePath());
      
        //@todo? check for an IOException here, throw ImportException instead, if caught
        String metadataAsString = new String(Files.readAllBytes(metadataFile.toPath()));
        return doImportHarvestedDataset(dataverseRequest, harvestingClient, harvestIdentifier, metadataFormat, metadataAsString, oaiDateStamp, cleanupLog);
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Dataset doImportHarvestedDataset(DataverseRequest dataverseRequest, 
            HarvestingClient harvestingClient, 
            String harvestIdentifier, 
            String metadataFormat, 
            String metadataString, 
            Date oaiDateStamp, 
            PrintWriter cleanupLog) throws ImportException, IOException {
 
        if (harvestingClient == null || harvestingClient.getDataverse() == null) {
            throw new ImportException("importHarvestedDataset called with a null harvestingClient, or an invalid harvestingClient.");
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

        logger.fine("importing " + metadataFormat + " for " + harvestIdentifier);
        
        if ("ddi".equalsIgnoreCase(metadataFormat) || "oai_ddi".equals(metadataFormat) 
                || metadataFormat.toLowerCase().matches("^oai_ddi.*")) {
            try {
                // TODO: 
                // import type should be configurable - it should be possible to 
                // select whether you want to harvest with or without files, 
                // ImportType.HARVEST vs. ImportType.HARVEST_WITH_FILES
                dsDTO = importDDIService.doImport(ImportType.HARVEST, metadataString);
            } catch (XMLStreamException | ImportException e) {
                throw new ImportException("Failed to process DDI XML record: "+ e.getClass() + " (" + e.getMessage() + ")");
            }
        } else if ("dc".equalsIgnoreCase(metadataFormat) || "oai_dc".equals(metadataFormat)) {
            try {
                dsDTO = importGenericService.processOAIDCxml(metadataString, harvestIdentifier, harvestingClient.isUseOaiIdentifiersAsPids());
            } catch (XMLStreamException e) {
                throw new ImportException("Failed to process Dublin Core XML record: "+ e.getClass() + " (" + e.getMessage() + ")");
            }
        } else if ("dataverse_json".equals(metadataFormat)) {
            // This is Dataverse metadata already formatted in JSON. 
            json = metadataString; 
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
        
        String protocol = obj.getString("protocol", null);
        String authority = obj.getString("authority", null);
        String identifier = obj.getString("identifier",null);
        
        GlobalId globalId;
        
        // A Global ID is required:
        // (meaning, we will fail with an exception if the imports above have 
        // not managed to find an acceptable global identifier in the harvested 
        // metadata)
        
        try { 
            globalId = PidUtil.parseAsGlobalID(protocol, authority, identifier);
        } catch (IllegalArgumentException iax) {
            throw new ImportException("The harvested metadata record with the OAI server identifier " + harvestIdentifier + " does not contain a global identifier this Dataverse can parse, skipping.");
        }
        
        if (globalId == null) {
            throw new ImportException("The harvested metadata record with the OAI server identifier " + harvestIdentifier + " does not contain a global identifier this Dataverse recognizes, skipping.");
        }
        
        String globalIdString = globalId.asString();
        
        if (StringUtils.isEmpty(globalIdString)) {
            // @todo this check may not be necessary, now that there's a null check above
            throw new ImportException("The harvested metadata record with the OAI server identifier " + harvestIdentifier + " does not contain a global identifier this Dataverse recognizes, skipping.");
        }
        
        DatasetVersion harvestedVersion; 
        
        Dataset existingDataset = datasetService.findByGlobalId(globalIdString);
        
        try {
            Dataset harvestedDataset;

            JsonParser parser = new JsonParser(datasetfieldService, metadataBlockService, settingsService, licenseService, datasetTypeService, harvestingClient);
            parser.setLenient(true);

            if (existingDataset == null) {
                // Creating a new dataset from scratch:
                
                harvestedDataset = parser.parseDataset(obj);

                harvestedDataset.setHarvestedFrom(harvestingClient);
                harvestedDataset.setHarvestIdentifier(harvestIdentifier);
                
                harvestedVersion = harvestedDataset.getVersions().get(0);
            } else {
                // We already have a dataset with this id in the database.
                // Let's check a few things before we go any further with it: 
                
                // If this dataset already exists IN ANOTHER COLLECTION
                // we are just going to skip it!
                if (existingDataset.getOwner() != null && !owner.getId().equals(existingDataset.getOwner().getId())) {
                    throw new ImportException("The dataset with the global id " + globalIdString + " already exists, in the dataverse " + existingDataset.getOwner().getAlias() + ", skipping.");
                }
                // And if we already have a dataset with this same global id at 
                // this Dataverse instance, but it is a LOCAL dataset (can happen!), 
                // we're going to skip it also: 
                if (!existingDataset.isHarvested()) {
                    throw new ImportException("A LOCAL dataset with the global id " + globalIdString + " already exists in this dataverse; skipping.");
                }
                // For harvested datasets, there should always only be one version.
                if (existingDataset.getVersions().size() != 1) {
                    throw new ImportException("Error importing Harvested Dataset, existing dataset has " + existingDataset.getVersions().size() + " versions");
                }
                
                // We will attempt to import the new version, and replace the 
                // current, already existing version with it.                
                harvestedVersion = parser.parseDatasetVersion(obj.getJsonObject("datasetVersion"));
                
                // For the purposes of validation, the version needs to be attached
                // to a non-null dataset. We will create a throwaway temporary 
                // dataset for this:
                harvestedDataset = createTemporaryHarvestedDataset(harvestedVersion);
            }
        
            harvestedDataset.setOwner(owner);
            
            // Either a full new import, or an update of an existing harvested
            // Dataset, perform some cleanup on the new version imported from the 
            // parsed metadata:
            
            harvestedVersion.setDatasetFields(harvestedVersion.initDatasetFields());

            if (harvestedVersion.getReleaseTime() == null) {
                harvestedVersion.setReleaseTime(oaiDateStamp);
            }
                        
            // Check data against validation constraints. 
            // Make an attempt to sanitize any invalid fields encountered - 
            // missing required fields or invalid values, by filling the values 
            // with NAs.
            
            boolean sanitized = validateAndSanitizeVersionMetadata(harvestedVersion, cleanupLog);
            
            // Note: this sanitizing approach, of replacing invalid values with 
            // "NA" does not work with certain fields. For example, using it to 
            // populate a GeoBox coordinate value will result in an invalid 
            // field. So we will attempt to re-validate the santized version.
            // This time around, it will throw an exception if still invalid, so 
            // that we'll stop before proceeding any further: 
            
            if (sanitized) {
                validateVersionMetadata(harvestedVersion, cleanupLog);
            }
               
            DatasetFieldUtil.tidyUpFields(harvestedVersion.getDatasetFields(), true);

            if (existingDataset != null) {
                importedDataset = engineSvc.submit(new UpdateHarvestedDatasetCommand(existingDataset, harvestedVersion, dataverseRequest));
            } else {
                importedDataset = engineSvc.submit(new CreateHarvestedDatasetCommand(harvestedDataset, dataverseRequest));
            }

        } catch (JsonParseException | ImportException | CommandException ex) {
            logger.fine("Failed to import harvested dataset: " + ex.getClass() + ": " + ex.getMessage());
            
            if (!"dataverse_json".equals(metadataFormat) && json != null) {
                // If this was an xml format that were able to transform into 
                // our json, let's save it for debugging etc. purposes
                File tempFile = File.createTempFile("meta", ".json");
                FileOutputStream savedJsonFileStream = new FileOutputStream(tempFile);
                byte[] jsonBytes = json.getBytes();
                int i = 0;
                while (i < jsonBytes.length) {
                    int chunkSize = i + 8192 <= jsonBytes.length ? 8192 : jsonBytes.length - i;
                    savedJsonFileStream.write(jsonBytes, i, chunkSize);
                    i += chunkSize;
                    savedJsonFileStream.flush();
                }
                savedJsonFileStream.close();
                logger.info("JSON produced saved in " + tempFile.getAbsolutePath());
            }
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
            JsonParser parser = new JsonParser(datasetfieldService, metadataBlockService, settingsService, licenseService, datasetTypeService);
            parser.setLenient(!importType.equals(ImportType.NEW));
            Dataset ds = parser.parseDataset(obj);

            // For ImportType.NEW, if the user supplies a global identifier, and it's not a protocol
            // we support, it will be rejected.
            
            if (importType.equals(ImportType.NEW)) {
                if (ds.getGlobalId().asString() != null && !PidUtil.getPidProvider(ds.getGlobalId().getProviderId()).canManagePID()) {
                    throw new ImportException("Could not register id " + ds.getGlobalId().asString() + ", protocol not supported");
                }
            }

            ds.setOwner(owner);
            ds.getLatestVersion().setDatasetFields(ds.getLatestVersion().initDatasetFields());

            // Check data against required constraints
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

    /**
     * A shortcut method for validating AND attempting to sanitize a DatasetVersion
     * @param version
     * @param cleanupLog - any invalid values and their replacements are logged there
     * @return true if any invalid values were encountered and sanitized
     * @throws ImportException (although it should never happen in this mode)
     */
    private boolean validateAndSanitizeVersionMetadata(DatasetVersion version, PrintWriter cleanupLog) throws ImportException {
        return validateVersionMetadata(version, true, cleanupLog);
    }
    
    /**
     * A shortcut method for validating a DatasetVersion; will throw an exception 
     * if invalid, without attempting to sanitize the invalid values. 
     * @param version
     * @param log - will log the invalid fields encountered there 
     * @throws ImportException 
     */
    private void validateVersionMetadata(DatasetVersion version, PrintWriter log) throws ImportException {
        validateVersionMetadata(version, false, log);
    } 
    
    /**
     * Validate the metadata fields of a newly-created version, and depending on 
     * the "sanitize" flag supplied, may or may not attempt to sanitize the supplied
     * values by replacing them with "NA"s. 
     * @param version
     * @param sanitize - boolean indicating whether to attempt to fix invalid values
     * @param cleanupLog - to log any invalid values encountered will be logged
     * @return - true if any invalid values have been replaced  
     * @throws ImportException 
     */
    private boolean validateVersionMetadata(DatasetVersion version, boolean sanitize, PrintWriter cleanupLog) throws ImportException {
        boolean fixed = false;
        Set<ConstraintViolation> invalidViolations = version.validate();
        if (!invalidViolations.isEmpty()) {
            for (ConstraintViolation v : invalidViolations) {
                Object invalid = v.getRootBean();
                String msg = "";
                if (invalid instanceof DatasetField) {
                    DatasetField f = (DatasetField) invalid; 
                    
                    msg += "Missing required field: " + f.getDatasetFieldType().getDisplayName() + ";";                  
                    if (sanitize) {
                        msg += " populated with '" + DatasetField.NA_VALUE + "'";
                        f.setSingleValue(DatasetField.NA_VALUE);
                        fixed = true;
                    }
                } else if (invalid instanceof DatasetFieldValue) {
                    DatasetFieldValue fv = (DatasetFieldValue) invalid;
                    
                    msg += "Invalid metadata field: " + fv.getDatasetField().getDatasetFieldType().getDisplayName() + "; "
                            + "Invalid value:  '" + fv.getValue() + "'";
                    if (sanitize) {
                        msg += ", replaced with '" + DatasetField.NA_VALUE + "'";
                        fv.setValue(DatasetField.NA_VALUE);
                        fixed = true;
                    }
                } else {
                    // DatasetVersion.validate() can also produce constraint violations
                    // in TermsOfUse and FileMetadata classes. 
                    // We do not make any attempt to sanitize those.
                    if (invalid != null) {
                        msg += "Invalid " + invalid.getClass().getName() + ": " + v.getMessage();
                    }
                }
                cleanupLog.println(msg);

                // Note: "NA" does not work with certain fields. For example, 
                // using it to populate a GeoBox coordinate value is going 
                // to result in an invalid field. So we'll need to validate the 
                // version again after the first, sanitizing pass and see if it 
                // helped or not.
            }
            if (!sanitize) {
                throw new ImportException("Version was still failing validation after the first attempt to sanitize the invalid values.");
            }
        }
        return fixed;
    }

    /**
     * Helper method that creates a throwaway Harvested Dataset to temporarily 
     * attach the newly-harvested version to. We need this when, instead of 
     * importing a brand-new harvested dataset from scratch, we are planning to
     * attempt to update an already existing dataset harvested from the same 
     * archival location.
     * @param harvestedVersion - a newly created Version imported from harvested metadata
     * @return - a temporary dataset to which the new version has been attached
     */
    private Dataset createTemporaryHarvestedDataset(DatasetVersion harvestedVersion) {
        Dataset tempDataset = new Dataset();
        harvestedVersion.setDataset(tempDataset);
        tempDataset.setVersions(new ArrayList<>(1));
        tempDataset.getVersions().add(harvestedVersion);
        
        return tempDataset;
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
