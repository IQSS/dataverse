/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.BufferedInputStream;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.QueryParam;



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
    DatasetServiceBean datasetService; 
    @EJB
    IngestServiceBean ingestService;

    //@EJB
    
    @Path("test/file")
    @GET
    @Produces({ "text/plain" })
    public String datafile(@QueryParam("fileName") String fileName, @QueryParam("fileType") String fileType, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        String output = "";

        if (StringUtil.isEmpty(fileName) || StringUtil.isEmpty(fileType)) {
            output = output.concat("Usage: /api/ingest/test/file?fileName=PATH&fileType=TYPE");
            return output; 
        }
        
        BufferedInputStream fileInputStream = null; 
        
        
        try {
            fileInputStream = new BufferedInputStream(new FileInputStream(new File(fileName)));
        } catch (FileNotFoundException notfoundEx) {
            fileInputStream = null; 
        }
        
        if (fileInputStream == null) {
            output = output.concat("Could not open file "+fileName+".");
            return output;
        }
        
        TabularDataFileReader ingestPlugin = ingestService.getTabDataReaderByMimeType(fileType);

        if (ingestPlugin == null) {
            output = output.concat("Could not locate an ingest plugin for type "+fileType+".");
            return output;
        }
        
        TabularDataIngest tabDataIngest = null;
        
        try {
            tabDataIngest = ingestPlugin.read(fileInputStream, false, null);
        } catch (IOException ingestEx) {
            output = output.concat("Caught an exception trying to ingest file " + fileName + ": " + ingestEx.getLocalizedMessage());
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
                    dataFile.setStorageIdentifier(tabFilename);
                    Dataset dataset = new Dataset();
                    dataFile.setOwner(dataset);
                    
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