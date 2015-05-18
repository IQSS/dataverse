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
import java.util.zip.ZipException;
import java.util.HashMap;
import java.util.*;

import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 *  Used to identify, "repackage", and extract data from Shapefiles in .zip format
 *
 *  (1) Identify if a .zip contains a shapefile: 
 *          boolean containsShapefile(FileInputStream zipStream) or boolean containsShapefile(FileInputStream zip_filename) 
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
 * @author raprasad
 *
 * 
 */
public class ShapefileHandler{

    private static final Logger logger = Logger.getLogger(ShapefileHandler.class.getCanonicalName());

    // Reference for these extensions: http://en.wikipedia.org/wiki/Shapefile
    public final static String SHAPEFILE_FILE_TYPE = "application/zipped-shapefile";
    public final static String SHAPEFILE_FILE_TYPE_FRIENDLY_NAME = "Shapefile as ZIP Archive";
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
    
    private List<File> finalRezippedFiles = new ArrayList<>();
    
    private String outputFolder = "unzipped";
    private String rezippedFolder = "rezipped";

    // Debug helper
    private void msg(String s){
        //logger.info(s);
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
    
    public List<File> getFinalRezippedFiles(){
        return this.finalRezippedFiles;
    }
    
    private void addFinalRezippedFile(String targetFileFullpath){
        if (targetFileFullpath==null){
            logger.warning("addFinalRezippedFile. targetFileFullpath is null");
            return;
        }
        File finalFile = new File(targetFileFullpath);
        if (!(finalFile.isFile())){
            logger.warning("addFinalRezippedFile. Not a file: " + targetFileFullpath);
            return;
        }
        this.finalRezippedFiles.add(finalFile);
    };

    
    private void addErrorMessage(String m){
        if (m == null){
            return;
        }
        logger.severe("ShapeFileHandler Error: " + m);
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
    
    private String getFileBasename(String fileName){
        if (fileName==null){
            return null;
        }
        String unzipFileName = new File(fileName).getName();
        if (unzipFileName.equals("")){
            logger.info("getFileBasename.  fileName is an empty string: " + fileName);
            return null;
        }
        return unzipFileName;
    }
    /*
        Unzip the files to the directory, FLATTENING the directory structure
        
        Any colliding names will result  in overwrites
    
    */
    private boolean unzipFilesToDirectory(FileInputStream zipfile_input_stream, File target_directory){
        //logger.info("unzipFilesToDirectory: " + target_directory.getAbsolutePath() );

        if (zipfile_input_stream== null){
            this.addErrorMessage("unzipFilesToDirectory. The zipfile_input_stream is null.");
            return false;
        }
        if (!target_directory.isDirectory()){
             this.addErrorMessage("This directory does not exist: " + target_directory.getAbsolutePath());
            return false;
        }

       List<String> unzippedFileNames = new ArrayList<>(); 
       
       ZipInputStream zipStream = new ZipInputStream(zipfile_input_stream);

       ZipEntry origEntry;
        byte[] buffer = new byte[2048];
        try {
            while((origEntry = zipStream.getNextEntry())!=null){
                
                String zentryFileName = origEntry.getName();
                //logger.info("\nOriginal entry name: " + origEntry);
                
                 if (this.isFileToSkip(zentryFileName)){
                    logger.fine("Skip file");
                    continue;
                 }
                
                // Create sub directory, if needed
                if (origEntry.isDirectory()) {
                    //logger.info("Subdirectory found!");
                    logger.fine("Skip directory");
                    //String dirpath = target_directory.getAbsolutePath() + "/" + zentryFileName;
                    //createDirectory(dirpath);
                    continue;           // Continue to next Entry
                }
                logger.fine("file found!");
                
                // Write the file
                String unzipFileName = this.getFileBasename(zentryFileName);
                if (unzipFileName==null){
                    logger.warning("Zip Entry Basename is an empty string: " + zentryFileName);
                    continue;
                }
                
                String outpath = target_directory.getAbsolutePath() + "/" + unzipFileName;
                if (unzippedFileNames.contains(outpath)){
                   logger.info("Potential name collision.  Avoiding duplicate files in 'collapsed' zip directories. Skipping file: " + zentryFileName);
                   continue;
                }else{
                    unzippedFileNames.add(outpath);
                }
                logger.fine("Write zip file: " + outpath);
                FileOutputStream fileOutputStream;
                long fsize = 0;
                fileOutputStream = new FileOutputStream(outpath);
                int len;// = 0;
                while ((len = zipStream.read(buffer)) > 0){
                    fileOutputStream.write(buffer, 0, len);
                    fsize+=len;
                } // end while
                fileOutputStream.close();
            } // end outer while
        } catch (IOException ex) {
            for (StackTraceElement el : ex.getStackTrace()){
                logger.severe(el.toString());
            }
            this.addErrorMessage("Failed to open ZipInputStream entry" +  ex.getMessage());
            return false;
        }

        try {
             zipStream.close();
        } catch (IOException ex) {
             Logger.getLogger(ShapefileHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    return true;
    }
    /*
        Rezip the shapefile(s) into a given directory
        Assumes that the zipfile_input_stream has already been checked!
    */
    public boolean rezipShapefileSets(FileInputStream zipfile_input_stream, File rezippedFolder) throws IOException{
        logger.fine("rezipShapefileSets");
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
        logger.fine("Try to create directory: " + dirname_for_unzipping );

        if (!this.createDirectory(dir_for_unzipping)){
            this.addErrorMessage("Failed to make directory: " + dirname_for_unzipping);
            return false;
        }
        
        
        // Unzip files!
        if (!this.unzipFilesToDirectory(zipfile_input_stream, dir_for_unzipping)){
            this.addErrorMessage("Failed to unzip files.");
            return false;
        }
        // Redistribute files!
        String target_dirname = rezippedFolder.getAbsolutePath();
        boolean redistribute_success = this.redistributeFilesFromZip(dirname_for_unzipping, target_dirname);

        //logger.fine("About to delete: " + dir_for_unzipping);               
        // Delete unzipped files in scratch directory
        //FileUtils.deleteDirectory(dir_for_unzipping);
        
        logger.fine("Post redistribute:)");
        for (File f : new File(target_dirname).listFiles()){
            logger.fine("File exists: " + f.getAbsolutePath());
        }
        
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
        if (file_ext.equals(BLANK_EXTENSION)){
            return dirname + "/" + file_basename;
        }
        return dirname + "/" + file_basename + "." + file_ext;
    }
    
    /*
        Create new zipped shapefile
    
    
    */
    private boolean redistributeFilesFromZip(String source_dirname, String target_dirname){

        logger.fine("redistributeFilesFromZip. source: '" + source_dirname + "'  target: '" + target_dirname + "'");

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
                        String targetFileFullpath = this.getRedistributeFilePath(target_dirname, key, ext_name);
                        this.straightFileCopy(source_file_fullpath, targetFileFullpath);
                        this.addFinalRezippedFile(targetFileFullpath);
                    }else{
                        namesToZip.add(key + "." + ext_name);
                
                    }
                }
            
                String target_zipfile_name = target_dirname + "/" + key + ".zip";
                //this.msg("target_zipfile_name: "+ target_zipfile_name);
                //this.msg("source_dirname: "+ source_dirname);
                
                //msgt("create zipped shapefile");
                ZipMaker zip_maker = new ZipMaker(namesToZip, source_dirname, target_zipfile_name);
                this.addFinalRezippedFile(target_zipfile_name);

                // rezip it
                                
            }else{
                // Non-shapefiles
                for (String ext_name : ext_list) {
                    String source_file_fullpath = this.getRedistributeFilePath(source_dirname, key, ext_name);
                    String targetFileFullpath = this.getRedistributeFilePath(target_dirname, key, ext_name);
                    this.straightFileCopy(source_file_fullpath, targetFileFullpath);
                    this.addFinalRezippedFile(targetFileFullpath);

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
        if (!(extension_list.contains(ext))){
            extension_list.add(ext);
            fileGroups.put(basename, extension_list);
        }
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
    
    private boolean isFileToSkip(String fname){
        if ((fname==null)||(fname.equals(""))){
            return true;
        }
        
        if (fname.startsWith("__")){
            return true;
        }
        
        if (fname.startsWith("._")){
            return true;
        }
        
        File fnameFile = new File(fname);
        if (fnameFile.getName().endsWith(".DS_Store")){
            return true;
        }
        return false;
    }
    
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
            ZipInputStream zipStream = new ZipInputStream(zip_file_stream);
            ZipEntry entry;
            
            while((entry = zipStream.getNextEntry())!=null){

                 String zentryFileName = entry.getName();
                 //msg("zip entry: " + entry.getName());
                 // Skip files or folders starting with __
                 if (this.isFileToSkip(zentryFileName)){
                     continue;
                 }

                if (entry.isDirectory()) {
                   //String dirpath = outputFolder + "/" + zentryFileName;
                   //createDirectory(dirpath);
                   continue;       
                }
                
                String unzipFileName = this.getFileBasename(zentryFileName);
                if (unzipFileName==null){
                    logger.warning("Zip Entry Basename is an empty string: " + zentryFileName);
                    continue;
                }

                
                String s = String.format("Entry: %s len %d added %TD",
                                   unzipFileName, entry.getSize(),
                                   new Date(entry.getTime()));

                if (!this.filesListInDir.contains(s)){                   
                    this.filesListInDir.add(s);
                    updateFileGroupHash(unzipFileName);
                    this.filesizeHash.put(unzipFileName, entry.getSize());
                }
           } // end while
           
           zipStream.close();

           if (this.filesListInDir.isEmpty()){
               errorMessage = "No files in zipStream";
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
       }catch(IllegalArgumentException ex){
           this.addErrorMessage("IllegalArgumentException when parsing zipfile");
           msgt("IllegalArgumentException when parsing zipfile");
           return false;
           
       }finally{
           
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