/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.BufferedInputStream;
import java.util.logging.Logger;
import javax.ejb.EJB;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.servlet.http.HttpServletResponse;



/**
 *
 * @author Leonid Andreev
 * 
 * This API call was originally created for batch-testing 4.0 ingest. 
 * It runs the ingest code that creates the dataverse data objects - datavariables,
 * datatable, etc. and generates a report with variable metadata - names, 
 * types, UNFs, but doesn't persist the objects in the database. 
 * It was put together as a bit of a hack - but we may have a place for it in 
 * the application - Gary has requested a mechanism for producing UNFs without
 * actually ingesting the file (for sensitive data, etc.).
 * So we'll probably beef up this API call a little bit (make it upload the 
 * file, etc.) and make a simple UI for it. 
 * 
 *      -- L.A. Aug. 2014 
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
        
        String absoluteFilePath = null; 
        if (fileType.equals("x-stata")) {
            absoluteFilePath = "/usr/share/data/retest_stata/reingest/" + fileName;
        } else if (fileType.equals("x-spss-sav")) {
            absoluteFilePath = "/usr/share/data/retest_sav/reingest/" + fileName;
        } else if (fileType.equals("x-spss-por")) {
            absoluteFilePath = "/usr/share/data/retest_por/reingest/" + fileName; 
        }
        
        try {
            fileInputStream = new BufferedInputStream(new FileInputStream(new File(absoluteFilePath)));
        } catch (FileNotFoundException notfoundEx) {
            fileInputStream = null; 
        }
        
        if (fileInputStream == null) {
            output = output.concat("Could not open file "+absoluteFilePath+".");
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

                    String tabFilename = FileUtil.replaceExtension(absoluteFilePath, "tab");
                    
                    java.nio.file.Files.copy(Paths.get(tabFile.getAbsolutePath()), Paths.get(tabFilename), StandardCopyOption.REPLACE_EXISTING);
                    
                    DataTable dataTable = tabDataIngest.getDataTable();
                    
                    DataFile dataFile = new DataFile();
                    dataFile.setStorageIdentifier(tabFilename);
                    
                    FileMetadata fileMetadata = new FileMetadata();
                    fileMetadata.setLabel(fileName);
                    
                    dataFile.setDataTable(dataTable);
                    dataTable.setDataFile(dataFile);
                    
                    fileMetadata.setDataFile(dataFile);
                    dataFile.getFileMetadatas().add(fileMetadata);
                    
                    output = output.concat ("NVARS: "+dataTable.getVarQuantity()+"\n");
                    output = output.concat ("NOBS: "+dataTable.getCaseQuantity()+"\n");
                    
                    try {
                        ingestService.produceSummaryStatistics(dataFile, tabFile);
                        output = output.concat ("UNF: "+dataTable.getUnf()+"\n");
                    } catch (IOException ioex) {
                        output = output.concat ("UNF: failed to calculate\n"+"\n");
                    }
                    
                    for (int i = 0; i < dataTable.getVarQuantity(); i++) {
                        String vartype = "";
                        
                        //if ("continuous".equals(dataTable.getDataVariables().get(i).getVariableIntervalType().getName())) {
                        if (dataTable.getDataVariables().get(i).isIntervalContinuous()) {
                            vartype = "numeric-continuous";
                        } else {
                            if (dataTable.getDataVariables().get(i).isTypeNumeric()) {
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