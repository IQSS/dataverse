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
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import edu.harvard.iq.dataverse.DataFile;
import java.util.logging.Logger;

/**
 *
 * @author Leonid Andreev
 */
public class ImageThumbConverter {
    public static int DEFAULT_CARDIMAGE_SIZE = 48;
    public static int DEFAULT_THUMBNAIL_SIZE = 64; 
    public static int DEFAULT_PREVIEW_SIZE = 400; 
    
    private static final Logger logger = Logger.getLogger(ImageThumbConverter.class.getCanonicalName());
    
    public ImageThumbConverter() {
    }
    
    public static FileAccessObject getImageThumb (DataFile file, FileAccessObject fileDownload) {
        return getImageThumb (file, fileDownload, DEFAULT_THUMBNAIL_SIZE);
    }
    
    public static FileAccessObject getImageThumb(DataFile file, FileAccessObject fileDownload, int size) {
        String imageThumbFileName = null;

        if (file != null && file.getContentType().substring(0, 6).equalsIgnoreCase("image/")) {
            imageThumbFileName = generateImageThumb(file.getFileSystemLocation().toString(), size);
        } else if (file != null && file.getContentType().equalsIgnoreCase("application/pdf")) {
            imageThumbFileName = generatePDFThumb(file.getFileSystemLocation().toString(), size);
        }
        
        if (imageThumbFileName != null) {
            File imageThumbFile = new File(imageThumbFileName);

            if (imageThumbFile != null && imageThumbFile.exists()) {

                fileDownload.closeInputStream();
                fileDownload.setSize(imageThumbFile.length());

                InputStream imageThumbInputStream = null;

                try {

                    imageThumbInputStream = new FileInputStream(imageThumbFile);
                } catch (IOException ex) {
                    return null;
                }

                if (imageThumbInputStream != null) {
                    fileDownload.setInputStream(imageThumbInputStream);
                    fileDownload.setIsLocalFile(true);

                    fileDownload.setMimeType("image/png");
                } else {
                    return null;
                }
            }
        }

        return fileDownload;
    }
    
    public static String generateImageThumb(String fileLocation) {
        return generateImageThumb(fileLocation, DEFAULT_THUMBNAIL_SIZE);
    }
    
    public static String generateImageThumb(String fileLocation, int size) {

        String thumbFileLocation = fileLocation + ".thumb" + size;

        // see if the thumb is already generated and saved:

        if (new File(thumbFileLocation).exists()) {
            return thumbFileLocation;
        }

        // let's attempt to generate the thumb:

        /*
         * update: 
         * Note that the thumb size has been made configurable, as of 3.6.1
         * - needs to be added here. 
        */

        /* 
         * Skipping ImageMagick hack, for now (as in, 4alpha1) -- L.A. 
         *
        if (new File("/usr/bin/convert").exists()) {

            String ImageMagick = "/usr/bin/convert -size 64x64 " + fileLocation + " -resize 64 -flatten png:" + thumbFileLocation;
            int exitValue = 1;

            try {
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec(ImageMagick);
                exitValue = process.waitFor();
            } catch (Exception e) {
                exitValue = 1;
            }

            if (exitValue == 0) {
                return true;
            }
        }
         */

        // For whatever reason, creating the thumbnail with ImageMagick
        // has failed.
        // Let's try again, this time with Java's standard Image
        // library:

        try {
            BufferedImage fullSizeImage = ImageIO.read(new File(fileLocation));

	    if ( fullSizeImage == null ) {
		return null; 
	    }

            int width = fullSizeImage.getWidth(null);
            int height = fullSizeImage.getHeight(null);
            
            logger.fine("image dimensions: "+width+"x"+height);
            
            double scaleFactor = 0.0;
            int thumbHeight = size; 
            int thumbWidth = size; 
            
            if (width > height) {
                scaleFactor = ((double) size) / (double) width;
                thumbHeight = (int) (height * scaleFactor);
            } else {
                scaleFactor = ((double) size) / (double) height;
                thumbWidth = (int) (width * scaleFactor);
            }
            
            logger.fine("scale factor: "+scaleFactor);
            logger.fine("thumbnail dimensions: "+thumbWidth+"x"+thumbHeight);



	    // We are willing to spend a few extra CPU cycles to generate
	    // better-looking thumbnails, hence the SCALE_SMOOTH flag. 
	    // SCALE_FAST would trade quality for speed. 

            //logger.fine("Start image rescaling ("+size+" pixels), SCALE_FAST used;");
	    java.awt.Image thumbImage = fullSizeImage.getScaledInstance(thumbWidth, thumbHeight, java.awt.Image.SCALE_FAST);
            //logger.fine("Finished image rescaling.");

            ImageWriter writer = null;
            Iterator iter = ImageIO.getImageWritersByFormatName("png");
            if (iter.hasNext()) {
                writer = (ImageWriter) iter.next();
            } else {
                return null;
            }

            BufferedImage lowRes = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            lowRes.getGraphics().drawImage(thumbImage, 0, 0, null);

            ImageOutputStream ios = ImageIO.createImageOutputStream(new File(thumbFileLocation));
            writer.setOutput(ios);

            // finally, save thumbnail image:
            writer.write(lowRes);
            writer.dispose();

            ios.close();
            thumbImage.flush();
            fullSizeImage.flush();
            lowRes.flush();
            return thumbFileLocation;
        } catch (Exception e) {
            // something went wrong, returning "false":
	    //dbgLog.fine("ImageIO: caught an exception while trying to generate a thumbnail for "+fileLocation);

            return null;
        }
    }
    
