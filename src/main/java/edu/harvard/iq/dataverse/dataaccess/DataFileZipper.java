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
package edu.harvard.iq.dataverse.dataaccess;

import java.io.InputStream;
import java.io.IOException;

import java.util.Iterator;

import edu.harvard.iq.dataverse.DataFile;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 *
 * @author Leonid Andreev
 */
public class DataFileZipper {
    public static long DEFAULT_ZIPFILE_LIMIT = 100 * 1024 * 1024; // 100MB
    
    private static final Logger logger = Logger.getLogger(DataFileZipper.class.getCanonicalName());
    
    private OutputStream outputStream = null; 
    private ZipOutputStream zipOutputStream = null;
    
    private List<String> fileNameList = null; // the list of file names to check for duplicates
    private List<Long> zippedFilesList = null; // list of successfully zipped files, to update guestbooks and download counts (not yet implemented)
    
    private String fileManifest = "";

    public DataFileZipper() {
        fileNameList = new ArrayList<>();
        zippedFilesList = new ArrayList<>(); 
    }
    
    public DataFileZipper(OutputStream outputStream) {
        this.outputStream = outputStream;
        fileNameList = new ArrayList<>();
        zippedFilesList = new ArrayList<>();
    }
    
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream; 
    }
    
    public OutputStream getOutputStream() {
        return this.outputStream;
    }
    
    public void setFileManifest(String fileManifest) {
        this.fileManifest = fileManifest;
    }
    
    public String getFileManifest() {
        return this.fileManifest; 
    }
    
    /* 
    The following 4 methods are no longer in use - should be removed.
    
    public void zipFiles(List<DataFile> files, OutputStream outstream) throws IOException {
        zipFiles(files, outstream, null, DEFAULT_ZIPFILE_LIMIT); 
    }
    
    public void zipFiles(List<DataFile> files, OutputStream outstream, long sizelimit) throws IOException {
        zipFiles(files, outstream, null, sizelimit);
    }
    
    public void zipFiles(List<DataFile> files, OutputStream outstream, String fileManifest) throws IOException {
        zipFiles(files, outstream, fileManifest, DEFAULT_ZIPFILE_LIMIT);
    }
    
    
    public void zipFiles(List<DataFile> files, OutputStream outstream, String fileManifest, long sizeLimit) throws IOException {
        boolean createManifest = fileManifest != null;

        if (files.size() < 1) {
            throw new IOException("Empty files list.");
        }

        long sizeTotal = 0L;

        ZipOutputStream zout = new ZipOutputStream(outstream);

        List nameList = new ArrayList(); // to check for duplicates
        List successList = new ArrayList(); // to update download counts (not yet implemented)

        Iterator iter = files.iterator();

        while (iter.hasNext()) {
            DataFile file = (DataFile) iter.next();

            DataAccessRequest daReq = new DataAccessRequest();

            StorageIO accessObject = DataAccess.createDataAccessObject(file, daReq);

            if (accessObject != null) {
                accessObject.open();
                long fileSize = accessObject.getSize();

                String fileName = accessObject.getFileName();
                String mimeType = accessObject.getMimeType();
                if (mimeType == null || mimeType.equals("")) {
                    mimeType = "application/octet-stream";
                }

                if (sizeTotal + fileSize < sizeLimit) {

                    Boolean Success = true;

                    InputStream instream = accessObject.getInputStream();
                    if (instream == null) {
                        if (createManifest) {
                            fileManifest = fileManifest + fileName
                                    + " (" + mimeType
                                    + ") COULD NOT be downloaded because an I/O error has occured. \r\n";
                        }

                        Success = false;
                    } else {
                        String zipEntryName = checkZipEntryName(fileName); //, nameList);
                        ZipEntry e = new ZipEntry(zipEntryName);
                        logger.fine("created new zip entry for " + zipEntryName);
                        // support for categories: (not yet implemented)
                        //String zipEntryDirectoryName = file.getCategory(versionNum);
                        //ZipEntry e = new ZipEntry(zipEntryDirectoryName + "/" + zipEntryName);

                        zout.putNextEntry(e);

                        // before writing out any bytes from the input stream, flush
                        // any extra content, such as the variable header for the 
                        // subsettable files:
                        String varHeaderLine = accessObject.getVarHeader();
                        if (varHeaderLine != null) {
                            zout.write(varHeaderLine.getBytes());
                            fileSize += (varHeaderLine.getBytes().length);
                        }

                        byte[] data = new byte[8192];

                        int i = 0;
                        long byteSize = 0; 
                        while ((i = instream.read(data)) > 0) {
                            zout.write(data, 0, i);
                            logger.fine("wrote " + i + " bytes;");

                            byteSize += i;
                            //zout.flush();
                        }
                        instream.close();
                        zout.closeEntry();
                        logger.fine("closed zip entry for " + zipEntryName);

                        if (createManifest) {
                            fileManifest = fileManifest + zipEntryName + " (" + mimeType + ") " + fileSize + " bytes.\r\n";
                        }

                        if (byteSize > 0) {
                            successList.add(file.getId());
                            if (fileSize != byteSize) {
                                logger.warning("File size mismatch: "+fileSize+" in the database, "+byteSize+" on disk.");
                            }
                            sizeTotal += Long.valueOf(byteSize);
                        }
                    }
                } else {
                    if (createManifest) {
                        fileManifest = fileManifest + fileName + " (" + mimeType + ") " + " skipped because the total size of the download bundle exceeded the limit of " + sizeLimit + " bytes.\r\n";
                    }
                }
            }
        }

        // finally, let's create the manifest entry (if requested):
        if (createManifest) {
            ZipEntry e = new ZipEntry("MANIFEST.TXT");

            zout.putNextEntry(e);
            zout.write(fileManifest.getBytes());
            zout.closeEntry();
        }

        zout.close();

    }
    */
    
    public void openZipStream() throws IOException {
        if (outputStream == null) {
            throw new IOException("Attempted to create a ZipOutputStream from a NULL OutputStream.");
        }
        this.zipOutputStream = new ZipOutputStream(outputStream);
    }
    
    public long addFileToZipStream(DataFile dataFile) throws IOException {
        if (zipOutputStream == null) {
            openZipStream();
        }
        
        boolean createManifest = fileManifest != null;
        
        DataAccessRequest daReq = new DataAccessRequest();

        StorageIO<DataFile> accessObject = DataAccess.getStorageIO(dataFile, daReq);

        if (accessObject != null) {
            accessObject.open();
            long byteSize = 0;

            String fileName = accessObject.getFileName();
            String mimeType = accessObject.getMimeType();
            if (mimeType == null || mimeType.equals("")) {
                mimeType = "application/octet-stream";
            }

            //if (sizeTotal + fileSize < sizeLimit) {
            Boolean Success = true;

            InputStream instream = accessObject.getInputStream();
            if (instream == null) {
                if (createManifest) {
                    addToManifest(fileName
                            + " (" + mimeType
                            + ") COULD NOT be downloaded because an I/O error has occured. \r\n");
                }

                Success = false;
            } else {
                String zipEntryName = checkZipEntryName(fileName);
                ZipEntry e = new ZipEntry(zipEntryName);
                logger.fine("created new zip entry for " + zipEntryName);
                // support for categories: (not yet implemented)
                //String zipEntryDirectoryName = file.getCategory(versionNum);
                //ZipEntry e = new ZipEntry(zipEntryDirectoryName + "/" + zipEntryName);

                zipOutputStream.putNextEntry(e);

                // before writing out any bytes from the input stream, flush
                // any extra content, such as the variable header for the 
                // subsettable files:
                String varHeaderLine = accessObject.getVarHeader();
                if (varHeaderLine != null) {
                    zipOutputStream.write(varHeaderLine.getBytes());
                    byteSize += (varHeaderLine.getBytes().length);
                }

                byte[] data = new byte[8192];

                int i = 0;
                while ((i = instream.read(data)) > 0) {
                    zipOutputStream.write(data, 0, i);
                    logger.fine("wrote " + i + " bytes;");

                    byteSize += i;
                    zipOutputStream.flush();
                }
                instream.close();
                zipOutputStream.closeEntry();
                logger.fine("closed zip entry for " + zipEntryName);

                if (createManifest) {
                    addToManifest(zipEntryName + " (" + mimeType + ") " + byteSize + " bytes.\r\n");
                }

                if (byteSize > 0) {
                    zippedFilesList.add(dataFile.getId());
                }
            }
            //} else if (createManifest) {
            //    addToManifest(fileName + " (" + mimeType + ") " + " skipped because the total size of the download bundle exceeded the limit of " + sizeLimit + " bytes.\r\n");
            //}
            return byteSize;
        }
        return 0L;
    }
    
    public void finalizeZipStream() throws IOException {
        boolean createManifest = fileManifest != null;
        
        if (zipOutputStream == null) {
            openZipStream();
        }
        
        if (createManifest) {
            ZipEntry e = new ZipEntry("MANIFEST.TXT");

            zipOutputStream.putNextEntry(e);
            zipOutputStream.write(fileManifest.getBytes());
            zipOutputStream.closeEntry();
        }

        zipOutputStream.flush();
        zipOutputStream.close();
    }
    
    public void addToManifest(String manifestEntry) {
        this.fileManifest = this.fileManifest + manifestEntry; 
    }
    
    // check for and process duplicates:
    private String checkZipEntryName(String originalName) {
        String name = originalName;
        int fileSuffix = 1;
        int extensionIndex = originalName.lastIndexOf(".");

        while (fileNameList.contains(name)) {
            if (extensionIndex != -1) {
                name = originalName.substring(0, extensionIndex) + "_" + fileSuffix++ + originalName.substring(extensionIndex);
            } else {
                name = originalName + "_" + fileSuffix++;
            }
        }
        fileNameList.add(name);
        return name;
    }
}
