package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import static edu.harvard.iq.dataverse.IdServiceBean.logger;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import static edu.harvard.iq.dataverse.dataaccess.DataAccess.getStorageIO;
import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import static edu.harvard.iq.dataverse.dataset.DatasetUtil.datasetLogoThumbnail;
import static edu.harvard.iq.dataverse.dataset.DatasetUtil.thumb48addedByImageThumbConverter;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author skraffmi
 */
public class ExportService {

    private static ExportService service;
    private ServiceLoader<Exporter> loader;
    static SettingsServiceBean settingsService;

    private ExportService() {
        loader = ServiceLoader.load(Exporter.class);
    }

    /**
     * @deprecated Use `getInstance(SettingsServiceBean settingsService)`
     * instead. For privacy reasons, we need to pass in settingsService so that
     * we can make a decision whether not not to exclude email addresses. No new
     * code should call this method and it would be nice to remove calls from
     * existing code.
     */
    @Deprecated
    public static synchronized ExportService getInstance() {
        return getInstance(null);
    }

    public static synchronized ExportService getInstance(SettingsServiceBean settingsService) {
        ExportService.settingsService = settingsService;
        if (service == null) {
            service = new ExportService();
        }
        return service;
    }

    public List< String[]> getExportersLabels() {
        List<String[]> retList = new ArrayList<>();
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

    public InputStream getExport(Dataset dataset, String formatName) throws ExportException, IOException {
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
        throw new ExportException("Failed to export the dataset as " + formatName);

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
        } catch (ExportException | IOException ex) {
            //ex.printStackTrace();
            return null;
        }
        return null;

    }

