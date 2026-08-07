package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.service.ExporterRegistryBean;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.XMLExporter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class ExportService {

    private static final Logger logger = Logger.getLogger(ExportService.class.getCanonicalName());

    @EJB
    ExporterRegistryBean exporterRegistry;
    
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

    // A convenience wrapper method; the actual implementation has been moved
    // into exportFormats() below.
    public void exportAllFormats(Dataset dataset) throws ExportException {
        exportFormats(dataset, List.of());
    }
    
    /** 
     * This method is added to supplement the classic exportAllFormats() in order
     * to allow the metadata export APIs to selectively re-export only the formats
     * specified. This is to finally allow an instance admin to avoid running
     * a complete, from-scratch reexport when only _some_, or just one of them
     * actually needs to be refreshed. On a large instance this can waste a
     * significant amount of time and CPU cycles. (new as of 6.12)
     * This method calls the cacheExport() method for every valid/supported
     * format name supplied, or for every Exporter available, if an empty List
     * is passed.
     * Only the latest published version is used for exports.
     * exportAllFormats() above is now a convenience wrapper, with the
     * implementation moved here.
     *
     * @param dataset
     * @param formatNames
     * @throws ExportException
     */
    public void exportFormats(Dataset dataset, List<String> formatNames) throws ExportException {
        if (dataset == null) {
            throw new ExportException("exportFormats called with null Dataset");
        }
        
        if (formatNames == null) {
            throw new ExportException("exportFormats called with null formatNames (use an empty List for \"all\"");
        }
        
        try {
            clearCachedFormats(dataset, formatNames);
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
                if (formatNames.isEmpty() || formatNames.contains(formatName)) {
                    if (e.getPrerequisiteFormatName().isPresent()) {
                        String prereqFormatName = e.getPrerequisiteFormatName().get();
                        try (InputStream preReqStream = getExport(dataset.getReleasedVersion(), prereqFormatName)) {
                            dataProvider.setPrerequisiteInputStream(preReqStream);
                            cacheExport(dataset, dataProvider, formatName, e);
                            dataProvider.setPrerequisiteInputStream(null);
                        } catch (IOException ioe) {
                            throw new ExportException("Could not get prerequisite " + e.getPrerequisiteFormatName() + " to create " + formatName + "export for dataset " + dataset.getId(), ioe);
                        }
                    } else {
                        cacheExport(dataset, dataProvider, formatName, e);
                    }
                }
            }
            // Finally, if we have been able to successfully export in all available
            // formats, we'll increment the "last exported" time stamp:
            if (formatNames.isEmpty()) {
                dataset.setLastExportTime(new Timestamp(new Date().getTime()));
            }

        } catch (ServiceConfigurationError serviceError) {
            throw new ExportException("Service configuration error during export. " + serviceError.getMessage());
        } catch (RuntimeException e) {
            logger.log(Level.FINE, e.getMessage(), e);
            throw new ExportException(
                    "Unknown runtime exception exporting metadata. " + (e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    // A convenience wrapper method
    public void clearAllCachedFormats(Dataset dataset) throws IOException {
        clearCachedFormats(dataset, List.of());
        dataset.setLastExportTime(null);
    }
    
    public void clearCachedFormats(Dataset dataset, List<String> formatNames) throws IOException {
        if (dataset == null) {
            throw new ExportException("cleareCachedFormats called with null Dataset");
        }
        
        if (formatNames == null) {
            throw new ExportException("clearCachedFormats called with null formatNames (use an empty List for \"all\"");
        }

        for (Exporter e : exporterMap.values()) {
            String formatName = e.getFormatName();
            if (formatNames.isEmpty() || formatNames.contains(formatName)) {
                try {
                    clearCachedExport(dataset, formatName);
                } catch (IOException ex) {
                    // not fatal
                }
            }
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
