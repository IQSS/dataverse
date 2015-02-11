/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportDDI;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportUtil;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.xml.stream.XMLStreamException;

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
    private static final Logger logger = Logger.getLogger(ImportServiceBean.class.getCanonicalName());
  
    @TransactionAttribute(REQUIRES_NEW)
   public Dataverse createDataverse(File dir, User u) throws ImportException {
            Dataverse d = new Dataverse();
                Dataverse root = dataverseService.findByAlias("root");
                d.setOwner(root);
                d.setAlias(dir.getName());
                d.setName(dir.getName());
                d.setAffiliation("affiliation");
                d.setPermissionRoot(false);
                d.setDescription("description");
                d.setDataverseType(Dataverse.DataverseType.RESEARCHERS);
                List<ControlledVocabularyValue> cvList = new ArrayList<>();
                cvList.add(datasetfieldService.findNAControlledVocabularyValue());
                d.setDataverseSubjects(cvList);
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
                    logger.log(Level.SEVERE, sb.toString());
                    System.out.println("Error creating dataverse: " + sb.toString());
                    throw new ImportException(sb.toString());
                } catch (Exception e) {
                    throw new ImportException(e.getMessage());
                }
                return d;

   }
   
  
  
  
  
    @TransactionAttribute(REQUIRES_NEW)
    public JsonObjectBuilder handleFile(User u, Dataverse owner, File file, ImportType importType) throws ImportException, IOException {
        System.out.println("handling file: " + file.getAbsolutePath());
        String ddiXMLToParse;
        try {
            ddiXMLToParse = new String(Files.readAllBytes(file.toPath()));
            JsonObjectBuilder status = doImport(u, owner, ddiXMLToParse, importType);
            status.add("file", file.getName());
            logger.info("completed doImport " + file.getParentFile().getName() + "/" + file.getName());
            return status;
      //  } catch (IOException e) {
            //      e.printStackTrace();
            //       logger.info("Error reading file " + file.getAbsolutePath()+"msg = " + e.getMessage());
            //       throw new ImportException("Error reading file " + file.getAbsolutePath()+"msg = " + e.getMessage(), e);
        } catch (ImportException ex) {
            logger.info("Import Exception processing file " + file.getParentFile().getName() + "/" + file.getName() + ", msg:" + ex.getMessage());
            return Json.createObjectBuilder().add("message", "Import Exception processing file " + file.getParentFile().getName() + "/" + file.getName() + ", msg:" + ex.getMessage());
        } catch (Exception e) {
            logger.info("Unexpected Error processing file " + file.getParentFile().getName() + "/" + file.getName() + ", msg:" + e.getMessage());
            String msg = "Unexpected Error in handleFile(), file:" + file.getParentFile().getName() + "/" + file.getName();
            e.printStackTrace();
            logger.severe(msg);
            throw new ImportException("Unexpected Error in handleFile(), file:" + file.getParentFile().getName() + "/" + file.getName(), e);

        }
    }
     public JsonObjectBuilder doImport(User u, Dataverse owner, String xmlToParse, ImportType importType) throws ImportException {
      
        String status = "";
        Long createdId = null;
        DatasetDTO dsDTO = null;
        try {            
            ImportDDI importDDI = new ImportDDI(importType);
            dsDTO = importDDI.doImport(xmlToParse);
        } catch (XMLStreamException e) {
            throw new ImportException("XMLStreamException" +  e);
        }
        // convert DTO to Json, 
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(dsDTO);
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject obj = jsonReader.readObject();
        //and call parse Json to read it into a dataset   
        try {
            Dataset ds = new JsonParser(datasetfieldService, metadataBlockService, settingsService).parseDataset(obj);
            ds.setOwner(owner);
            ds.getLatestVersion().setDatasetFields(ds.getLatestVersion().initDatasetFields());

            // If there are missing values in Required fields, fill them with "N/A"
            if (importType.equals(ImportType.MIGRATION) || importType.equals(ImportType.HARVEST)) {
                Set<ConstraintViolation> violations = ds.getVersions().get(0).validateRequired();
                if (!violations.isEmpty()) {
                    for (ConstraintViolation v : violations) {
                        DatasetField f = ((DatasetField) v.getRootBean());
                        f.setSingleValue(DatasetField.NA_VALUE);
                    }
                }
            }
            Dataset existingDs = datasetService.findByGlobalId(ds.getGlobalId());

            if (existingDs != null) {
                if (importType.equals(ImportType.HARVEST)) {
                    // For harvested datasets, there should always only be one version.
                    // We will replace the current version with the imported version.
                    if (existingDs.getVersions().size()!=1) {
                        throw new ImportException("Error importing Harvested Dataset, existing dataset has "+ existingDs.getVersions().size() + " versions");
                    }
                    engineSvc.submit(new DestroyDatasetCommand(existingDs,u));
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
                status = " created datasetVersion, id=" + dsv.getId() + ".";       
                createdId = dsv.getId();
                }
               
            } else {
                Dataset managedDs = engineSvc.submit(new CreateDatasetCommand(ds, u, false, importType));
                status = " created dataset, id=" + managedDs.getId() + ".";
                createdId = managedDs.getId();
            }

        } catch (JsonParseException ex) {
            
            logger.info("Error parsing datasetVersion: " + ex.getMessage());
            throw new ImportException("Error parsing datasetVersion: " + ex.getMessage(), ex);
        } catch(CommandException ex) {  
            logger.info("Error excuting Create dataset command: " + ex.getMessage());
            throw new ImportException("Error excuting dataverse command: " + ex.getMessage(), ex);
        }
        return Json.createObjectBuilder().add("message", status).add("id", createdId);
     }
     
}
