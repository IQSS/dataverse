package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Embargo;
import edu.harvard.iq.dataverse.FileMetadata;

import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import static edu.harvard.iq.dataverse.dataaccess.DataAccess.getStorageIO;
import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.XMLExporter;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;

/**
 *
 * @author skraffmi
 */
public class ExportService {

    private static ExportService service;
    private ServiceLoader<Exporter> loader;
    private Map<String, Exporter> exporterMap = new HashMap<>();

    private static final Logger logger = Logger.getLogger(ExportService.class.getCanonicalName());

    private ExportService() {
        /*
         * Step 1 - find the EXPORTERS dir and add all jar files there to a class loader
         */
        List<URL> jarUrls = new ArrayList<>();
        Optional<String> exportPathSetting = JvmSettings.EXPORTERS_DIRECTORY.lookupOptional(String.class);
        if (exportPathSetting.isPresent()) {
            Path exporterDir = Paths.get(exportPathSetting.get());
            // Get all JAR files from the configured directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(exporterDir, "*.jar")) {
                // Using the foreach loop here to enable catching the URI/URL exceptions
                for (Path path : stream) {
                    logger.log(Level.FINE, "Adding {0}", path.toUri().toURL());
                    // This is the syntax required to indicate a jar file from which classes should
                    // be loaded (versus a class file).
                    jarUrls.add(new URL("jar:" + path.toUri().toURL() + "!/"));
                }
            } catch (IOException e) {
                logger.warning("Problem accessing external Exporters: " + e.getLocalizedMessage());
            }
        }
        URLClassLoader cl = URLClassLoader.newInstance(jarUrls.toArray(new URL[0]), this.getClass().getClassLoader());

