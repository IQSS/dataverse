package edu.harvard.iq.dataverse.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
//import java.util.zip.ZipOutputStream;
import java.util.zip.ZipException;
import java.util.HashMap;
import java.util.*;

import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
//import java.nio.file.Path;

/**
 *  Used to identify, "repackage", and extract data from Shapefiles in .zip format
 *
 *  (1) Identify if a .zip contains a shapefile: 
 *          boolean containsShapefile(FileInputStream zip_stream) or boolean containsShapefile(FileInputStream zip_filename) 
 *
 *
 * 
 *  (2) Unpack/"Repackage" .zip:
 *          (a) All files extracted
 *          (b) Each group of files that make up a shapefile are made into individual .zip files
 *          (c) Non shapefile-related files left on their own
 *
 *      If the original .zip contains:  "shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.ain",  "shape1.aih",
 *                                      "shape2.shp", "shape2.shx", "shape2.dbf", "shape2.prj",
 *                                      "shape1.pdf", "README.md", "shape_notes.txt"
 *      The repackaging results in a folder containing: 
 *                                  "shape1.zip", 
 *                                  "shape2.zip",
 *                                  "shape1.pdf", "README.md", "shape_notes.txt"
 * 
 *  Code Example:  
 *          FileInputStream shp_file_input_stream = new FileInputStream(new File("zipped_shapefile.zip"))
 *          ShapefileHandler shp_handler = new ShapefileHandler(shp_file_input_stream);
 *          if (shp_handler.containsShapefile()){
 *              File rezip_folder = new File("~/folder_for_rezipping");
 *              boolean rezip_success = shp_handler.rezipShapefileSets(shp_file_input_stream, rezip_folder );
 *              if (!rezip_success){
 *                  // rezip failed, should be an error message (String) available
                    System.out.println(shp_handler.error_message);
 *              }
 *          }else{              
 *              if (shp_handler.errorFound){
 *                  System.out.println("Error message: " + shp_handler.error_message;
 *              }
 *          }
 *         
 *
 * @author Raman Prasad
 * 
 */
public class ShapefileHandler{

    // Reference for these extensions: http://en.wikipedia.org/wiki/Shapefile
    public final static List<String> SHAPEFILE_MANDATORY_EXTENSIONS = Arrays.asList("shp", "shx", "dbf", "prj");
    public final static String SHP_XML_EXTENSION = "shp.xml";
    public final static String BLANK_EXTENSION = "__PLACEHOLDER-FOR-BLANK-EXTENSION__";
    public final static List<String> SHAPEFILE_ALL_EXTENSIONS = Arrays.asList("shp", "shx", "dbf", "prj", "sbn", "sbx", "fbn", "fbx", "ain", "aih", "ixs", "mxs", "atx", ".cpg", SHP_XML_EXTENSION);  
    
    public boolean DEBUG = false;
        
    private boolean zipFileProcessed = false;
    public boolean errorFound = false;
    public String errorMessage = new String();
    
    // List of files in .zip archive
    private List<String> filesListInDir = new ArrayList<>();

    // Hash of file names and byte sizes {  "file name" : bytes }  example: { "water.shp" : 541234 }
    private HashMap<String, Long> filesizeHash = new HashMap<>();   
    
    // Hash of file basenames and a list of extensions. 
    /*   e.g.  { "subway_shapefile" : [ ".dbf", ".prj", ".sbn", ".sbx", ".shp", ".shx"] 
               , "shapefile_info" : [".docx"]
               , "README" : ["md"]
               , "Notes" : [""]
              }
    */
    private Map<String, List<String>> fileGroups = new HashMap<>();
    
    private String outputFolder = "unzipped";
    private String rezippedFolder = "rezipped";

    // Debug helper
    private void msg(String s){
        if (DEBUG){
            System.out.println(s);
        }
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }

