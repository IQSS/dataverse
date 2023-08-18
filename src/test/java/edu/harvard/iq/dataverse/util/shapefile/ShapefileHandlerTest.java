/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.util.shapefile;


//import edu.harvard.iq.dataverse.util.ZipMaker;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import static edu.harvard.iq.dataverse.util.ShapefileHandler.SHP_XML_EXTENSION;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


// Dataverse/4.0/QA/4.0_files/ingest_4.0/dta/date_new_stata.dta
/**
 *
 * @author raprasad
 */
public class ShapefileHandlerTest {
    
    @TempDir
    Path tempFolder;
    
    public void msg(String s){
            System.out.println(s);
    }
    
    public void msgt(String s){
        msg("------------------------------------------------------------");
        msg(s);
        msg("------------------------------------------------------------");
    }

    
    
     private File createBlankFile(String filename) throws IOException {
        if (filename == null){
            return null;
        }
        return Files.createFile(tempFolder.resolve(filename)).toFile();
    }
    
    private FileInputStream createZipReturnFilestream(List<String> file_names, String zipfile_name) throws IOException{
        
        File zip_file_obj = this.createAndZipFiles(file_names, zipfile_name);
        if (zip_file_obj == null){
            return null;
        }
        
        FileInputStream file_input_stream = new FileInputStream(zip_file_obj);

        return file_input_stream;
        
    }
    
    /*
        Convenience class to create .zip file and return a FileInputStream
    
        @param List<String> file_names - List of filenames to add to .zip.  These names will be used to create 0 length files
        @param String zipfile_name - Name of .zip file to create
    */
    private File createAndZipFiles(List<String> file_names, String zipfile_name) throws IOException{
        if ((file_names == null)||(zipfile_name == null)){
            return null;
        }
        
        // Create blank files based on a list of file names
        //
        Collection<File> fileCollection = new ArrayList<>();
        for (String fname : file_names) {
           File file_obj = this.createBlankFile(fname);
           fileCollection.add(file_obj);
           //msg("File created: " + file_obj.getName());           
        }
        
        Path zip_file_obj = this.tempFolder.resolve(zipfile_name);
        ZipOutputStream zip_stream = new ZipOutputStream(new FileOutputStream(zip_file_obj.toFile()));

        // Iterate through File objects and add them to the ZipOutputStream
        for (File file_obj : fileCollection) {
             this.addToZipFile(file_obj.getName(), file_obj, zip_stream);
        }

        /* -----------------------------------
        Cleanup: Delete single files that were added to .zip
        ----------------------------------- */
        for (File file_obj : fileCollection) {
             file_obj.delete();
        }
        
        return zip_file_obj.toFile();
        
    } // end createAndZipFiles
    
    
    @Test
    public void testCreateZippedNonShapefile() throws IOException{
        msgt("(1) testCreateZippedNonShapefile");
                
        // Create files and put them in a .zip
        List<String> file_names = Arrays.asList("not-quite-a-shape.shp", "not-quite-a-shape.shx", "not-quite-a-shape.dbf", "not-quite-a-shape.pdf"); //, "prj");
        File zipfile_obj = createAndZipFiles(file_names, "not-quite-a-shape.zip");
        
        // Pass the .zip to the ShapefileHandler
        ShapefileHandler shp_handler = new ShapefileHandler(new FileInputStream(zipfile_obj));
        shp_handler.DEBUG= true;

        // Contains shapefile?
        assertFalse(shp_handler.containsShapefile());

        // get file_groups Map
        Map<String, List<String>> file_groups = shp_handler.getFileGroups();
        
        assertFalse(file_groups.isEmpty(), "verify that the dict is not empty");
        assertTrue(file_groups.containsKey("not-quite-a-shape"), "verify key existance");
        assertEquals(List.of("shp", "shx", "dbf", "pdf"), file_groups.get("not-quite-a-shape"), "verify value of key");
        
        // Delete .zip
        zipfile_obj.delete();
        
        msg("Passed!");
    }
    
    
        
    @Test
    public void testZippedTwoShapefiles() throws IOException{
        msgt("(2) testZippedTwoShapefiles");
                
        // Create files and put them in a .zip
        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.fbn", "shape1.fbx", // 1st shapefile
                                            "shape2.shp", "shape2.shx", "shape2.dbf", "shape2.prj",     // 2nd shapefile
                                            "shape2.txt", "shape2.pdf", "shape2",                  // single files, same basename as 2nd shapefile
                                            "README.MD", "shp_dictionary.xls", "notes"  ); //, "prj");                  // single files
        
        File zipfile_obj = createAndZipFiles(file_names, "two-shapes.zip");
        
