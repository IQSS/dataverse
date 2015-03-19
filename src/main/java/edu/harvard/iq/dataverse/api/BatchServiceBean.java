package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportUtil;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.ImportLogger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 * EJB for kicking off big batch jobs asynchronously from the REST API  (BatchImport.java)
 * @author ellenk
 */
@Stateless
public class BatchServiceBean {

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    ImportServiceBean importService;
    @EJB
    ImportLogger importLogger;

    @Asynchronous
    public void processFilePath(String fileDir, String parentIdtf, AuthenticatedUser u, Dataverse owner, ImportUtil.ImportType importType) throws ImportException, IOException {
        importLogger.getLogger().info("BEGIN IMPORT");
        JsonArrayBuilder status = Json.createArrayBuilder();
        Date timestamp = new Date();
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        
        PrintWriter validationLog = new PrintWriter(new FileWriter( "../logs/validationLog"+  formatter.format(timestamp)+".txt"));
       PrintWriter cleanupLog = new PrintWriter(new FileWriter( "../logs/cleanupLog"+  formatter.format(timestamp)+".txt"));
        File dir = new File(fileDir);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!file.isHidden()) {
                    if (file.isDirectory()) {
                        status.add(handleDirectory(u, file, importType, validationLog, cleanupLog));
                    } else {
                        status.add(importService.handleFile(u, owner, file, importType, validationLog, cleanupLog));

                    }
                }
            }
        } else {
            status.add(importService.handleFile(u, owner, dir, importType, validationLog, cleanupLog));

        }
        validationLog.close();
        cleanupLog.close();
        importLogger.getLogger().info("END IMPORT");

    }

    public JsonArrayBuilder handleDirectory(AuthenticatedUser u, File dir, ImportUtil.ImportType importType, PrintWriter validationLog, PrintWriter cleanupLog) throws ImportException{
        JsonArrayBuilder status = Json.createArrayBuilder();
        Dataverse owner = dataverseService.findByAlias(dir.getName());
        if (owner == null) {
            if (importType.equals(ImportUtil.ImportType.MIGRATION) || importType.equals(ImportUtil.ImportType.NEW)) {
                System.out.println("creating new dataverse: " + dir.getName());
                owner = importService.createDataverse(dir, u);
            } else {
                throw new ImportException("Can't find dataverse with identifier='" + dir.getName() + "'");
            }
        }
        for (File file : dir.listFiles()) {
            if (!file.isHidden()) {
                try {
                    JsonObjectBuilder fileStatus = importService.handleFile(u, owner, file, importType, validationLog, cleanupLog);
                    status.add(fileStatus);
                } catch (ImportException | IOException e) {
                    status.add(Json.createObjectBuilder().add("importStatus", "Exception importing " + file.getName() + ", message = " + e.getMessage()));
                }
            }
        }
        return status;
    }

}