    /*
        Constructor, start with filename
    */
    public ShapefileHandler(String filename){

        if (filename==null){
            this.addErrorMessage("The filename was null");
            return;
        }
        
        FileInputStream zip_file_stream;
        try {
            zip_file_stream = new FileInputStream(new File(filename));
        } catch (FileNotFoundException ex) {
            this.addErrorMessage("The file was not found");
            return;
        }
        
       this.examineZipfile(zip_file_stream);

    }

   
   /*
        Constructor, start with FileInputStream
    */
    public ShapefileHandler(FileInputStream zip_file_stream){

        if (zip_file_stream==null){
            this.addErrorMessage("The zip_file_stream was null");
            return;
        }
        this.examineZipfile(zip_file_stream);
    }
    
    private void addErrorMessage(String m){
        if (m == null){
            return;
        }
        this.errorFound = true;
        this.errorMessage = m;
    }
    /*
        Create a directory, if one doesn"t exist
    */
    private boolean createDirectory(String fname){
        if (fname == null){
            return false;
        }
      	File folder_obj = new File(fname);
        msg("ShapefileHandler. Folder created: " + folder_obj.getAbsolutePath());
      	return createDirectory(folder_obj);
      	
    } // createDirectory
    
    private boolean createDirectory(File folder){
        if (folder == null){
            return false;
        }
        try{
          	if(!folder.exists()){
          	    msg("Creating folder: " + folder.getName());
          		folder.mkdirs();	    
          	}else{
          	    msg("Folder exists: " + folder.getName());
          	}
         }catch(SecurityException ex){
           this.addErrorMessage("Tried to create directory but resulted in SecurityException");
           return false;
        }catch(NullPointerException ex){
            this.addErrorMessage("Tried to create directory but resulted in NullPointerException");

            return false;
        }
        return true;
    } // createDirectory    

    
    /*
        Print out the key/value pairs of the Hash of filenames and sizes
    */
    private void showFileNamesSizes(){
        msgt("Hash: file names + sizes");
        Iterator<String> keySetIterator = this.filesizeHash.keySet().iterator();

        while(keySetIterator.hasNext()){
          String key = keySetIterator.next();
          msg("key: [" + key + "] value: [" + this.filesizeHash.get(key)+"]");
          
        }
    } // end showFileNamesSizes
    
    
    public Map getFileGroups(){
        return this.fileGroups;
    }

    /*
        Iterate through Hash of file base names and extensions
    */
    public void showFileGroups(){

        msgt("Hash: file base names + extensions");
        
        for (Map.Entry<String, List<String>> entry : fileGroups.entrySet()){
            msg("\nKey: [" + entry.getKey() + "] Ext List: " + entry.getValue());
            if (doesListContainShapefileExtensions(entry.getValue())){
                msg(" >>>> YES, This is a shapefile!");
            }else{
                msg(" >>>> Not a shapefile");
            }
        }
       
    } // end showFileGroups
    
    /*
        Return a count of shapefile sets in this .zip
    */
    public int getShapefileCount(){
        int shp_cnt = 0;
        
        for (Map.Entry<String, List<String>> entry : fileGroups.entrySet()){
            if (doesListContainShapefileExtensions(entry.getValue())){
                shp_cnt+=1;
            }
        }
        return shp_cnt;
    }
    
    
    private boolean deleteDirectory(String dirname){
        
        if (dirname==null){
            return false;
        }
        File dir_obj = new File(dirname);
        if (!(dir_obj.exists())){
            return true;
        }
       File[] entries = dir_obj.listFiles();
       msgt("deleteDirectory");
       if (entries==null){
           return true;
       }
       for(File f: entries){
          f.delete();
       }
       dir_obj.delete();
       return true;
        
    }
    
