/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client.datafiletransfer;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anuj
 */
public class DataFileDownload {
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq."
            + "dataverse.harvest.client.datafiletransfer.DataFileDownload");
    
    /*
    @param fileURL a String variable for the Data File Url
    @param fileAbsolutePath a String variable for the Data File name and path 
           i.e it's absolute path
    @throws FileNotFoundException, IOException
    */
    public DataFileDownload(String fileURL , String fileAbsolutePath ) throws 
            FileNotFoundException, IOException{
        this.saveDataFile(fileURL , fileAbsolutePath);      
    }    
    
    public void saveDataFile(String fileURL, String fileAbsolutePath) throws 
            MalformedURLException, FileNotFoundException, IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            logger.log( Level.INFO , "Downloading file at URL: "+fileURL);
            in = new BufferedInputStream(new URL(fileURL).openStream());
            fout = new FileOutputStream(fileAbsolutePath);

            byte data[] = new byte[2048];
            int count;
            while ((count = in.read(data, 0, 2048)) != -1) {
                fout.write(data, 0, count);
            }
        }
        catch (IOException ioe){
            logger.log( Level.WARNING , "Unable to download file with URL: "
                    +fileURL);
            
        }
        finally {
            if (in != null)
            in.close();
            if (fout != null)
                fout.close();
        }
    }
}
