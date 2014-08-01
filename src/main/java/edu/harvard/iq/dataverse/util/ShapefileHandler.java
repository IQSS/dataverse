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
import java.util.logging.Level;
import java.util.logging.Logger;
//import java.nio.file.Path;

/**
 *  Used to identify, "repackage", and extract data from Shapefiles in .zip format
 * 
 *  (1) Identify if a .zip contains a shapefile: 
 *          boolean containsShapefile(FileInputStream zip_stream) or boolean containsShapefile(FileInputStream zip_filename) 
 *
 *  (2) Unpack/"Repackage" .zip:
 *          (a) All files extracted
 *          (b) Each group of files that make up a shapefile are made into individual .zip files
 *          (c) Non shapefile related files left on their own
 *
 * (3) For shapefile sets, described in (2)(b), create a text file describing the contents of the zipped shapefile
 * 
 * @author Raman Prasad
 * 
 */
public class ShapefileHandler{

    // Reference for these extensions: http://en.wikipedia.org/wiki/Shapefile
    public final static List<String> SHAPEFILE_MANDATORY_EXTENSIONS = Arrays.asList("shp", "shx", "dbf", "prj");
    public final static String SHP_XML_EXTENSION = "shp.xml";
    public final static List<String> SHAPEFILE_ALL_EXTENSIONS = Arrays.asList("shp", "shx", "dbf", "prj", "sbn", "sbx", "fbn", "fbx", "ain", "aih", "ixs", "mxs", "atx", ".cpg", SHP_XML_EXTENSION);  
    
