/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.util;


import edu.harvard.iq.dataverse.util.ZipMaker;

import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;

import java.nio.file.Files;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


// Dataverse/4.0/QA/4.0_files/ingest_4.0/dta/date_new_stata.dta
/**
 *
 * @author rmp553
 */
public class ShapefileHandlerTest {
    
        
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
 
    
  

    //@Test
    public void testWrite() throws IOException {
    
    // Create a temporary file.
    // This is guaranteed to be deleted after the test finishes.
    final File tempFile = tempFolder.newFile("myfile.txt");
    
    // Write something to it.
    FileUtils.writeStringToFile(tempFile, "hello world");
    
    // Read it.
    //final String s = FileUtils.readFileToString(tempFile);
 
    // Check that what was written is correct.
    String fcontents = FileUtils.readFileToString(tempFile);
    System.out.println(fcontents);
    System.out.println("-----------   ShapefileHandlerTest  -----------");

    assertEquals(fcontents, "hello world");
   // assertThat("hello world", is(s));
  }
    
     private File createBlankFile(String filename) throws IOException {
        if (filename == null){
            return null;
        }
        File aFile = this.tempFolder.newFile(filename);
        aFile.createNewFile();
        return aFile;
    }
    

    @Test
    public void testCreateZippedShapefile() throws IOException {
    
        System.out.println("-----------   testCreateZippedShapefile  -----------");

        // Create four files 
        String file_basename = "income_areas";
        List<String> filenames = Arrays.asList("shp", "shx", "dbf", "prj");
        
        Collection<File> fileCollection = new ArrayList<File>();
        for (String ext_name : filenames) {
           //System.out.println("ext: " + ext_name);
           File shpPart = this.createBlankFile(file_basename + "." +  ext_name);
           fileCollection.add(shpPart);
           System.out.println("File created: " + shpPart.getName());
           
        }
        
        
        /* -----------------------------------
           Add the files to a .zip
        ----------------------------------- */

        // create a  ZipOutputStream
        String zippedShapefileName = file_basename + ".zip";
        ZipOutputStream zip_stream = new ZipOutputStream(new FileOutputStream(this.tempFolder.newFile(zippedShapefileName)));
        System.out.println("\nCreate zipped shapefile: " + zippedShapefileName);
        // Iterate through File objects and add them to the ZipOutputStream
        for (File file_obj : fileCollection) {
             this.addToZipFile(file_obj.getName(), file_obj, zip_stream);
        }

        
        
  //  assertEquals(fcontents, "");
  }
    
       private boolean addToZipFile(String fileName, File fileToZip, ZipOutputStream zip_output_stream) throws FileNotFoundException, IOException {

            //File file = new File(fullFilepath);
            FileInputStream file_input_stream = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileName);
            zip_output_stream.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = file_input_stream.read(bytes)) >= 0) {
                    zip_output_stream.write(bytes, 0, length);
            }

            zip_output_stream.closeEntry();
            file_input_stream.close();

            System.out.println("File [" + fileName + "] added to .zip");

            return true;
	} // end: addToZipFile

}
    


