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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Anuj Thakur
 * @version 4.5 
 */
public class DataFileDownload {
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq."
            + "dataverse.harvest.client.datafiletransfer.DataFileDownload");
    
    /**
    @param fileURL a String variable for the data file url
    @param fileAbsolutePath a String variable for the Data File name and path 
           i.e it's absolute path
    @throws FileNotFoundException, IOException
    */
    String fileURL;
    String fileAbsolutePath;
    public long size = 0;
    
    public DataFileDownload(String fileURL , String fileAbsolutePath ) throws 
            FileNotFoundException, IOException{
        /**
         * The Url of the data file where it is initially present i.e on the 
         * harvesting server
         */
        this.fileURL = fileURL;
        /**
         * The absolute path where the file has to be saved.
         */
        this.fileAbsolutePath = fileAbsolutePath;
        /**
         * Used to measure the performance of this transfer mechanism.
         */
        this.size = 0;    
    }    
    
    /**
     * Downloads the data file from the fileURL to the fileAbsolutePath location on
     * the current system.
     * 
     * @param fileURL a String variable for the data file url
     * @param fileAbsolutePath a String variable for the data file name and path
     *        i.e it's absolute path
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void saveDataFile(String fileURL, String fileAbsolutePath) throws 
            MalformedURLException, FileNotFoundException, IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        //logger.log(Level.INFO, "Downloading file: "+fileURL);
        /*Path p = Paths.get(fileAbsolutePath.substring(0, fileAbsolutePath.lastIndexOf("/")));
        if(!Files.exists(p)) {
            Files.createDirectories(p);
        }*/
        try {
            //logger.log( Level.INFO , "Downloading file at URL: "+fileURL);
            in = new BufferedInputStream(new URL(fileURL).openStream());
            fout = new FileOutputStream(fileAbsolutePath);

            byte data[] = new byte[2048];
            int count;
            while ((count = in.read(data, 0, 2048)) != -1) {
                fout.write(data, 0, 2048);
                this.size = this.size + 2048;
            }
        }
        catch (MalformedURLException mue) {
            throw new MalformedURLException( "Malformed URL: "+fileURL);
        }
        catch (IOException ioe){
            throw new IOException("Unable to download file with URL: "
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