    private static boolean DEBUG = true;
        
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
               , "README" : [""]
              }
    */
    private Map<String, List<String>> fileGroups = new HashMap<String, List<String>>();
    
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
    public ShapefileHandler(FileInputStream zip_file_stream, String output_folder, String rezipped_folder){
        //processZipfile(zip_file_stream);
        this.outputFolder = output_folder;
        this.rezippedFolder = rezipped_folder;
        msg("rezippedFolder " + this.rezippedFolder );
        msg("Exists " + new File(this.rezippedFolder).exists() );
        this.examineZipfile(zip_file_stream);
        //showFileNamesSizes();
        this.showFileGroups();
    }

     /*
        Constructor, start with FileInputStream
    */
    public ShapefileHandler(FileInputStream zip_file_stream){
        //processZipfile(zip_file_stream);
        
        this.examineZipfile(zip_file_stream);
        //showFileNamesSizes();
        //showFileGroups();
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
    private void showFileGroups(){

        msgt("Hash: file base names + extensions");
        
        for (Map.Entry<String, List<String>> entry : fileGroups.entrySet()){
            msg("Key: [" + entry.getKey() + "] Ext List: " + entry.getValue());
            if (doesListContainShapefileExtensions(entry.getValue())){
                msg(" >>>> GOT IT! <<<<<<<<");
            }
        }
       
    } // end showFileGroups
    
        
    
    /*    
        Given the path to a zip file, do the following:
    
        - Unzip the contents to a directory
        - check 
    */
    public void processZipfile(String zip_filename){
        if (zip_filename==null){
            this.addErrorMessage("The .zip filename was not given");
            return;
        }
        
        File zip_file = new File(zip_filename);
        if (!zip_file.isFile()){
            this.addErrorMessage("This is not a file: " + zip_file.getAbsolutePath());
            return;
        }
        FileInputStream zip_filestream;
        try{
            zip_filestream = new FileInputStream(zip_file);
        }catch(FileNotFoundException e){
            this.addErrorMessage("FileNotFoundException: " + zip_file.getAbsolutePath());
            return;
        }
        this.processZipfile(zip_filestream);
        
    }
    
    
    public void processZipfile(FileInputStream zip_filestream){
        if (zip_filestream==null){
            this.addErrorMessage("The .zip FileInputStream was not given");
            return;
        }
        
        // Examine the file
        examineAndUnzipFile(zip_filestream);

        if (!this.zipFileProcessed){
            msgt("not processed?");
            return;
        }
        msgt("What have we got!");
        for (String element : filesListInDir) {
            msg(element);
        }

        rezipShapefileSets();

        showFileNamesSizes();
        showFileGroups();
    }
    
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
            msg("Write zip file" + outpath);
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
        
        msgt("rezipShapefileSets");
        
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
        String dirname_for_unzipping;
        File dir_for_unzipping;
        
        dirname_for_unzipping = rezippedFolder.getAbsolutePath() + "/" + "scratch-for-unzip-12345";
        dir_for_unzipping = new File(dirname_for_unzipping);
        dir_for_unzipping.mkdirs();
        this.unzipFilesToDirectory(zipfile_input_stream, dir_for_unzipping);
        
        if (true){
            
            return true;
        }
        
      if (false){
          msg("unzipped directory: " + dir_for_unzipping.getAbsolutePath());
        return true;
          
      } 
     
      String target_dirname = rezippedFolder.getAbsolutePath();
       // START: Redistribute files!
       for (Map.Entry<String, List<String>> entry : fileGroups.entrySet()){
            
            String key = entry.getKey();
            msg("\n" + key);
            List<String> ext_list = entry.getValue();
            msg(Arrays.toString(ext_list.toArray()));
        
            if (doesListContainShapefileExtensions(ext_list)){
                List<String> namesToZip = new ArrayList<>();
                
                for (String ext : ext_list) {
                    namesToZip.add(key + "." + ext);
                }
                String target_zipfile_name = target_dirname + "/" + key + ".zip";
                this.msg("target_zipfile_name: "+ target_zipfile_name);
                this.msg("source_dirname: "+ dirname_for_unzipping);
                
                msgt("copy shapefile");
                ZipMaker zip_maker = new ZipMaker(namesToZip, dirname_for_unzipping, target_zipfile_name);
                // rezip it
                
                
            }else{
                for (String ext : ext_list) {
                    File source_file = new File(dir_for_unzipping + "/" + key + "." + ext);
                    File target_file = new File(target_dirname + "/" + key + "." + ext);
                    msg("-- copy non shapefile --");
                    msg("Source file: " + source_file.getAbsolutePath());
                    msg("Target file: " + target_file.getAbsolutePath());
                    
                    File target_file_dir = target_file.getParentFile();
                    createDirectory(target_file_dir);
    
                   try{
                        Files.copy(source_file.toPath(), target_file.toPath(), REPLACE_EXISTING);    
                        msg("File copied: " + source_file.toPath() + " To: " + target_file.toPath());
                    }catch(java.nio.file.NoSuchFileException ex){
                        this.addErrorMessage("Failed to copy file. NoSuchFileException: [" + key + "."+ ext);
                        return false;
                    }catch(IOException ex){
                        this.addErrorMessage("Failed to copy file. IOException: [" + key + "."+ ext);
                        return false;
                    }
                }
            }
        }
       
       // END: Redistribute files
       
        return true;
    }
    /*
        Make a folder with the extracted files
        Except: separately rezip shapefile sets
    */
    public boolean rezipShapefileSets(){
        
        msgt("rezipShapefileSets");
        if (!containsShapefile()){
            msgt("There are no shapefiles to re-zip");
            return false;
        }else if(containsOnlySingleShapefile()){
            msgt("Original zip is good! Every file is part of a single shapefile set");
            return false;
        }
        
        deleteDirectory(rezippedFolder);
        if (!createDirectory(rezippedFolder)){
            errorMessage = "Failed to create rezipped directory: " + rezippedFolder;
        }
        
        
        for (Map.Entry<String, List<String>> entry : fileGroups.entrySet()){
            
            String key = entry.getKey();
            msg("\n" + key);
            List<String> ext_list = entry.getValue();
            msg(Arrays.toString(ext_list.toArray()));
        
            if (doesListContainShapefileExtensions(ext_list)){
                List<String> namesToZip = new ArrayList<>();
                
                for (String ext : ext_list) {
                    namesToZip.add(key + "." + ext);
                }
                String shpZippedName = rezippedFolder + "/" + key + ".zip";
                this.msg("shpZippedName: "+ shpZippedName);
                this.msg("outputFolder: "+ this.outputFolder);
                ZipMaker zip_maker = new ZipMaker(namesToZip, this.outputFolder, shpZippedName);
                // rezip it
                
                
            }else{
                for (String ext : ext_list) {
                    File source_file = new File(outputFolder + "/" + key + "." + ext);
                    File target_file = new File(rezippedFolder + "/" + key + "." + ext);
                   
                    File target_file_dir = target_file.getParentFile();
                    createDirectory(target_file_dir);
    
                   try{
                        Files.copy(source_file.toPath(), target_file.toPath(), REPLACE_EXISTING);    
                        msg("File copied: " + source_file.toPath() + " To: " + target_file.toPath());
                    }catch(java.nio.file.NoSuchFileException ex){
                        
                        msgt("NoSuchFileException");
                        
                    }catch(IOException ex){
                        
                        msgt("failed to copy");
                    }
                }
            }
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
            addToFileGroupHash(tokens[0], "");      // file basename, no extension

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

    
    
    
    /*
    
       @param fname .zip filename in String format
        
    */
    private boolean examineAndUnzipFile(String filename){
       
         if (filename==null){
                this.addErrorMessage("No file name was given.  Please use a file with the .zip extension");            
                return false;
            }
         if (!filename.toLowerCase().endsWith(".zip")){
                this.addErrorMessage("This file does not end with the .zip extension");
                return false;
         }
         
         File f;
         
         f = new File(filename);
         if (!f.exists()){            
            this.addErrorMessage("The file does not exist: " + filename);            
            return false;
         }
         
         return this.examineAndUnzipFile(f);
    }
    
    /*
    
       @param fname .zip filename in String format
        
    */
    private boolean examineAndUnzipFile(File file_obj){
            
        if (file_obj==null){
            this.addErrorMessage("The file object stream is null");
            return false;
        }

            
        FileInputStream zip_file_stream;// = null;
            
        try{
            zip_file_stream = new FileInputStream(file_obj);
            
        }catch(FileNotFoundException ex){
            this.addErrorMessage("The file object was not found!");
            return false;
        }
        return this.examineAndUnzipFile(zip_file_stream);
            
            
    }
    
    private boolean examineAndUnzipFile(FileInputStream zip_file_stream){
        msgt("examineAndUnzipFile: " + zip_file_stream.toString());
         if (zip_file_stream==null){
                this.addErrorMessage("The zip file stream was null");
                return false;
            }

            if (!createDirectory(this.outputFolder)){
                msg("Failed to create directory! " + this.outputFolder);
                return false;
            }

    try{
        ZipInputStream zip_stream = new ZipInputStream(zip_file_stream);
        
        
         ZipEntry entry;
         byte[] buffer = new byte[2048];
    
          while((entry = zip_stream.getNextEntry())!=null){
              
              String zentry_file_name = entry.getName();
              
              // Skip files or folders starting with __
              if (zentry_file_name.startsWith("__")){
                  continue;
              }
              
                if (entry.isDirectory()) {
                    String dirpath = outputFolder + "/" + zentry_file_name;
                    createDirectory(dirpath);
                    
                    // Continue to next Entry
                    continue;       
                }
                String s = String.format("Entry: %s len %d added %TD",
                                entry.getName(), entry.getSize(),
                                new Date(entry.getTime()));
                filesListInDir.add(s);
                                                
                updateFileGroupHash(zentry_file_name);
                
                //msg(s);
                
                // Once we get the entry from the stream, the stream is
                // positioned read to read the raw data, and we keep
                // reading until read returns 0 or less.
                String outpath = outputFolder + "/" + entry.getName();
                msg("Write zip file" + outpath);
                FileOutputStream output = null;
                try{           
                    long fsize = 0;
                    output = new FileOutputStream(outpath);
                    int len;// = 0;
                    while ((len = zip_stream.read(buffer)) > 0){
                        output.write(buffer, 0, len);
                        fsize+=len;
                    } // end while
                    //msg("File size: " + fsize + " from zip:" + entry.getSize());
                    if (!(entry.getSize()== fsize)){
                        msg("different: " + fsize);
                        filesizeHash.put(zentry_file_name, fsize);  // Pull file size from actual unzipped file
                    }else{
                        filesizeHash.put(zentry_file_name, entry.getSize());    // Pull file size from .zip metadata
                    }
                }finally{
                    // we must always close the output file
                    if(output!=null) output.close();
                } // end try
            }
            
            zip_stream.close();
            
            if (filesListInDir.isEmpty()){
                errorMessage = "No files in zip_stream";
                return false;
            }
            
            zipFileProcessed = true;
            return true;
            
        }catch(ZipException ex){
                errorMessage = "ZipException";
                msgt("ZipException");
                return false;
                
        }catch(IOException ex){
            //ex.printStackTrace(); 
            errorMessage = "IOException File name";
            msgt("IOException");
            return false;
        }finally{
            // we must always close the zip file.
        }
        
    } // end examineAndUnzipFile
    
    
    public static void main(String[] args){

       if (args.length == 0){
           System.out.println( "No file name, so add one!");
           // Water_single_shp.zip
         //  ShapefileHandler zpt = new ShapefileHandler("unzipped.zip");  
           File zfile = new File("../test_data/Waterbody_shp.zip");
           FileInputStream zstream = null;
           try{
               zstream = new FileInputStream(zfile);
           }catch(FileNotFoundException ex){
               
           }
           ShapefileHandler zpt = new ShapefileHandler(zstream);
          // ShapefileHandler zpt = new ShapefileHandler("../test_data/Waterbody_shp.zip");    
          // ShapefileHandler zpt = new ShapefileHandler("social_disorder_in_boston.zip");
           if (!zpt.zipFileProcessed){
               System.out.println("--------- FAIL -------------");
               System.out.println(zpt.errorMessage);
               System.out.println("Contains shapefile? " + zpt.containsShapefile());
           }
           if (zpt.containsShapefile()){
               System.out.println("--------- SHAPE FOUND! -------------");
               System.out.println("Shape count: " + zpt.getShapefileCount());
               System.out.println("Contains shapefile? " + zpt.containsShapefile());
           }

       }else if(args.length > 1){
           System.out.println( "Please only give one file name!");  
       }else{   
           String zip_name =  args[0];      
           System.out.println( "Process File: " + zip_name);

           System.out.println( "Process File: " + zip_name);                
           ShapefileHandler zpt = new ShapefileHandler(zip_name);
           if (!zpt.zipFileProcessed){
               System.out.println("--------- FAIL -------------");
               
               System.out.println(zpt.errorMessage);
           }
           if (zpt.containsShapefile()){
               System.out.println("--------- SHAPE FOUND! -------------");
               System.out.println("Shape count: " + zpt.getShapefileCount());
               
           }
           
       }
   } // end main

} // end ShapefileHandler