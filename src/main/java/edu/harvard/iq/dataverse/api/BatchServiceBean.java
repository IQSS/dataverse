/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
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

/**
 * EJB for kicking off big batch jobs asynchronously from the REST API  (BatchImport.java)
 * @author ellenk
 */
@Stateless
public class BatchServiceBean {

    @EJB
    ImportServiceBean importService;
    private static final Logger logger = Logger.getLogger(BatchImport.class.getCanonicalName());

    @Asynchronous
    public void processFilePath(String fileDir, String parentIdtf, User u, Dataverse owner, ImportUtil.ImportType importType) throws ImportException, IOException {
        JsonArrayBuilder status = Json.createArrayBuilder();
        int count = 0;

        File dir = new File(fileDir);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!file.isHidden()) {
                    if (file.isDirectory()) {
                        status.add(importService.handleDirectory(u, file, importType));
                    } else {
                        status.add(importService.handleFile(u, owner, file, importType));
                        count++;
                    }
                }
            }
        } else {
            status.add(importService.handleFile(u, owner, dir, importType));
            count++;
        }
        logger.info("END IMPORT, processed " + count + "files.");

    }
}
