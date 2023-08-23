package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

/**
 * EJB for kicking off big batch jobs asynchronously from the REST API  (BatchImport.java)
 * @author ellenk
 */
@Stateless
public class BatchServiceBean {
 private static final Logger logger = Logger.getLogger(BatchServiceBean.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    ImportServiceBean importService;
    

    @Asynchronous
    public void processFilePath(String fileDir, String parentIdtf, DataverseRequest dataverseRequest, Dataverse owner, ImportUtil.ImportType importType, Boolean createDV)  {
        logger.info("BEGIN IMPORT");
        PrintWriter validationLog = null;
        PrintWriter cleanupLog = null;
        try {
        JsonArrayBuilder status = Json.createArrayBuilder();
        Date timestamp = new Date();
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        
        validationLog = new PrintWriter(new FileWriter( "../logs/validationLog"+  formatter.format(timestamp)+".txt"));
        cleanupLog = new PrintWriter(new FileWriter( "../logs/cleanupLog"+  formatter.format(timestamp)+".txt"));
        File dir = new File(fileDir);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!file.isHidden()) {
                    if (file.isDirectory()) {
                        try {
                            status.add(handleDirectory(dataverseRequest, file, importType, validationLog, cleanupLog, createDV));
                        } catch (ImportException e) {
                            logger.log(Level.SEVERE, "Exception in handleDirectory() for "+ file.getName(),e);
                        }
                    } else {
                        try {
                            status.add(importService.handleFile(dataverseRequest, owner, file, importType, validationLog, cleanupLog));
                        } catch(ImportException e) {
                             logger.log(Level.SEVERE, "Exception in handleFile() for "+ file.getName(),e);
                        }

                    }
                }
            }
        } else {
            status.add(importService.handleFile(dataverseRequest, owner, dir, importType, validationLog, cleanupLog));

        }
        }
        catch(Exception e) {
                logger.log(Level.SEVERE, "Exception in processFilePath()", e);
        } finally {
            validationLog.close();
            cleanupLog.close();
        }
        logger.info("END IMPORT");

    }

    public JsonArrayBuilder handleDirectory(DataverseRequest dataverseRequest, File dir, ImportUtil.ImportType importType, PrintWriter validationLog, PrintWriter cleanupLog, Boolean createDV) throws ImportException{
        JsonArrayBuilder status = Json.createArrayBuilder();
        Dataverse owner = dataverseService.findByAlias(dir.getName());
        if (owner == null ) {
            if (createDV) {
                System.out.println("creating new dataverse: " + dir.getName());
                owner = importService.createDataverse(dir.getName(), dataverseRequest);
            } else {
                throw new ImportException("Can't find dataverse with identifier='" + dir.getName() + "'");
            }
        }
        for (File file : dir.listFiles()) {
            if (!file.isHidden()) {
                try {
                    JsonObjectBuilder fileStatus = importService.handleFile(dataverseRequest, owner, file, importType, validationLog, cleanupLog);
                    status.add(fileStatus);
                } catch (ImportException | IOException e) {
                    status.add(Json.createObjectBuilder().add("importStatus", "Exception importing " + file.getName() + ", message = " + e.getMessage()));
                }
            }
        }
        return status;
    }

}
