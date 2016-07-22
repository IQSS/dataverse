
package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.SystemConfig;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.jsonAsDatasetDto;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author skraffmi
 */
public class ExportService {
    
    private static ExportService service;
    private ServiceLoader<Exporter> loader;
    private static SystemConfig systemConfig; 

    private ExportService() {
        loader = ServiceLoader.load(Exporter.class);        
    }

    public static synchronized ExportService getInstance() {
        if (service == null) {
            service = new ExportService();
        } else{
            service.loader.reload();
        }
        return service;
    }
                
    public List< String[]> getExportersLabels() {
        List<String[]> retList = new ArrayList();
        Iterator<Exporter> exporters = ExportService.getInstance().loader.iterator();
        while (exporters.hasNext()) {
            Exporter e = exporters.next();
            String[] temp = new String[2];
            temp[0] = e.getDisplayName();
            temp[1] = e.getProvider();
            retList.add(temp);
        }
        return retList;
    }

    public InputStream getExport(Dataset dataset, String formatName) throws ExportException {
        // first we will try to locate an already existing, cached export 
        // for this format: 
        InputStream exportInputStream = getCachedExportFormat(dataset, formatName);
        
        if (exportInputStream != null) {
            return exportInputStream;
        }
        
        // if it doesn't exist, we'll try to run the export: 
        
        exportFormat(dataset, formatName);
        
        // and then try again: 
        
        exportInputStream = getCachedExportFormat(dataset, formatName);
        
        if (exportInputStream != null) {
            return exportInputStream;
        }
        
        // if there is no cached export still - we have to give up and throw 
        // an exception!
        
        throw new ExportException("Failed to export the dataset as "+formatName);
        
    }
    
    public String getExportAsString(Dataset dataset, String formatName) {
        try {
            InputStream inputStream = getExport(dataset, formatName);
            if (inputStream != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append('\n');
                }
                br.close();
                return sb.toString();
            }
        } catch (Exception ex) {
            return null;
        }
        return null;

    }
    
    
    // This method goes through all the Exporters and calls 
    // the "chacheExport()" method that will save the produced output  
    // in a file in the dataset directory, on each Exporter available. 
    
    public void exportAllFormats (Dataset dataset) throws ExportException {
        try {
            final JsonObjectBuilder datasetAsJsonBuilder = jsonAsDatasetDto(dataset.getLatestVersion());
            JsonObject datasetAsJson = datasetAsJsonBuilder.build();
            
            Iterator<Exporter> exporters = loader.iterator();
            while ( exporters.hasNext()) {
                Exporter e = exporters.next();
                String formatName = e.getProvider(); 
                
                // the DDI exporter needs this Dataverse's preferred url 
                // in order to cook the datafile access URLs in otherMat and 
                // fileDscr sections. 
                // yeah, this is a hack - but I can't immediately think of a 
                // better solution
                if ("DDI".equals(e.getProvider())) {
                    if (systemConfig != null) {
                        e.setParam("dataverse_site_url", systemConfig.getDataverseSiteUrl());
                    }
                }
                cacheExport(dataset, formatName, datasetAsJson, e);
                
            }
        } catch (ServiceConfigurationError serviceError) {
            serviceError.printStackTrace();
            throw new ExportException("Service configuration error during export. "+serviceError.getMessage());
        }
        // Finally, if we have been able to successfully export in all available 
        // formats, we'll increment the "last exported" time stamp: 
        
        dataset.setLastExportTime(new Timestamp(new Date().getTime()));
        
    }
    
    @TransactionAttribute(REQUIRES_NEW)
    public void exportAllFormatsInNewTransaction(Dataset dataset) {
        try {
            exportAllFormats(dataset);
        } catch (ExportException ee) {}
    }
           
    
    
    
    // This method finds the exporter for the format requested, 
    // then produces the dataset metadata as a JsonObject, then calls
    // the "chacheExport()" method that will save the produced output  
    // in a file in the dataset directory. 
    
    public void exportFormat(Dataset dataset, String formatName) throws ExportException {
        try {
            Iterator<Exporter> exporters = loader.iterator();
            while (exporters.hasNext()) {
                Exporter e = exporters.next();
                if (e.getProvider().equals(formatName)) {
                    final JsonObjectBuilder datasetAsJsonBuilder = jsonAsDatasetDto(dataset.getLatestVersion());
                    // the DDI exporter needs this Dataverse's preferred url 
                    // in order to cook the datafile access URLs in otherMat and 
                    // fileDscr sections. 
                    // yeah, this is a hack - but I can't immediately think of a 
                    // better solution
                    if ("DDI".equals(e.getProvider())) {
                        if (systemConfig != null) {
                            e.setParam("dataverse_site_url", systemConfig.getDataverseSiteUrl());
                        }
                    }
                    cacheExport(dataset, formatName, datasetAsJsonBuilder.build(), e);
                }
            }
        } catch (ServiceConfigurationError serviceError) {
            serviceError.printStackTrace();
            throw new ExportException("Service configuration error during export. " + serviceError.getMessage());
        }
    }
    
    // This method runs the selected metadata exporter, caching the output 
    // in a file in the dataset dirctory:
    private void cacheExport(Dataset dataset, String format, JsonObject datasetAsJson, Exporter exporter) throws ExportException {
        try {
            if (dataset.getFileSystemDirectory() != null && !Files.exists(dataset.getFileSystemDirectory())) {
                /* Note that "createDirectories()" must be used - not 
                     * "createDirectory()", to make sure all the parent 
                     * directories that may not yet exist are created as well. 
                 */

                Files.createDirectories(dataset.getFileSystemDirectory());
            }

            Path cachedMetadataFilePath = Paths.get(dataset.getFileSystemDirectory().toString(), "export_" + format + ".cached");
            FileOutputStream cachedExportOutputStream = new FileOutputStream(cachedMetadataFilePath.toFile());
            exporter.exportDataset(datasetAsJson, cachedExportOutputStream);
            cachedExportOutputStream.flush();
            cachedExportOutputStream.close();

        } catch (IOException ioex) {
            throw new ExportException("IO Exception thrown exporting as " + format);
        }

    }
    
    
    // This method checks if the metadata has already been exported in this 
    // format and cached on disk. If it has, it'll open the file and retun 
    // the file input stream. If not, it'll return null. 
    
    private InputStream getCachedExportFormat(Dataset dataset, String formatName) {

        try {
            if (dataset.getFileSystemDirectory() != null) {
                Path cachedMetadataFilePath = Paths.get(dataset.getFileSystemDirectory().toString(), "export_" + formatName + ".cached");
                if (Files.exists(cachedMetadataFilePath)) {
                    FileInputStream cachedExportInputStream = new FileInputStream(cachedMetadataFilePath.toFile());
                    return cachedExportInputStream;
                }
            }
        } catch (IOException ioex) {
            // don't do anything - we'll just return null
        }

        return null;

    }
    
    
    public Boolean isXMLFormat(String provider){
        try {
            Iterator<Exporter> exporters = loader.iterator();
            while (exporters.hasNext()) {
                Exporter e = exporters.next();
                if (e.getProvider().equals(provider)) {
                    return e.isXMLFormat();
                }
            }
        } catch (ServiceConfigurationError serviceError) {
            serviceError.printStackTrace();
        }
        return null;       
    }
    
    public void setSystemConfig(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }
    
    public SystemConfig getSystemConfig() {
        return this.systemConfig;
    }
 
}
