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

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Iterator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

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
    public static long DEFAULT_ZIPFILE_LIMIT = 10 * 1024 * 1024; // 10MB (?)
    
    private static final Logger logger = Logger.getLogger(DataFileZipper.class.getCanonicalName());
    
    public DataFileZipper() {
    }
    
    public void zipFiles(List<DataFile> files, OutputStream outstream) throws IOException {
        zipFiles(files, outstream, DEFAULT_ZIPFILE_LIMIT); 
    }
    
    public void zipFiles(List<DataFile> files, OutputStream outstream, long sizeLimit) throws IOException {
        String fileManifest = "";

        // check for restricted files
        //Iterator iter = files.iterator();
        //while (iter.hasNext()) {
        //    DataFile file = (DataFile) iter.next();
        //    if ( ... FILE RESTRICTED ... )) {
        //        fileManifest = fileManifest + file.getFileMetadata().getLabel() + " IS RESTRICTED AND CANNOT BE DOWNLOADED\r\n";
        //        iter.remove();
        //    }
        //}
        if (files.size() < 1) {
            throw new IOException("Empty files list.");
        }

        long sizeTotal = 0L;

        ZipOutputStream zout = new ZipOutputStream(outstream);

        List nameList = new ArrayList(); // to check for duplicates
        List successList = new ArrayList(); // to update download counts (not yet implemented)

        Iterator iter = files.iterator();

        while (iter.hasNext()) {
            int fileSize = 0;
            DataFile file = (DataFile) iter.next();

            DataAccessRequest daReq = new DataAccessRequest();
            DataAccessObject accessObject = DataAccess.createDataAccessObject(file, daReq);

            if (accessObject != null) {
                accessObject.open();

                String fileName = accessObject.getFileName();
                String mimeType = accessObject.getMimeType();
                if (mimeType == null || mimeType.equals("")) {
                    mimeType = "application/octet-stream";
                }

                if (sizeTotal < sizeLimit) {

                    Boolean Success = true;

                    InputStream instream = accessObject.getInputStream();
                    if (instream == null) {
                        fileManifest = fileManifest + fileName
                                + " (" + mimeType
                                + ") COULD NOT be downloaded because an I/O error has occured. \r\n";

                        Success = false;
                    } else {
                        String zipEntryName = checkZipEntryName(fileName, nameList);
                        ZipEntry e = new ZipEntry(zipEntryName);
                        logger.info("created new zip entry for " + zipEntryName);
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
                        while ((i = instream.read(data)) > 0) {
                            zout.write(data, 0, i);
                            logger.info("wrote " + i + " bytes;");

                            fileSize += i;
                            //zout.flush();
                        }
                        instream.close();
                        zout.closeEntry();
                        logger.info("clozed zip entry for " + zipEntryName);

                        fileManifest = fileManifest + zipEntryName + " (" + mimeType + ") " + fileSize + " bytes.\r\n";

                        if (fileSize > 0) {
                            successList.add(file.getId());
                            sizeTotal += Long.valueOf(fileSize);
                        }
                    }
                } else {
                    fileManifest = fileManifest + fileName + " (" + mimeType + ") " + " skipped because the total size of the download bundle exceeded the limit of " + sizeLimit + " bytes.\r\n";
                }
            }
        }

        // finally, let's create the manifest entry:
        ZipEntry e = new ZipEntry("MANIFEST.TXT");

        zout.putNextEntry(e);
        zout.write(fileManifest.getBytes());
        zout.closeEntry();

        zout.close();

    }

    // check for and process duplicates:
    private String checkZipEntryName(String originalName, List nameList) {
        String name = originalName;
        int fileSuffix = 1;
        int extensionIndex = originalName.lastIndexOf(".");

        while (nameList.contains(name)) {
            if (extensionIndex != -1) {
                name = originalName.substring(0, extensionIndex) + "_" + fileSuffix++ + originalName.substring(extensionIndex);
            } else {
                name = originalName + "_" + fileSuffix++;
            }
        }
        nameList.add(name);
        return name;
    }
}