        // Pass the .zip to the ShapefileHandler
        ShapefileHandler shp_handler = new ShapefileHandler(new FileInputStream(zipfile_obj));
        shp_handler.DEBUG= true;
        
        assertTrue(shp_handler.containsShapefile(), "verify shapefile existance");
        assertFalse(shp_handler.errorFound, "verify that no error was found");
        
        shp_handler.showFileGroups();
       // if (true){
         //   return ;
        //}
        // get file_groups Map
        Map<String, List<String>> file_groups = shp_handler.getFileGroups();
        
        assertFalse(file_groups.isEmpty(), "verify that the dict is not empty");

        // Verify the keys
        assertTrue(file_groups.containsKey("shape1"), "verify key existance of 'shape1'");
        assertTrue(file_groups.containsKey("shape2"), "verify key existance of 'shape2'");

        // Verify the values
        assertEquals(file_groups.get("shape1"), Arrays.asList("shp", "shx", "dbf", "prj", "fbn", "fbx"), "verify value of key 'shape1'");
        assertEquals(file_groups.get("shape2"), Arrays.asList("shp", "shx", "dbf", "prj", "txt", "pdf", ShapefileHandler.BLANK_EXTENSION), "verify value of key 'shape2'");
        
        // Rezip/Reorder the files
        File test_unzip_folder = Files.createDirectory(this.tempFolder.resolve("test_unzip")).toFile();
        //File test_unzip_folder = new File("/Users/rmp553/Desktop/blah");
        shp_handler.rezipShapefileSets(new FileInputStream(zipfile_obj), test_unzip_folder );
        
   
        // Does the re-ordering do what we wanted?
        List<String> rezipped_filenames = new ArrayList<>();
        rezipped_filenames.addAll(Arrays.asList(test_unzip_folder.list()));
        msg("rezipped_filenames: " + rezipped_filenames);
        List<String> expected_filenames = Arrays.asList("shape1.zip", "shape2.zip", "shape2.txt", "shape2.pdf", "shape2", "README.MD", "shp_dictionary.xls", "notes");
        
        assertTrue(rezipped_filenames.containsAll(expected_filenames), "verify that all files exist");
        
        // Delete .zip
        zipfile_obj.delete();
        
        msg("Passed!");
    }
    
    
    @Test
    public void testZippedShapefileWithExtraFiles() throws IOException{
        msgt("(3) testZippedShapefileWithExtraFiles");
                
        // Create files and put them in a .zip
        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.pdf", "shape1.cpg", "shape1." + SHP_XML_EXTENSION, "README.md", "shape_notes.txt"); 
        File zipfile_obj = createAndZipFiles(file_names, "shape-plus.zip");

        // Pass the .zip to the ShapefileHandler
        ShapefileHandler shp_handler = new ShapefileHandler(new FileInputStream(zipfile_obj));
        shp_handler.DEBUG= true;
        
        assertTrue(shp_handler.containsShapefile(), "verify shapefile existance");

        // get file_groups Map
        Map<String, List<String>> file_groups = shp_handler.getFileGroups();
        
        assertFalse(file_groups.isEmpty(), "verify that the dict is not empty");

        // Verify the keys
        assertTrue(file_groups.containsKey("shape1"), "verify key existance of 'shape1'");

        // Verify the values
        assertEquals(List.of("shp", "shx", "dbf", "prj", "pdf", "cpg", SHP_XML_EXTENSION), file_groups.get("shape1"), "verify value of key 'shape1'");
        assertEquals(List.of("md"), file_groups.get("README"), "verify value of key 'README'");
        assertEquals(List.of("txt"), file_groups.get("shape_notes"), "verify value of key 'shape_notes'");
        
        File unzip2Folder = Files.createDirectory(this.tempFolder.resolve("test_unzip2")).toFile();
        // Rezip/Reorder the files
        shp_handler.rezipShapefileSets(new FileInputStream(zipfile_obj), unzip2Folder);
        //shp_handler.rezipShapefileSets(new FileInputStream(zipfile_obj), new File("/Users/rmp553/Desktop/blah"));
        
   
        // Does the re-ordering do what we wanted?
        List<String> rezipped_filenames = new ArrayList<>();
        rezipped_filenames.addAll(Arrays.asList(unzip2Folder.list()));
        
        msg("rezipped_filenames: " + rezipped_filenames);
        List<String> expected_filenames = Arrays.asList("shape1.zip", "scratch-for-unzip-12345", "shape1.pdf", "README.md", "shape_notes.txt");
        
        assertTrue(expected_filenames.containsAll(rezipped_filenames), "verify that all files exist");
        
        // Delete .zip
        zipfile_obj.delete();
        
        msg("Passed!");
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

            //msg("File [" + fileName + "] added to .zip");

            return true;
	} // end: addToZipFile

}
    


