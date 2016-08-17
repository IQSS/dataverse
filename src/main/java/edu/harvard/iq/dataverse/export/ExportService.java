
package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
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
            temp[1] = e.getProviderName();
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
            //ex.printStackTrace();
            return null;
        }
        return null;

    }
    
    
    // This method goes through all the Exporters and calls 
    // the "chacheExport()" method that will save the produced output  
    // in a file in the dataset directory, on each Exporter available. 
    
    public void exportAllFormats (Dataset dataset) throws ExportException {
        clearAllCachedFormats(dataset);
        
        try {
            DatasetVersion releasedVersion = dataset.getReleasedVersion();
            if (releasedVersion == null) {
                throw new ExportException("No released version for dataset "+dataset.getGlobalId());
            }
            final JsonObjectBuilder datasetAsJsonBuilder = jsonAsDatasetDto(releasedVersion);
            JsonObject datasetAsJson = datasetAsJsonBuilder.build();
            
            Iterator<Exporter> exporters = loader.iterator();
            while ( exporters.hasNext()) {
                Exporter e = exporters.next();
                String formatName = e.getProviderName(); 
                
                cacheExport(releasedVersion, formatName, datasetAsJson, e);
                
            }
        } catch (ServiceConfigurationError serviceError) {
            throw new ExportException("Service configuration error during export. "+serviceError.getMessage());
        }
        // Finally, if we have been able to successfully export in all available 
        // formats, we'll increment the "last exported" time stamp: 
        
        dataset.setLastExportTime(new Timestamp(new Date().getTime()));
        
    }
    
    public void clearAllCachedFormats(Dataset dataset) {
        Iterator<Exporter> exporters = loader.iterator();
        while (exporters.hasNext()) {
            Exporter e = exporters.next();
            String formatName = e.getProviderName();
            
            clearCachedExport(dataset, formatName);
        }
        
        dataset.setLastExportTime(null);
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
                if (e.getProviderName().equals(formatName)) {
                    DatasetVersion releasedVersion = dataset.getReleasedVersion();
                    if (releasedVersion == null) {
                        throw new IllegalStateException("No Released Version");
                    }
                    final JsonObjectBuilder datasetAsJsonBuilder = jsonAsDatasetDto(releasedVersion);
                    cacheExport(releasedVersion, formatName, datasetAsJsonBuilder.build(), e);
                }
            }
        } catch (ServiceConfigurationError serviceError) {
            throw new ExportException("Service configuration error during export. " + serviceError.getMessage());
        } catch (IllegalStateException e) {
            throw new ExportException("No published version found during export. " + dataset.getGlobalId());
        }
    }
    
    public Exporter getExporter(String formatName) throws ExportException {
        try {
            Iterator<Exporter> exporters = loader.iterator();
            while (exporters.hasNext()) {
                Exporter e = exporters.next();
                if (e.getProviderName().equals(formatName)) {
                    return e;
                }
            }
        } catch (ServiceConfigurationError serviceError) {
            throw new ExportException("Service configuration error during export. " + serviceError.getMessage());
        } catch (Exception ex) {
            throw new ExportException("Could not find Exporter \""+formatName+"\", unknown exception");
        }
        throw new ExportException("No such Exporter: "+formatName);
    }
    
    // This method runs the selected metadata exporter, caching the output 
    // in a file in the dataset dirctory:
    private void cacheExport(DatasetVersion version, String format, JsonObject datasetAsJson, Exporter exporter) throws ExportException {
        try {
            if (version.getDataset().getFileSystemDirectory() != null && !Files.exists(version.getDataset().getFileSystemDirectory())) {
                /* Note that "createDirectories()" must be used - not 
                     * "createDirectory()", to make sure all the parent 
                     * directories that may not yet exist are created as well. 
                 */

                Files.createDirectories(version.getDataset().getFileSystemDirectory());
            }

            Path cachedMetadataFilePath = Paths.get(version.getDataset().getFileSystemDirectory().toString(), "export_" + format + ".cached");
            FileOutputStream cachedExportOutputStream = new FileOutputStream(cachedMetadataFilePath.toFile());
            exporter.exportDataset(version, datasetAsJson, cachedExportOutputStream);
            cachedExportOutputStream.flush();
            cachedExportOutputStream.close();

        } catch (IOException ioex) {
            throw new ExportException("IO Exception thrown exporting as " + format);
        }

    }
    
    private void clearCachedExport(Dataset dataset, String format) {
        if (dataset != null && dataset.getFileSystemDirectory() != null && Files.exists(dataset.getFileSystemDirectory())) {

            Path cachedMetadataFilePath = Paths.get(dataset.getFileSystemDirectory().toString(), "export_" + format + ".cached");
            try {
                Files.delete(cachedMetadataFilePath);
            } catch (IOException ioex) {
            }
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
    
    public Long getCachedExportSize(Dataset dataset, String formatName) {
        try {
            if (dataset.getFileSystemDirectory() != null) {
                Path cachedMetadataFilePath = Paths.get(dataset.getFileSystemDirectory().toString(), "export_" + formatName + ".cached");
                if (Files.exists(cachedMetadataFilePath)) {
                    return cachedMetadataFilePath.toFile().length();
                }
            }
        } catch (Exception ioex) {
            // don't do anything - we'll just return null
        }

        return null;
    }
    
    
    public Boolean isXMLFormat(String provider){
        try {
            Iterator<Exporter> exporters = loader.iterator();
            while (exporters.hasNext()) {
                Exporter e = exporters.next();
                if (e.getProviderName().equals(provider)) {
                    return e.isXMLFormat();
                }
            }
        } catch (ServiceConfigurationError serviceError) {
            serviceError.printStackTrace();
        }
        return null;       
    }
 
}
