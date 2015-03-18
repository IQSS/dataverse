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
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ImportLogger;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author ellenk
 */
@Stateless
public class ImportServiceBean {

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
    
    @EJB ImportDDIServiceBean importDDIService;
    @EJB
    ImportLogger importLogger;

    @TransactionAttribute(REQUIRES_NEW)
    public Dataverse createDataverse(File dir, AuthenticatedUser u) throws ImportException {
        Dataverse d = new Dataverse();
        Dataverse root = dataverseService.findByAlias("root");
        d.setOwner(root);
        d.setAlias(dir.getName());
        d.setName(dir.getName());
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
            d = engineSvc.submit(new CreateDataverseCommand(d, u, null, null));
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append("Error creating dataverse.");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ").append(violation.getPropertyPath()).append(" at ").append(violation.getLeafBean()).append(" - ").append(violation.getMessage());
                    }
                }
            }
            importLogger.getLogger().log(Level.SEVERE, sb.toString());
            System.out.println("Error creating dataverse: " + sb.toString());
            throw new ImportException(sb.toString());
        } catch (Exception e) {
            throw new ImportException(e.getMessage());
        }
        return d;

    }

    @TransactionAttribute(REQUIRES_NEW)
    public JsonObjectBuilder handleFile(User u, Dataverse owner, File file, ImportType importType, PrintWriter validationLog) throws ImportException, IOException {

        System.out.println("handling file: " + file.getAbsolutePath());
        String ddiXMLToParse;
        try {
            ddiXMLToParse = new String(Files.readAllBytes(file.toPath()));
            JsonObjectBuilder status = doImport(u, owner, ddiXMLToParse, importType);
            status.add("file", file.getName());
            importLogger.getLogger().info("completed doImport " + file.getParentFile().getName() + "/" + file.getName());
            return status;
        } catch (ImportException ex) {
            String msg = "Import Exception processing file " + file.getParentFile().getName() + "/" + file.getName() + ", msg:" + ex.getMessage();
            importLogger.getLogger().info(msg);
            if (validationLog!=null) {
                validationLog.println(msg);
            }
            return Json.createObjectBuilder().add("message", "Import Exception processing file " + file.getParentFile().getName() + "/" + file.getName() + ", msg:" + ex.getMessage());
        } catch (Exception e) {
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

    public JsonObjectBuilder doImport(User u, Dataverse owner, String xmlToParse, ImportType importType) throws ImportException, IOException {

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
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject obj = jsonReader.readObject();
        //and call parse Json to read it into a dataset   
        try {
            JsonParser parser = new JsonParser(datasetfieldService, metadataBlockService, settingsService);
            parser.setLenient(!importType.equals(ImportType.NEW));
            Dataset ds = parser.parseDataset(obj);

            // For ImportType.NEW, if the user supplies a global identifier, and it's not a protocol
            // we support, it will be rejected.
            if (importType.equals(ImportType.NEW)) {
                if (ds.getGlobalId() != null && !ds.getProtocol().equals(settingsService.getValueForKey(SettingsServiceBean.Key.Protocol, ""))) {
                    throw new ImportException("Could not register id " + ds.getGlobalId() + ", protocol not supported");
                }
            }

            ds.setOwner(owner);
            ds.getLatestVersion().setDatasetFields(ds.getLatestVersion().initDatasetFields());

            // Check data against required contraints
            Set<ConstraintViolation> violations = ds.getVersions().get(0).validateRequired();
            if (!violations.isEmpty()) {
                if (importType.equals(ImportType.MIGRATION) || importType.equals(ImportType.HARVEST)) {
                    // For migration and harvest, add NA for missing required values
                    for (ConstraintViolation v : violations) {
                        DatasetField f = ((DatasetField) v.getRootBean());
                        f.setSingleValue(DatasetField.NA_VALUE);
                    }
                } else {
                    // when importing a new dataset, the import will fail
                    // if required values are missing.
                    String errMsg = "Error importing data:";
                    for (ConstraintViolation v : violations) {
                        errMsg += " " + v.getMessage();
                    }
                    throw new ImportException(errMsg);
                }
            }

            // Check data against validation constraints
            // If we are migrating and dataset contact email is invalid, then set it to NA
            // in all other cases, stop processing of this file by throwing exception
            Set<ConstraintViolation> invalidViolations = ds.getVersions().get(0).validate();
            if (!invalidViolations.isEmpty()) {
                for (ConstraintViolation v : invalidViolations) {
                    DatasetFieldValue f = ((DatasetFieldValue) v.getRootBean());
                    boolean fixed = false;
                    if (importType.equals(ImportType.MIGRATION)){
                        fixed = processMigrationValidationError(f);
                    }
                    if(!fixed){
                        String msg = " Validation error for value: " + f.getValue() + ", " + f.getValidationMessage();                       
                        throw new ImportException(msg); 
                    }                   
                }
            } 
            

            Dataset existingDs = datasetService.findByGlobalId(ds.getGlobalId());

            if (existingDs != null) {
                if (importType.equals(ImportType.HARVEST)) {
                    // For harvested datasets, there should always only be one version.
                    // We will replace the current version with the imported version.
                    if (existingDs.getVersions().size() != 1) {
                        throw new ImportException("Error importing Harvested Dataset, existing dataset has " + existingDs.getVersions().size() + " versions");
                    }
                    engineSvc.submit(new DestroyDatasetCommand(existingDs, u));
                    Dataset managedDs = engineSvc.submit(new CreateDatasetCommand(ds, u, false, importType));
                    status = " updated dataset, id=" + managedDs.getId() + ".";
                } else {
                    // If we are adding a new version to an existing dataset,
                    // check that the version number isn't already in the dataset
                    for (DatasetVersion dsv : existingDs.getVersions()) {
                        if (dsv.getVersionNumber().equals(ds.getLatestVersion().getVersionNumber())) {
                            throw new ImportException("VersionNumber " + ds.getLatestVersion().getVersionNumber() + " already exists in dataset " + existingDs.getGlobalId());
                        }
                    }
                    DatasetVersion dsv = engineSvc.submit(new CreateDatasetVersionCommand(u, existingDs, ds.getVersions().get(0)));
                    status = " created datasetVersion, for dataset "+ dsv.getDataset().getGlobalId();
                    createdId = dsv.getId();
                }

            } else {
                Dataset managedDs = engineSvc.submit(new CreateDatasetCommand(ds, u, false, importType));
                status = " created dataset, id=" + managedDs.getId() + ".";
                createdId = managedDs.getId();
            }

        } catch (JsonParseException ex) {

            importLogger.getLogger().info("Error parsing datasetVersion: " + ex.getMessage());
            throw new ImportException("Error parsing datasetVersion: " + ex.getMessage(), ex);
        } catch (CommandException ex) {
            importLogger.getLogger().info("Error excuting Create dataset command: " + ex.getMessage());
            throw new ImportException("Error excuting dataverse command: " + ex.getMessage(), ex);
        }
        return Json.createObjectBuilder().add("message", status);
    }
    
    private boolean processMigrationValidationError(DatasetFieldValue f) {
        if (f.getDatasetField().getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
            importLogger.getLogger().info("Dataset Contact email is invalid, setting to NA. Invalid value: " + f.getValue());
            f.setValue(DatasetField.NA_VALUE);
            return true;
        }
        if (f.getDatasetField().getDatasetFieldType().getName().equals(DatasetFieldConstant.producerURL)) {
            if (f.getValue().equals("PRODUCER URL")) {
                importLogger.getLogger().info("Producer URL is PRODUCER URL, setting to NA. Invalid value: " + f.getValue());
                f.setValue(DatasetField.NA_VALUE);
                return true;
            }
        }
        if (f.getDatasetField().getDatasetFieldType().getFieldType().equals(DatasetFieldType.FieldType.DATE)) {
            String convertedVal = convertInvalidDateString(f.getValue());
            if(!(convertedVal == null)) {
                f.setValue(convertedVal); 
                return true;
            }           
        }
        return false;
    }
    
    private String convertInvalidDateString(String inString){
        
        //converts XXXX0000 to XXXX for date purposes
        if (inString.trim().length() == 8){
            if (inString.trim().contains("0000")){
                return inString.trim().substring(0, 3);
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