    // This method goes through all the Exporters and calls 
    // the "chacheExport()" method that will save the produced output  
    // in a file in the dataset directory, on each Exporter available. 
    public void exportAllFormats(Dataset dataset) throws ExportException {
        try {
            clearAllCachedFormats(dataset);
        } catch (IOException ex) {
            Logger.getLogger(ExportService.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            DatasetVersion releasedVersion = dataset.getReleasedVersion();
            if (releasedVersion == null) {
                throw new ExportException("No released version for dataset " + dataset.getGlobalId());
            }
            JsonPrinter jsonPrinter = new JsonPrinter(settingsService);
            final JsonObjectBuilder datasetAsJsonBuilder = jsonPrinter.jsonAsDatasetDto(releasedVersion);
            JsonObject datasetAsJson = datasetAsJsonBuilder.build();

            Iterator<Exporter> exporters = loader.iterator();
            while (exporters.hasNext()) {
                Exporter e = exporters.next();
                String formatName = e.getProviderName();

                cacheExport(releasedVersion, formatName, datasetAsJson, e);

            }
        } catch (ServiceConfigurationError serviceError) {
            throw new ExportException("Service configuration error during export. " + serviceError.getMessage());
        }
        // Finally, if we have been able to successfully export in all available 
        // formats, we'll increment the "last exported" time stamp: 

        dataset.setLastExportTime(new Timestamp(new Date().getTime()));

    }

    public void clearAllCachedFormats(Dataset dataset) throws IOException {
        try {
            Iterator<Exporter> exporters = loader.iterator();
            while (exporters.hasNext()) {
                Exporter e = exporters.next();
                String formatName = e.getProviderName();

                clearCachedExport(dataset, formatName);
            }

            dataset.setLastExportTime(null);
        } catch (IOException ex) {
            //not fatal
        }
    }

    // This method finds the exporter for the format requested, 
    // then produces the dataset metadata as a JsonObject, then calls
    // the "cacheExport()" method that will save the produced output  
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
                    JsonPrinter jsonPrinter = new JsonPrinter(settingsService);
                    final JsonObjectBuilder datasetAsJsonBuilder = jsonPrinter.jsonAsDatasetDto(releasedVersion);
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
            throw new ExportException("Could not find Exporter \"" + formatName + "\", unknown exception");
        }
        throw new ExportException("No such Exporter: " + formatName);
    }

    // This method runs the selected metadata exporter, caching the output 
    // in a file in the dataset directory / container based on its DOI:
    private void cacheExport(DatasetVersion version, String format, JsonObject datasetAsJson, Exporter exporter) throws ExportException {
        try {
            // With some storage drivers, we can open a WritableChannel, or OutputStream 
            // to directly write the generated metadata export that we want to cache; 
            // Some drivers (like Swift) do not support that, and will give us an
            // "operation not supported" exception. If that's the case, we'll have 
            // to save the output into a temp file, and then copy it over to the 
            // permanent storage using the IO "save" command: 
            boolean tempFileRequired = false;
            File tempFile = null;
            OutputStream outputStream = null;
            Dataset dataset = version.getDataset();
            StorageIO<Dataset> storageIO = null;
            try {
                storageIO = DataAccess.createNewStorageIO(dataset, "placeholder");
                Channel outputChannel = storageIO.openAuxChannel("export_" + format + ".cached", DataAccessOption.WRITE_ACCESS);
                outputStream = Channels.newOutputStream((WritableByteChannel) outputChannel);
            } catch (IOException ioex) {
                tempFileRequired = true;
                tempFile = File.createTempFile("tempFileToExport", ".tmp");
                outputStream = new FileOutputStream(tempFile);
            }

            try {
                Path cachedMetadataFilePath = Paths.get(version.getDataset().getFileSystemDirectory().toString(), "export_" + format + ".cached");

                if (!tempFileRequired) {
                    FileOutputStream cachedExportOutputStream = new FileOutputStream(cachedMetadataFilePath.toFile());
                    exporter.exportDataset(version, datasetAsJson, cachedExportOutputStream);
                    cachedExportOutputStream.flush();
                    cachedExportOutputStream.close();

                } else {
                    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
                    exporter.exportDataset(version, datasetAsJson, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    
                    logger.fine("Saving path as aux for temp file in: " + Paths.get(tempFile.getAbsolutePath()));
                    storageIO.savePathAsAux(Paths.get(tempFile.getAbsolutePath()), "export_" + format + ".cached");
                    boolean tempFileDeleted = tempFile.delete();
                    logger.fine("tempFileDeleted: " + tempFileDeleted);
                }

            } catch (IOException ioex) {
                throw new ExportException("IO Exception thrown exporting as " + "export_" + format + ".cached");
            }

        } catch (IOException ioex) {
            throw new ExportException("IO Exception thrown exporting as " + "export_" + format + ".cached");
        }

    }

    private void clearCachedExport(Dataset dataset, String format) throws IOException {
        try {
            StorageIO<Dataset> storageIO = getStorageIO(dataset);
            storageIO.deleteAuxObject("export_" + format + ".cached");

        } catch (IOException ex) {
            throw new IOException("IO Exception thrown exporting as " + "export_" + format + ".cached");
        }

    }

    // This method checks if the metadata has already been exported in this 
    // format and cached on disk. If it has, it'll open the file and retun 
    // the file input stream. If not, it'll return null. 
    private InputStream getCachedExportFormat(Dataset dataset, String formatName) throws ExportException, IOException {

        StorageIO<Dataset> dataAccess = null;

        try {
            dataAccess = DataAccess.getStorageIO(dataset);
        } catch (IOException ioex) {
            throw new IOException("IO Exception thrown exporting as " + "export_" + formatName + ".cached");
        }

        InputStream cachedExportInputStream = null;

        try {
            if (dataAccess.getAuxFileAsInputStream("export_" + formatName + ".cached") != null) {
                cachedExportInputStream = dataAccess.getAuxFileAsInputStream("export_" + formatName + ".cached");
                return cachedExportInputStream;
            }
        } catch (IOException ioex) {
            throw new IOException("IO Exception thrown exporting as " + "export_" + formatName + ".cached");
        }
        return null;

    }

    /*The below method, getCachedExportSize(), is not currently used.
     *An exercise for the reader could be to refactor it if it's needed
     *to be compatible with storage drivers other than local filesystem.
     *Files.exists() would need to be discarded.
     * -- L.A. 4.8 */
//    public Long getCachedExportSize(Dataset dataset, String formatName) {
//        try {
//            if (dataset.getFileSystemDirectory() != null) {
//                Path cachedMetadataFilePath = Paths.get(dataset.getFileSystemDirectory().toString(), "export_" + formatName + ".cached");
//                if (Files.exists(cachedMetadataFilePath)) {
//                    return cachedMetadataFilePath.toFile().length();
//                }
//            }
//        } catch (Exception ioex) {
//            // don't do anything - we'll just return null
//        }
//
//        return null;
//    }
    public Boolean isXMLFormat(String provider) {
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
