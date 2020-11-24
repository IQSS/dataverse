/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/
package edu.harvard.iq.dataverse.custom.service.download;

import edu.harvard.iq.dataverse.custom.service.util.DirectAccessUtil;
import static edu.harvard.iq.dataverse.custom.service.util.DatabaseAccessUtil.lookupZipJob;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Custom (standalone) download service for Dataverse
 * 
 * @author Leonid Andreev
 */
public class ZipDownloadService { 
    
    private static String jobKey = null;
    private List<String[]> jobFiles = null;
    private boolean zipOnly = false; 
    
    private DirectAccessUtil directAccessUtil = null; 
    private ZipOutputStream zipOutputStream = null;

    public static void main(String args[]) throws Exception {
        
        ZipDownloadService zipperService = new ZipDownloadService();
        
        if (!zipperService.parseArgs(args)) {
            zipperService.usage();
            return; 
        }
        
        zipperService.parseCgiQueryParameters();
               
        zipperService.execute(jobKey);
    }

    private static void usage() {
        System.out.println("\nUsage:");
        System.out.println("  java -jar ZipDownloadService-1.0.0.jar [-ziponly]>\n");

        System.out.println("  supported options:");
        System.out.println("   -ziponly = output zip only, no http header/no chunking");
        System.out.println("");

    }

    // The only option supported at the moment is "zip only" - output just the
    // compressed stream, skip the HTTP header and chunking.
    public boolean parseArgs(String[] args) {

        if (args == null || args.length == 0) {
            return true; 
        } else if (args.length == 1) {
            if (args[0].equals("-ziponly")) {
                this.zipOnly = true;
                return true;
            }
        }
        
        return false; 
    }
    
    // Does not support any parameters, except the job-identifying token key, 
    // supplied as the entire query string. 
    public void parseCgiQueryParameters() {
        String queryString = System.getenv().get("QUERY_STRING");
        if (queryString != null) {
            jobKey = queryString; 
        }
    }
    
    public void print404() {
        System.out.println("Status: 404 Not Found\r");
        System.out.println("Content-Type: text/html\r");
        System.out.println("\r");

        System.out.println("<h1>404 No such download job!</h1>");
    }
    
    public void printZipHeader() {
        System.out.println("Content-disposition: attachment; filename=\"dataverse_files.zip\"\r");
        System.out.println("Content-Type: application/zip; name=\"dataverse_files.zip\"\r");
        System.out.println("Transfer-Encoding: chunked\r");
        System.out.println("\r");
        System.out.flush();
    }
    
    public void execute(String key) {
        
        jobFiles = lookupZipJob(key); 
        
        if (jobFiles == null || jobFiles.size() == 0) {
            this.print404();
            System.exit(0);
        }
        
        this.processFiles();
    }
    
    public void processFiles() {
        
        if (!this.zipOnly) {
            this.printZipHeader();
        }
        
        Set<String> zippedFolders = new HashSet<>();
        Set<String> fileNamesList = new HashSet<>();
       
        for (String [] fileEntry : jobFiles) {
            String storageLocation = fileEntry[0];
            String fileName = fileEntry[1];
            
            //System.out.println(storageLocation + ":" + fileName);
            
            if (this.zipOutputStream == null) {
                openZipStream();
            }
            
            if (this.directAccessUtil == null) {
                this.directAccessUtil = new DirectAccessUtil();
            }
            
            InputStream inputStream = this.directAccessUtil.openDirectAccess(storageLocation);
                
            String zipEntryName = checkZipEntryName(fileName, fileNamesList);
            // this may not be needed anymore - some extra sanitizing of the file 
            // name we used to have to do - since all the values in a current Dataverse 
            // database may already be santized enough. 
	    // (Edit: Yes, we still need this - there are still datasets with multiple 
	    // files with duplicate names; this method takes care of that)
            if (inputStream != null && this.zipOutputStream != null) {
                
                ZipEntry entry = new ZipEntry(zipEntryName);

                byte[] bytes = new byte[2 * 8192];
                int read = 0;
                long readSize = 0L;

                try {
                    // Does this file have a folder name? 
                    if (hasFolder(zipEntryName)) {
                        addFolderToZipStream(getFolderName(zipEntryName), zippedFolders);
                    }

                    this.zipOutputStream.putNextEntry(entry);

                    while ((read = inputStream.read(bytes)) != -1) {
                        this.zipOutputStream.write(bytes, 0, read);
                        readSize += read;
                    }
                    this.zipOutputStream.closeEntry();

                    /*if (fileSize == readSize) {
                        //System.out.println("Read "+readSize+" bytes;");
                    } else {
                        throw new IOException("Byte size mismatch: expected " + fileSize + ", read: " + readSize);
                    }*/
                } catch (IOException ioex) {
                    System.err.println("Failed to compress "+storageLocation);
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException ioexIgnore) {
                        System.err.println("Warning: IO exception trying to close input stream - "+storageLocation);
                    }
                }
            } else {
                System.err.println("Failed to access "+storageLocation);
            }
                
        }
        try {
            this.zipOutputStream.flush();
            this.zipOutputStream.close();

            System.out.flush();
            System.out.close();
        } catch (Exception e) {
        }
    }
    
    public void openZipStream() {
        if (this.zipOutputStream == null) {
            if (this.zipOnly) {
                this.zipOutputStream = new ZipOutputStream(System.out);
            } else {
                this.zipOutputStream = new ZipOutputStream(new ChunkingOutputStream(System.out));
            }
        }
    }
    
    private boolean hasFolder(String fileName) {
        if (fileName == null) {
            return false;
        }
        return fileName.indexOf('/') >= 0;
    }
    
    private String getFolderName(String fileName) {
        if (fileName == null) {
            return "";
        }
        String folderName = fileName.substring(0, fileName.lastIndexOf('/'));
        // If any of the saved folder names start with with slashes,            
        // we want to remove them:                                              
        // (i.e., ///foo/bar will become foo/bar)                               
        while (folderName.startsWith("/")) {
            folderName = folderName.substring(1);
        }
        return folderName;
    }
    
    private void addFolderToZipStream(String folderName, Set<String> zippedFolders) throws IOException {
        // We don't want to create folders in the output Zip file that have 
        // already been added:
        if (!"".equals(folderName)) {
            if (!zippedFolders.contains(folderName)) {
                ZipEntry d = new ZipEntry(folderName + "/");
                zipOutputStream.putNextEntry(d);
                zipOutputStream.closeEntry();
                zippedFolders.add(folderName);
            }
        }
    }
    
    // check for and process duplicates:
    private String checkZipEntryName(String originalName, Set<String> fileNames) {
        String name = originalName;
        int fileSuffix = 1;
        int extensionIndex = originalName.lastIndexOf(".");

        while (fileNames.contains(name)) {
            if (extensionIndex != -1) {
                name = originalName.substring(0, extensionIndex) + "_" + fileSuffix++ + originalName.substring(extensionIndex);
            } else {
                name = originalName + "_" + fileSuffix++;
            }
        }
        fileNames.add(name);
        return name;
    }
}
