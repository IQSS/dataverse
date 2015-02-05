/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportUtil;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
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
    private static final Logger logger = Logger.getLogger(BatchImport.class.getCanonicalName());

    @Asynchronous
    public void processFilePath(String fileDir, String parentIdtf, User u, Dataverse owner, ImportUtil.ImportType importType) throws ImportException, IOException {
        JsonArrayBuilder status = Json.createArrayBuilder();
    
        File dir = new File(fileDir);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!file.isHidden()) {
                    if (file.isDirectory()) {
                        status.add(handleDirectory(u, file, importType));
                    } else {
                        status.add(importService.handleFile(u, owner, file, importType));
                       
                    }
                }
            }
        } else {
            status.add(importService.handleFile(u, owner, dir, importType));
          
        }
        logger.info("END IMPORT");

    }
    
     public JsonArrayBuilder handleDirectory(User u, File dir, ImportUtil.ImportType importType) throws ImportException, IOException {
        JsonArrayBuilder status = Json.createArrayBuilder();
        Dataverse owner = dataverseService.findByAlias(dir.getName());
        if (owner == null) {
            if (importType.equals(ImportUtil.ImportType.MIGRATION) || importType.equals(ImportUtil.ImportType.NEW)) {
                System.out.println("creating new dataverse: " + dir.getName());
                owner=importService.createDataverse(dir, u);
            } else {
                throw new ImportException("Can't find dataverse with identifier='" + dir.getName() + "'");
            }
        }
        for (File file : dir.listFiles()) {
            if (!file.isHidden()) {
                try {
                    JsonObjectBuilder fileStatus = importService.handleFile(u, owner, file, importType);
                    status.add(fileStatus);
                } catch (ImportException e) {
                    status.add(Json.createObjectBuilder().add("importStatus", "Exception importing " + file.getName() + ", message = " + e.getMessage()));
                }
            }
        }
        return status;
    }
 
}