        /*
         * Step 2 - load all Exporters that can be found, using the jars as additional
         * sources
         */
        loader = ServiceLoader.load(Exporter.class, cl);
        /*
         * Step 3 - Fill exporterMap with providerName as the key, allow external
         * exporters to replace internal ones for the same providerName. FWIW: From the
         * logging it appears that ServiceLoader returns classes in ~ alphabetical order
         * rather than by class loader, so internal classes handling a given
         * providerName may be processed before or after external ones.
         */
        loader.forEach(exp -> {
            String formatName = exp.getFormatName();
            // If no entry for this providerName yet or if it is an external exporter
            if (!exporterMap.containsKey(formatName) || exp.getClass().getClassLoader().equals(cl)) {
                exporterMap.put(formatName, exp);
            }
            logger.log(Level.FINE, "SL: " + exp.getFormatName() + " from " + exp.getClass().getCanonicalName()
                    + " and classloader: " + exp.getClass().getClassLoader().getClass().getCanonicalName());
        });
    }

    public static synchronized ExportService getInstance() {
        if (service == null) {
            service = new ExportService();
        }
        return service;
    }

    public List<String[]> getExportersLabels() {
        List<String[]> retList = new ArrayList<>();

        exporterMap.values().forEach(exp -> {
            String[] temp = new String[2];
            temp[0] = exp.getDisplayName(BundleUtil.getCurrentLocale());
            temp[1] = exp.getFormatName();
            retList.add(temp);
        });
        return retList;
    }

    public InputStream getExport(DatasetVersion datasetVersion, String formatName) throws ExportException, IOException {

        Dataset dataset = datasetVersion.getDataset();
        InputStream exportInputStream = null;

        if (datasetVersion.isDraft()) {
            // For drafts we create the export on the fly rather than caching.
            Exporter exporter = exporterMap.get(formatName);
            if (exporter != null) {
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    // getPrerequisiteFormatName logic copied from exportFormat()
                    if (exporter.getPrerequisiteFormatName().isPresent()) {
                        String prereqFormatName = exporter.getPrerequisiteFormatName().get();
                        try (InputStream preReqStream = getExport(datasetVersion, prereqFormatName)) {
                            InternalExportDataProvider dataProvider = new InternalExportDataProvider(datasetVersion, preReqStream);
                            exporter.exportDataset(dataProvider, outputStream);
                        } catch (IOException ioe) {
                            throw new ExportException("Could not get prerequisite " + prereqFormatName + " to create " + formatName + " export for dataset " + dataset.getId(), ioe);
                        }
                    } else {
                        InternalExportDataProvider dataProvider = new InternalExportDataProvider(datasetVersion);
                        exporter.exportDataset(dataProvider, outputStream);
                    }
                    return new ByteArrayInputStream(outputStream.toByteArray());
                }
            }
        } else {
            // for non-drafts (published versions) we try to locate an already existing, cached export
            exportInputStream = getCachedExportFormat(dataset, formatName);
        }

        // The DDI export is limited for restricted and actively embargoed files (no
        // data/file description sections).and when an embargo ends, we need to refresh
        // this export.
        boolean clearCachedExport = false;
        if (formatName.equals(DDIExporter.PROVIDER_NAME) && (exportInputStream != null)) {
            // We want ddi and there was a cached version
            LocalDate exportLocalDate = null;
            Date lastExportDate = dataset.getLastExportTime();
            // if lastExportDate == null, assume it's not set because were exporting for the
            // first time now (e.g. during publish) and therefore no changes are needed
            if (lastExportDate != null) {
                exportLocalDate = lastExportDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                logger.fine("Last export date: " + exportLocalDate.toString());
                // Track which embargoes we've already checked
                Set<Long> embargoIds = new HashSet<Long>();
                // Check for all files in the latest released version
                for (FileMetadata fm : dataset.getLatestVersionForCopy().getFileMetadatas()) {
                    // ToDo? This loop is necessary because we have not stored the date when the
                    // next embargo in this datasetversion will end. If we knew that (another
                    // dataset/datasetversion column), we could make
                    // one check that nextembargoEnd exists and is after the last export and before
                    // now versus scanning through files until we potentially find such an embargo.
                    Embargo e = fm.getDataFile().getEmbargo();
                    if (e != null) {
                        logger.fine("Datafile:  " + fm.getDataFile().getId());
                        logger.fine("Embargo end date: " + e.getFormattedDateAvailable());
                    }
                    if (e != null && !embargoIds.contains(e.getId()) && e.getDateAvailable().isAfter(exportLocalDate)
                            && e.getDateAvailable().isBefore(LocalDate.now())) {
                        logger.fine("Request that the ddi export be cleared.");
                        // The file has been embargoed and the embargo ended after the last export and
                        // before the current date, so we need to remove the cached DDI export and make
                        // it refresh
                        clearCachedExport = true;
                        break;
                    } else if (e != null) {
                        logger.fine("adding embargo to checked list: " + e.getId());
                        embargoIds.add(e.getId());
                    }
                }
            }
            if (clearCachedExport) {
                try {
                    exportInputStream.close();
                    clearCachedExport(dataset, formatName);
                } catch (Exception ex) {
                    logger.warning("Failure deleting DDI export format for dataset id: " + dataset.getId()
                            + " after embargo expiration: " + ex.getLocalizedMessage());
                } finally {
                    exportInputStream = null;
                }
            }
        }

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

    public String getLatestPublishedAsString(Dataset dataset, String formatName) {
        if (dataset == null) {
            return null;
        }
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        if (releasedVersion == null) {
            return null;
        }
        InputStream inputStream = null;
        InputStreamReader inp = null;
        try {
            inputStream = getExport(releasedVersion, formatName);
            if (inputStream != null) {
                inp = new InputStreamReader(inputStream, "UTF8");
                BufferedReader br = new BufferedReader(inp);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append('\n');
                }
                br.close();
                inp.close();
                inputStream.close();
                return sb.toString();
            }
        } catch (IOException ex) {
            logger.log(Level.FINE, ex.getMessage(), ex);
            return null;
        } finally {
            IOUtils.closeQuietly(inp);
            IOUtils.closeQuietly(inputStream);
        }
        return null;

    }

    // This method goes through all the Exporters and calls
    // the "cacheExport()" method that will save the produced output
    // in a file in the dataset directory, on each Exporter available.
    // This is only for the latest published version.
    public void exportAllFormats(Dataset dataset) throws ExportException {
        try {
            clearAllCachedFormats(dataset);
        } catch (IOException ex) {
            Logger.getLogger(ExportService.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            DatasetVersion releasedVersion = dataset.getReleasedVersion();
            if (releasedVersion == null) {
                throw new ExportException("No released version for dataset " + dataset.getGlobalId().toString());
            }
            InternalExportDataProvider dataProvider = new InternalExportDataProvider(releasedVersion);

            for (Exporter e : exporterMap.values()) {
                String formatName = e.getFormatName();
                if(e.getPrerequisiteFormatName().isPresent()) {
                    String prereqFormatName = e.getPrerequisiteFormatName().get();
                    try (InputStream preReqStream = getExport(dataset.getReleasedVersion(), prereqFormatName)) {
                        dataProvider.setPrerequisiteInputStream(preReqStream);
                        cacheExport(dataset, dataProvider, formatName, e);
                        dataProvider.setPrerequisiteInputStream(null);
                    } catch (IOException ioe) {
                        throw new ExportException ("Could not get prerequisite " + e.getPrerequisiteFormatName() + " to create " + formatName + "export for dataset " + dataset.getId(), ioe);
                    }
                } else {
                    cacheExport(dataset, dataProvider, formatName, e);
                }
            }
            // Finally, if we have been able to successfully export in all available
            // formats, we'll increment the "last exported" time stamp:
            dataset.setLastExportTime(new Timestamp(new Date().getTime()));

        } catch (ServiceConfigurationError serviceError) {
            throw new ExportException("Service configuration error during export. " + serviceError.getMessage());
        } catch (RuntimeException e) {
            logger.log(Level.FINE, e.getMessage(), e);
            throw new ExportException(
                    "Unknown runtime exception exporting metadata. " + (e.getMessage() == null ? "" : e.getMessage()));
        }

    }

    public void clearAllCachedFormats(Dataset dataset) throws IOException {
        try {

            for (Exporter e : exporterMap.values()) {
                String formatName = e.getFormatName();
                clearCachedExport(dataset, formatName);
            }

            dataset.setLastExportTime(null);
        } catch (IOException ex) {
            // not fatal
        }
    }

    // This method finds the exporter for the format requested,
    // then produces the dataset metadata as a JsonObject, then calls
    // the "cacheExport()" method that will save the produced output
    // in a file in the dataset directory.
    public void exportFormat(Dataset dataset, String formatName) throws ExportException {
        try {

            Exporter e = exporterMap.get(formatName);
            if (e != null) {
                DatasetVersion releasedVersion = dataset.getReleasedVersion();
                if (releasedVersion == null) {
                    throw new ExportException(
                            "No published version found during export. " + dataset.getGlobalId().toString());
                }
                if(e.getPrerequisiteFormatName().isPresent()) {
                    String prereqFormatName = e.getPrerequisiteFormatName().get();
                    try (InputStream preReqStream = getExport(releasedVersion, prereqFormatName)) {
                        InternalExportDataProvider dataProvider = new InternalExportDataProvider(releasedVersion, preReqStream);
                        cacheExport(dataset, dataProvider, formatName, e);
                    } catch (IOException ioe) {
                        throw new ExportException ("Could not get prerequisite " + e.getPrerequisiteFormatName() + " to create " + formatName + "export for dataset " + dataset.getId(), ioe);
                    }
                } else {
                    InternalExportDataProvider dataProvider = new InternalExportDataProvider(releasedVersion);
                    cacheExport(dataset, dataProvider, formatName, e);
                }
                // As with exportAll, we should update the lastexporttime for the dataset
                dataset.setLastExportTime(new Timestamp(new Date().getTime()));
            } else {
                throw new ExportException("Exporter not found");
            }
        } catch (IllegalStateException e) {
            // IllegalStateException can potentially mean very different, and
            // unexpected things. An exporter attempting to get a single primitive
            // value from a fieldDTO that is in fact a Multiple and contains a
            // json vector (this has happened, for example, when the code in the
            // DDI exporter was not updated following a metadata fieldtype change),
            // will result in IllegalStateException.
            throw new ExportException("IllegalStateException caught when exporting " + formatName + " for dataset "
                    + dataset.getGlobalId().toString()
                    + "; may or may not be due to a mismatch between an exporter code and a metadata block update. "
                    + e.getMessage());
        }

    }

    public Exporter getExporter(String formatName) throws ExportException {
        Exporter e = exporterMap.get(formatName);
        if (e != null) {
            return e;
        }
        throw new ExportException("No such Exporter: " + formatName);
    }

    // This method runs the selected metadata exporter, caching the output
    // in a file in the dataset directory / container based on its DOI:
    private void cacheExport(Dataset dataset, InternalExportDataProvider dataProvider, String format, Exporter exporter)
            throws ExportException {
        
        OutputStream outputStream = null;
        try {
            boolean tempFileUsed = false;
            File tempFile = null;
            StorageIO<Dataset> storageIO = null;

            // With some storage drivers, we can open a WritableChannel, or OutputStream
            // to directly write the generated metadata export that we want to cache;
            // Some drivers (like Swift) do not support that, and will give us an
            // "operation not supported" exception. If that's the case, we'll have
            // to save the output into a temp file, and then copy it over to the
            // permanent storage using the IO "save" command:
            try {
                storageIO = DataAccess.getStorageIO(dataset);
                Channel outputChannel = storageIO.openAuxChannel("export_" + format + ".cached",
                        DataAccessOption.WRITE_ACCESS);
                outputStream = Channels.newOutputStream((WritableByteChannel) outputChannel);
            } catch (IOException ioex) {
                // A common case = an IOException in openAuxChannel which is not supported by S3
                // stores for WRITE_ACCESS
                tempFileUsed = true;
                tempFile = File.createTempFile("tempFileToExport", ".tmp");
                outputStream = new FileOutputStream(tempFile);
            }

            try {
                // Write the metadata export file to the outputStream, which may be the final
                // location or a temp file
                exporter.exportDataset(dataProvider, outputStream);
                outputStream.flush();
                outputStream.close();
                if (tempFileUsed) {
                    logger.fine("Saving export_" + format + ".cached aux file from temp file: "
                            + Paths.get(tempFile.getAbsolutePath()));
                    storageIO.savePathAsAux(Paths.get(tempFile.getAbsolutePath()), "export_" + format + ".cached");
                    boolean tempFileDeleted = tempFile.delete();
                    logger.fine("tempFileDeleted: " + tempFileDeleted);
                }
            } catch (ExportException exex) {
                /*
                 * This exception is from the particular exporter and may not affect other
                 * exporters (versus other exceptions in this method which are from the basic
                 * mechanism to create a file) So we'll catch it here and report so that loops
                 * over other exporters can continue. Todo: Might be better to create a new
                 * exception subtype and send it upward, but the callers currently just log and
                 * ignore beyond terminating any loop over exporters.
                 */
                logger.warning("Exception thrown while creating export_" + format + ".cached : " + exex.getMessage());
            } catch (IOException ioex) {
                throw new ExportException("IO Exception thrown exporting as " + "export_" + format + ".cached");
            }

        } catch (IOException ioex) {
            // This catches any problem creating a local temp file in the catch clause above
            throw new ExportException("IO Exception thrown before exporting as " + "export_" + format + ".cached");
        } finally {
            IOUtils.closeQuietly(outputStream);
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
            throw new IOException("IO Exception thrown exporting as " + "export_" + formatName + ".cached", ioex);
        }

        InputStream cachedExportInputStream = null;

        try {
            cachedExportInputStream = dataAccess.getAuxFileAsInputStream("export_" + formatName + ".cached");
            return cachedExportInputStream;
        } catch (IOException ioex) {
            throw new IOException("IO Exception thrown exporting as " + "export_" + formatName + ".cached", ioex);
        }

    }

    /*
     * The below method, getCachedExportSize(), is not currently used. An exercise
     * for the reader could be to refactor it if it's needed to be compatible with
     * storage drivers other than local filesystem. Files.exists() would need to be
     * discarded. -- L.A. 4.8
     */
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
        Exporter e = exporterMap.get(provider);
        if (e != null) {
            return e instanceof XMLExporter;
        }
        return null;
    }

    public String getMediaType(String provider) {
        Exporter e = exporterMap.get(provider);
        if (e != null) {
            return e.getMediaType();
        }
        return MediaType.TEXT_PLAIN;
    }

}
