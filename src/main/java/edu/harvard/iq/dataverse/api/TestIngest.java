/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.dataaccess.OptionalAccessService;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.BufferedInputStream;

import java.lang.reflect.Type;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.Properties;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import javax.servlet.http.HttpServletResponse;

/*
    Custom API exceptions [NOT YET IMPLEMENTED]
import edu.harvard.iq.dataverse.api.exceptions.NotFoundException;
import edu.harvard.iq.dataverse.api.exceptions.ServiceUnavailableException;
import edu.harvard.iq.dataverse.api.exceptions.PermissionDeniedException;
import edu.harvard.iq.dataverse.api.exceptions.AuthorizationRequiredException;
*/

/**
 *
 * @author Leonid Andreev
 * 
 * The data (file) access API is based on the DVN access API v.1.0 (that came 
 * with the v.3.* of the DVN app) and extended for DVN 4.0 to include some
 * extra fancy functionality, such as subsetting individual columns in tabular
 * data files and more.
 */

@Path("ingest")
public class TestIngest {
    private static final Logger logger = Logger.getLogger(TestIngest.class.getCanonicalName());
    
    @EJB
    DataFileServiceBean dataFileService;
    @EJB 
    DatasetServiceBean datasetService; 
    @EJB
    IngestServiceBean ingestService;

    //@EJB
    
    @Path("test/{fileName}/{fileType}")
    @GET
    @Produces({ "text/plain" })
    public String datafile(@PathParam("fileName") String fileName, @PathParam("fileType") String fileType, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        String output = "";

        if (fileName == null || fileType == null || "".equals(fileName) || "".equals(fileType)) {
            output = output.concat("Usage: java edu.harvard.iq.dataverse.ingest.IngestServiceBean <file> <type>.");
            return output; 
        }
        
        BufferedInputStream fileInputStream = null; 
        
        if (fileType.equals("x-stata")) {
            fileName = "/usr/share/data/retest_stata/reingest/" + fileName;
        } else if (fileType.equals("x-spss-sav")) {
            fileName = "/usr/share/data/retest_sav/reingest/" + fileName;
        }
        
        try {
            fileInputStream = new BufferedInputStream(new FileInputStream(new File(fileName)));
        } catch (FileNotFoundException notfoundEx) {
            fileInputStream = null; 
        }
        
        if (fileInputStream == null) {
            output = output.concat("Could not open file "+fileName+".");
            return output;
        }
        
        fileType = "application/"+fileType; 
        TabularDataFileReader ingestPlugin = ingestService.getTabDataReaderByMimeType(fileType);

        if (ingestPlugin == null) {
            output = output.concat("Could not locate an ingest plugin for type "+fileType+".");
            return output;
        }
        
        TabularDataIngest tabDataIngest = null;
        
        try {
            tabDataIngest = ingestPlugin.read(fileInputStream, null);
        } catch (IOException ingestEx) {
            output = output.concat("Caught an exception trying to ingest file "+fileName+".");
            return output;
        }
        
        try {
            if (tabDataIngest != null) {
                File tabFile = tabDataIngest.getTabDelimitedFile();

                if (tabDataIngest.getDataTable() != null
                        && tabFile != null
                        && tabFile.exists()) {

                    String tabFilename = FileUtil.replaceExtension(fileName, "tab");
                    
                    java.nio.file.Files.copy(Paths.get(tabFile.getAbsolutePath()), Paths.get(tabFilename), StandardCopyOption.REPLACE_EXISTING);
                    
                    DataTable dataTable = tabDataIngest.getDataTable();
                    
                    DataFile dataFile = new DataFile();
                    dataFile.setFileSystemName(tabFilename);
                    
                    dataFile.setDataTable(dataTable);
                    dataTable.setDataFile(dataFile);
                    
                    output = output.concat ("NVARS: "+dataTable.getVarQuantity()+"\n");
                    output = output.concat ("NOBS: "+dataTable.getCaseQuantity()+"\n");
                    
                    try {
                        ingestService.produceSummaryStatistics(dataFile);
                        output = output.concat ("UNF: "+dataTable.getUnf()+"\n");
                    } catch (IOException ioex) {
                        output = output.concat ("UNF: failed to calculate\n"+"\n");
                    }
                    
                    for (int i = 0; i < dataTable.getVarQuantity(); i++) {
                        String vartype = "";
                        
                        if ("continuous".equals(dataTable.getDataVariables().get(i).getVariableIntervalType().getName())) {
                            vartype = "numeric-continuous";
                        } else {
                            if ("numeric".equals(dataTable.getDataVariables().get(i).getVariableFormatType().getName())) {
                                vartype = "numeric-discrete";
                            } else {
                                String formatCategory = dataTable.getDataVariables().get(i).getFormatCategory();
                                if ("time".equals(formatCategory)) {
                                    vartype = "character-time";
                                } else if ("date".equals(formatCategory)) {
                                    vartype = "character-date";
                                } else {
                                    vartype = "character";
                                }
                            }
                        }
                        
                        output = output.concat ("VAR"+i+" ");
                        output = output.concat (dataTable.getDataVariables().get(i).getName()+" ");
                        output = output.concat (vartype+" ");
                        output = output.concat (dataTable.getDataVariables().get(i).getUnf());
                        output = output.concat ("\n"); 
                        
                    }
                
                } else {
                    output = output.concat("Ingest failed to produce tab file or data table for file "+fileName+".");
                    return output;
                }
            } else {
                output = output.concat("Ingest resulted in a null tabDataIngest object for file "+fileName+".");
                return output;
            }
        } catch (IOException ex) {
            output = output.concat("Caught an exception trying to save ingested data for file "+fileName+".");
            return output;
        }
        
        return output;
    }
    
    
}