    private boolean unzipFilesToDirectory(FileInputStream zipfile_input_stream, File target_directory) throws IOException{
        if (zipfile_input_stream== null){
            this.addErrorMessage("unzipFilesToDirectory. The zipfile_input_stream is null.");
            return false;
        }
        if (!target_directory.isDirectory()){
             this.addErrorMessage("This directory does not exist: " + target_directory.getAbsolutePath());
            return false;
        }
        
       ZipInputStream zip_stream = new ZipInputStream(zipfile_input_stream);
              
        ZipEntry orig_entry;
        byte[] buffer = new byte[2048];
        while((orig_entry = zip_stream.getNextEntry())!=null){

            String zentry_file_name = orig_entry.getName();

            // Skip files or folders starting with __
            if (zentry_file_name.startsWith("__")){
                continue;
            }

            // Create sub directory, if needed
            if (orig_entry.isDirectory()) {
                String dirpath = target_directory.getAbsolutePath() + "/" + zentry_file_name;
                createDirectory(dirpath);
                continue;           // Continue to next Entry
            }
            // Write the file
            String outpath = target_directory.getAbsolutePath() + "/" + zentry_file_name;
            //msg("Write zip file" + outpath);
            FileOutputStream output;
            long fsize = 0;
            output = new FileOutputStream(outpath);
            int len;// = 0;
            while ((len = zip_stream.read(buffer)) > 0){
                output.write(buffer, 0, len);
                fsize+=len;
            } // end while
            output.close();
        } // end outer while
    
    return true;
    }
    /*
        Rezip the shapefile(s) into a given directory
        Assumes that the zipfile_input_stream has already been checked!
    */
    public boolean rezipShapefileSets(FileInputStream zipfile_input_stream, File rezippedFolder) throws IOException{
        
        //msgt("rezipShapefileSets");
        if (!this.zipFileProcessed){
             this.addErrorMessage("First use 'examineZipFile' (called in the constructor)");
            return false;
        }
        if (!this.containsShapefile()){
             this.addErrorMessage("There are no shapefiles here!");
            return false;
        }
        if (zipfile_input_stream== null){
            this.addErrorMessage("The zipfile_input_stream is null.");
            return false;
        }
        if (rezippedFolder == null){
            this.addErrorMessage("The rezippedFolder is null.");
            return false;
        }

        if (!rezippedFolder.isDirectory()){
            this.addErrorMessage("The rezippedFolder does not exist: " + rezippedFolder.getAbsolutePath());
            return false;
        }
        if (!containsShapefile()){
            msgt("There are no shapefiles to re-zip");
            return false;
        }
        
        // Create target directory for unzipping files
        String dirname_for_unzipping;
        File dir_for_unzipping;
        
        dirname_for_unzipping = rezippedFolder.getAbsolutePath() + "/" + "scratch-for-unzip-12345";
        dir_for_unzipping = new File(dirname_for_unzipping);
        dir_for_unzipping.mkdirs();

        //for (String elem_name : dir_for_unzipping.list()){
        //    msg("dir contents: " + elem_name);
        //}
        
        // Unzip files!
        if (!this.unzipFilesToDirectory(zipfile_input_stream, dir_for_unzipping)){
            this.addErrorMessage("Failed to unzip files.");
            return false;
        }
      
        // Redistribute files!
        String target_dirname = rezippedFolder.getAbsolutePath();
        boolean redistribute_success = this.redistributeFilesFromZip(dirname_for_unzipping, target_dirname);
     
        // Delete unzipped files in scratch directory
        FileUtils.deleteDirectory(dir_for_unzipping);
        
        
        return redistribute_success;
            
    }
    
    private String getRedistributeFilePath(String dirname, String file_basename, String file_ext){
        
        if (dirname==null){
            this.addErrorMessage("getRedistributeFilePath. dirname is null");
            return null;
        }
        if (file_basename==null){
            this.addErrorMessage("getRedistributeFilePath. file_basename is null");
            return null;
        }
        if (file_ext==null){
            this.addErrorMessage("getRedistributeFilePath. file_ext is null");
            return null;
        }
        if (file_ext == BLANK_EXTENSION ){
            return dirname + "/" + file_basename;
        }
        return dirname + "/" + file_basename + "." + file_ext;
    }
    
