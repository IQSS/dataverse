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
    
    public static boolean isThumbnailAvailable (DataFile file) {
        return isThumbnailAvailable(file, DEFAULT_THUMBNAIL_SIZE);
    }
    
    public static boolean isThumbnailAvailable (DataFile file, int size) {
        if (file == null) {
            return false; 
        }
        
        String imageThumbFileName = null;

        if (file.getContentType().substring(0, 6).equalsIgnoreCase("image/")) {
            imageThumbFileName = generateImageThumb(file.getFileSystemLocation().toString(), size);
        } else if (file.getContentType().equalsIgnoreCase("application/pdf")) {
            imageThumbFileName = generatePDFThumb(file.getFileSystemLocation().toString(), size);
        } else if (file.getContentType().equalsIgnoreCase("application/zipped-shapefile")) {
            imageThumbFileName = generateWorldMapThumb(file.getFileSystemLocation().toString(), size);
        }
        
        if (imageThumbFileName != null) {
            return true; 
        }
        
        return false; 
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
        } else if (file.getContentType().equalsIgnoreCase("application/zipped-shapefile")) {
            imageThumbFileName = generateWorldMapThumb(file.getFileSystemLocation().toString(), size);
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
	    Image thumbImage = fullSizeImage.getScaledInstance(thumbWidth, thumbHeight, java.awt.Image.SCALE_FAST);
            //logger.fine("Finished image rescaling.");

            // if transparency is defined, we should preserve it in the png:
            /*   
            OK, turns out *nothing* special needs to be done in order to preserve
            the transparency; the transparency is already there, because ImageIO.read()
            creates a BufferedImage with the color type BufferedImage.TYPE_INT_ARGB;
            all we need to do, is to create the output BufferedImage lowRes, 
            below, with this same color type. The transparency was getting lost 
            only because that BufferedImage was made with TYPE_INT_RGB, thus
            stripping the transparency off.
            
            BufferedImage bufferedImageForTransparency = new BufferedImage(thumbWidth, thumbgetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bufferedImageForTransparency.createGraphics();
            g2.drawImage(thumbImage, 0, 0, null);
            g2.dispose();
            
            int color = bufferedImageForTransparency.getRGB(0, 0);
            
            logger.info("color we'll be using for transparency: "+color);
            
            thumbImage = makeColorTransparent(bufferedImageForTransparency, new Color(color));
            */
            
            ImageWriter writer = null;
            Iterator iter = ImageIO.getImageWritersByFormatName("png");
            if (iter.hasNext()) {
                writer = (ImageWriter) iter.next();
            } else {
                return null;
            }

            BufferedImage lowRes = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = lowRes.createGraphics();
            g2.drawImage(thumbImage, 0, 0, null);
            g2.dispose();

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
            // something went wrong...
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
    
    public static String generateWorldMapThumb(String fileLocation, int size) {
        String thumbFileLocation = fileLocation + ".img.thumb" + size;

        // see if the thumb is already generated and saved:

        if (new File(thumbFileLocation).exists()) {
            return thumbFileLocation;
        }
        
        // if not, see if the full-size image has already been generated for this
        // WM image. 
        // (we can't generate it here, on demand; it gets generated the first 
        // time the world map app is called for this file!)
        // if it does exist, we can try to generate a thumbnail, using the 
        // normal image thumb method:
        
        String worldMapImageLocation = fileLocation + ".img";
        //logger.info ("worldMapImageLocation: " + worldMapImageLocation);
        if (new File(worldMapImageLocation).exists()) {
            return generateImageThumb(worldMapImageLocation, size);
        }
        
        // nothing we can do, sorry.
        
        return null; 
    }
    
    /*
       The method below takes a BufferedImage, and makes the specified color
       transparent. Turns out we don't really need to do this explicitly, since 
       the original transparency can easily be preserved. 
    
    private static Image makeColorTransparent(final BufferedImage im, final Color color) {
        final ImageFilter filter = new RGBImageFilter() {
            // the color we are looking for (white)... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFFFFFFFF;

            public final int filterRGB(final int x, final int y, final int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                } else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        final ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }
    */
}
