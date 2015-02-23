/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.ejb.Stateless;

/**
 *
 * @author ellenk
 */
@Stateless
public class ImportLogger
         {
    private Logger logger;
    public ImportLogger() throws SecurityException {
        super();
    }
    
    public Logger getLogger()  {
        if (logger==null) {
           
            logger = Logger.getLogger("edu.harvard.iq.dataverse");
            try {
           FileHandler myFileHandler = new FileHandler("../logs/import-log.%u.%g.txt",true);
            myFileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(myFileHandler);   
            } catch(IOException ex) {
                throw new RuntimeException("Error opening ImportLogger fileHandler",ex);
            }
       
        }
        return logger;
    }
}