    /*
        Create new zipped shapefile
    
    
    */
    private boolean redistributeFilesFromZip(String source_dirname, String target_dirname){
      
        int cnt =0;
       /* START: Redistribute files by iterating through the Map of basenames + extensions
        
        example key: "shape1"
        example ext_list: ["shp", "shx", "dbf", "prj"]
       */
       for (Map.Entry<String, List<String>> entry : fileGroups.entrySet()){
            cnt++;
            String key = entry.getKey();
            List<String> ext_list = entry.getValue();

            msg("\n(" + cnt + ") Basename: " + key);
            msg("Extensions: " + Arrays.toString(ext_list.toArray()));
            
            // Is this a shapefile?  If so, rezip it
            if (doesListContainShapefileExtensions(ext_list)){
    
                List<String> namesToZip = new ArrayList<>();
                
                for (String ext_name : ext_list) {
                    if (!this.isShapefileExtension(ext_name)){
                        // Another file with similar basename as shapefile.  
                        // e.g. if shapefile basename is "census", this might be "census.xls", "census.pdf", or another non-shapefile extension
                        String source_file_fullpath = this.getRedistributeFilePath(source_dirname, key, ext_name);
                        String target_file_fullpath = this.getRedistributeFilePath(target_dirname, key, ext_name);
                        this.straightFileCopy(source_file_fullpath, target_file_fullpath);
                    }else{
                        namesToZip.add(key + "." + ext_name);
                
                    }
                }
            
                String target_zipfile_name = target_dirname + "/" + key + ".zip";
                //this.msg("target_zipfile_name: "+ target_zipfile_name);
                //this.msg("source_dirname: "+ source_dirname);
                
                //msgt("create zipped shapefile");
                ZipMaker zip_maker = new ZipMaker(namesToZip, source_dirname, target_zipfile_name);
                // rezip it
                                
            }else{
                // Non-shapefiles
                for (String ext_name : ext_list) {
                    String source_file_fullpath = this.getRedistributeFilePath(source_dirname, key, ext_name);
                    String target_file_fullpath = this.getRedistributeFilePath(target_dirname, key, ext_name);
                    this.straightFileCopy(source_file_fullpath, target_file_fullpath);
                }
            }
        }
       
       // END: Redistribute files
       
        return true;
    }  // end: redistributeFilesFromZip
    
    
    private boolean straightFileCopy(String sourceFileName, String targetFileName){
        
        //msg("Copy [" + sourceFileName + " to [" + targetFileName + "]");
        if ((sourceFileName == null)||(targetFileName==null)){
            this.addErrorMessage("The source or target file was null.\nSource: " + sourceFileName +"\nTarget: " + targetFileName);
            return false;
        }
        
        File source_file = new File(sourceFileName);
        File target_file = new File(targetFileName);
        try {
            Files.copy(source_file.toPath(), target_file.toPath(), REPLACE_EXISTING);    
        } catch (IOException ex) {
            this.addErrorMessage("Failed to copy file. IOException\nSource: " +  sourceFileName +"\nTarget: " + targetFileName);
            return false;
        }
       
        return true;
        
    }
  
    public boolean containsOnlySingleShapefile(){
        if (containsShapefile()){
            if (fileGroups.size()==filesizeHash.size()){
                return true;
            }
        }
        return false;
    }
    
    /*
        Does this zip file contain a shapefile set?
    */
    public boolean containsShapefile(){
        for (Map.Entry<String, List<String>> entry : fileGroups.entrySet()){
            String key = entry.getKey();
            List<String> ext_list = entry.getValue();
            if (doesListContainShapefileExtensions(ext_list)){
                return true;
            }
        }
              
        return false;
    }
    
    private boolean isShapefileExtension(String ext_name){
        if (ext_name == null){
            return false;
        }
        return SHAPEFILE_ALL_EXTENSIONS.contains(ext_name);
    }
    /*
        Does a list of file extensions match those required for a shapefile set?
    */
    private boolean doesListContainShapefileExtensions(List<String> ext_list){
        if (ext_list == null){
            return false;
        }
        return ext_list.containsAll(SHAPEFILE_MANDATORY_EXTENSIONS);
    }
    
    
    private void addToFileGroupHash(String basename, String ext){
        if ((basename==null)||(ext==null)){
            return;
        }
        List<String> extension_list = fileGroups.get(basename);
        if (extension_list==null) {
            extension_list = new ArrayList<>();
        }
        extension_list.add(ext);
        fileGroups.put(basename, extension_list);
    }   // end addToFileGroupHash
    
