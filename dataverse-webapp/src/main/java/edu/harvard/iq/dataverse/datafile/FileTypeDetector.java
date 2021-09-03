package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.ingest.IngestableDataChecker;
import edu.harvard.iq.dataverse.util.JhoveFileType;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.ejb.EJBException;
import javax.enterprise.context.ApplicationScoped;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/**
 * our check is fairly weak (it appears to be hard to really
 * really recognize a FITS file without reading the entire
 * stream...), so in version 3.* we used to nsist on *both*
 * the ".fits" extension and the header check;
 * in 4.0, we'll accept either the extension, or the valid
 * magic header
 *
 */
@ApplicationScoped
public class FileTypeDetector {
    private static final Logger logger = LoggerFactory.getLogger(FileTypeDetector.class);

    private static final MimetypesFileTypeMap MIME_TYPE_MAP = new MimetypesFileTypeMap();

    /**
     * Detects file type based on file content and filename
     */
    public String determineFileType(File f, String fileName) throws IOException {
        String fileType = "application/octet-stream";

        // step 1:
        // Apply our custom methods to try and recognize data files that can be
        // converted to tabular data
        logger.debug("Attempting to identify potential tabular data files;");
        fileType = detectTabularFileType(f, fileType);
        logger.debug("determineFileType: tabular data checker found " + fileType);

        // step 2: check the mime type of this file with Jhove
        if (!isContentTypeSpecificEnough(fileType)) {
            JhoveFileType jw = new JhoveFileType();
            String jHovemimeType = jw.getFileMimeType(f);
            if (jHovemimeType != null) {
                // remove parameter (eg. text/plain; charset=US-ASCII -> text/plain)
                MediaType mediaType = MediaType.parse(jHovemimeType);
                fileType = mediaType.getBaseType().toString();
            }
        }

        // step 3: check the mime type of this file with Tika
        if (!isContentTypeSpecificEnough(fileType)) {
            fileType = new Tika().detect(f);
        }


        // step 3: Check if xml is an graphml xml
        if ("application/xml".equals(fileType) && isGraphMLFile(f)) {
            fileType = "text/xml-graphml";
        }
        
        // step 4:
        // if this is a compressed file - zip or gzip - we'll check the
        // file(s) inside the compressed stream and see if it's one of our
        // recognized formats that we want to support compressed:

        if ("application/x-gzip".equals(fileType) || "application/gzip".equals(fileType)) {
            logger.debug("we'll run additional checks on this gzipped file.");
            // We want to be able to support gzipped FITS files, same way as
            // if they were just regular FITS files:
            // (new FileInputStream() can throw a "filen not found" exception;
            // however, if we've made it this far, it really means that the
            // file does exist and can be opened)
            try (InputStream uncompressedIn = new GZIPInputStream(new FileInputStream(f))) {
                if (isFITSFile(uncompressedIn)) {
                    fileType = "application/fits-gzipped";
                }
            } catch (IOException e) {
                logger.warn("file {} does not seems to be a gzip", fileName);
            }
        }
        if ("application/zip".equals(fileType)) {

            // Is this a zipped Shapefile?
            // Check for shapefile extensions as described here: http://en.wikipedia.org/wiki/Shapefile

            ShapefileHandler shapefileHandler = new ShapefileHandler(f);
            if (shapefileHandler.containsShapefile()) {
                fileType = ShapefileHandler.SHAPEFILE_FILE_TYPE;
            }
        }
        
        // step 5:
        // Additional processing; if we haven't gotten much useful information
        // back from previous steps, we'll try and make an educated guess based on
        // the file extension:

        if (!isContentTypeSpecificEnough(fileType)) {
            
            logger.debug("Type by extension, for " + fileName + ": " + MIME_TYPE_MAP.getContentType(fileName));
            String fileTypeByExtension = MIME_TYPE_MAP.getContentType(fileName);
            if (!"application/octet-stream".equals(fileTypeByExtension)) {
                fileType = fileTypeByExtension;
                logger.debug("mime type recognized by extension: " + fileType);
            }
        }

        logger.debug("returning fileType " + fileType);
        return fileType;
    }

    public String detectTabularFileType(File file, String fallbackContentType) {
        IngestableDataChecker tabChecker = new IngestableDataChecker();
        return StringUtils.defaultString(tabChecker.detectTabularDataFormat(file), fallbackContentType);
    }
    
    // -------------------- PRIVATE --------------------

    private boolean isContentTypeSpecificEnough(String contentType) {
        return !"text/plain".equals(contentType) && !"application/octet-stream".equals(contentType);
    }

    /**
     * Custom method for identifying FITS files:
     * TODO:
     * the existing check for the "magic header" is very weak (see below);
     * it should probably be replaced by attempting to parse and read at
     * least the primary HDU, using the NOM fits parser.
     * -- L.A. 4.0 alpha
     */
    private boolean isFITSFile(InputStream ins) throws IOException {

        // number of header bytes read for identification:
        byte[] magicWord = "SIMPLE".getBytes(StandardCharsets.UTF_8);
        int magicWordLength = magicWord.length;

        byte[] b = new byte[magicWordLength];
        logger.debug("attempting to read " + magicWordLength + " bytes from the FITS format candidate stream.");
        IOUtils.read(ins, b);

        if (Arrays.equals(magicWord, b)) {
            logger.debug("yes, this is FITS file!");
            return true;
        }

        return false;
    }

    private boolean isGraphMLFile(File file) {
        boolean isGraphML = false;
        logger.debug("begin isGraphMLFile()");
        
        try (FileReader fileReader = new FileReader(file)) {
            XMLInputFactory xmlif = XMLInputFactory.newInstance();
            xmlif.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE);

            XMLStreamReader xmlr = xmlif.createXMLStreamReader(fileReader);
            for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (xmlr.getLocalName().equals("graphml")) {
                        String schema = xmlr.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance",
                                                               "schemaLocation");
                        logger.debug("schema = " + schema);
                        if (schema != null && schema.contains("http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd")) {
                            logger.debug("graphML is true");
                            isGraphML = true;
                        }
                    }
                    break;
                }
            }
        } catch (XMLStreamException e) {
            logger.debug("XML error - this is not a valid graphML file.");
            isGraphML = false;
        } catch (IOException e) {
            throw new EJBException(e);
        }
        logger.debug("end isGraphML()");
        return isGraphML;
    }
}