    public static String generatePDFThumb(String fileLocation) {
        return generatePDFThumb(fileLocation, DEFAULT_THUMBNAIL_SIZE);
    }
    
    public static String generatePDFThumb(String fileLocation, int size) {

        String thumbFileLocation = fileLocation + ".thumb" + size;

        // see if the thumb is already generated and saved:

        if (new File(thumbFileLocation).exists()) {
            return thumbFileLocation;
        }

        // doesn't exist yet, let's attempt to generate it:

	String imageMagickExec = System.getProperty("dataverse.path.imagemagick.convert");

        if ( imageMagickExec != null ) {
            imageMagickExec = imageMagickExec.trim();
        }
        
        // default location:
        
        if ( imageMagickExec == null || imageMagickExec.equals("") ) {
            imageMagickExec = "/usr/bin/convert";
        } 
        
        if (new File(imageMagickExec).exists()) {

            // Based on the lessons recently learned in production: 
            //  - use "-thumbnail" instead of "-resize";
            //  - use "-flatten"
            //  - use "-strip"
            //  - (maybe?) use jpeg instead of png - ?
            //
            // (what we observed in production - 3.6.3, June 2014 - was that 
            // for very large TIFF images, when processed wihout the options
            // above, thumbnails produced were still obscenely *huge*; for ex.,
            // for a 100MB 3000x4000 px. TIFF file, the 275 px. PNG thumbnail 
            // produced was 5MB! - which, for a page with multiple high-res 
            // images (Grad. School of Design dv) resulted in taking forever 
            // to load... JPG thumbnails, similarly produced, were smaller, but 
            // still unnecessarily large. I was never able to figure out what 
            // was going on (full-rez color profiles still stored in the 
            // resized version - ??), but the combination above takes care of 
            // it and brings down the thumbnail size to under 50K, were it 
            // belongs. :)
            //          -- L.A. June 2014
            
            String ImageMagick = imageMagickExec + " pdf:" + fileLocation + "[0] -thumbnail "+ size + "x" + size + " -flatten -strip png:" + thumbFileLocation;
            int exitValue = 1;

            try {
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec(ImageMagick);
                exitValue = process.waitFor();
            } catch (Exception e) {
                exitValue = 1;
            }

            if (exitValue == 0 && new File(thumbFileLocation).exists()) {
                return thumbFileLocation;
            }
        }

        return null; 
        
    }
}