    /**
     * Update the fileGroup hash which contains a { base_filename : [ext1, ext2, etc ]}
     * This is used to determine whether a .zip contains a shapefile set
     #
     * @param fname filename in String format
     */
    private void updateFileGroupHash(String fname){
        if (fname == null){
            return;
        }
             
        // Split filename into basename and extension.  No extension yields only basename
        //
        if (fname.toLowerCase().endsWith(SHP_XML_EXTENSION)){
            int idx = fname.toLowerCase().indexOf("." + SHP_XML_EXTENSION);
            if (idx >= 1){   // if idx==0, then the file name is ".shp.xml""
                String basename = fname.substring(0, idx);
                String ext = fname.substring(idx+1);
                addToFileGroupHash(basename, ext);
                return;
            }
        }
        
        String[] tokens = fname.split("\\.(?=[^\\.]+$)");
        if (tokens.length==1){
            addToFileGroupHash(tokens[0], BLANK_EXTENSION);      // file basename, no extension

        }else if (tokens.length==2){
            addToFileGroupHash(tokens[0], tokens[1]);  // file basename, extension
        }
    } // end updateFileGroupHash
    
    
    /**************************************
     * Iterate through the zip file contents.
     * Does it contain any shapefiles?
     *
     * @param FileInputStream zip_file_stream
     */
    private boolean examineZipfile(FileInputStream zip_file_stream){
       // msgt("examineZipfile");
        
        if (zip_file_stream==null){
               this.addErrorMessage("The zip file stream was null");
               return false;
           }
        
       // Clear out file lists
       this.filesListInDir.clear();
       this.filesizeHash.clear();
       this.fileGroups.clear();
       
       try{
            ZipInputStream zip_stream = new ZipInputStream(zip_file_stream);
            ZipEntry entry;
            
            while((entry = zip_stream.getNextEntry())!=null){

                 String zentry_file_name = entry.getName();
                 //msg("zip entry: " + entry.getName());
                 // Skip files or folders starting with __
                 if (zentry_file_name.startsWith("__")){
                     continue;
                 }

                if (entry.isDirectory()) {
                   //String dirpath = outputFolder + "/" + zentry_file_name;
                   //createDirectory(dirpath);
                   continue;       
                }
                
                String s = String.format("Entry: %s len %d added %TD",
                                   entry.getName(), entry.getSize(),
                                   new Date(entry.getTime()));

                this.filesListInDir.add(s);
                updateFileGroupHash(zentry_file_name);
                this.filesizeHash.put(zentry_file_name, entry.getSize());
           } // end while
           
           zip_stream.close();

           if (this.filesListInDir.isEmpty()){
               errorMessage = "No files in zip_stream";
               return false;
           }

           this.zipFileProcessed = true;
           return true;

       }catch(ZipException ex){
               this.addErrorMessage("ZipException");
               msgt("ZipException");
               return false;

       }catch(IOException ex){
           //ex.printStackTrace(); 
           this.addErrorMessage("IOException File name");
           msgt("IOException");
           return false;
       }finally{
           // we must always close the zip file.
       }

   } // end examineFile

  public static void main(String[] args){

        // Example usage
       if (args.length == 0){
           

       }else if(args.length > 1){
           System.out.println( "Please only give one file name!");  
       }else{   
           /*
           String zip_name =  args[0];      
           System.out.println( "Process File: " + zip_name);
           System.out.println( "Process File: " + zip_name);                
           ShapefileHandler zpt = new ShapefileHandler(zip_name);
           */
       }
   } // end main

} // end ShapefileHandler