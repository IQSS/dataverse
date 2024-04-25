package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes a CSV file with file names and download URL's.
 */
@Stateless
public class DatasetFileDownloadUrlCsvWriter {

    private static final Logger logger = LoggerFactory.getLogger(DatasetFileDownloadUrlCsvWriter.class);

    @EJB
    protected SystemConfig systemConfig;

    // -------------------- LOGIC --------------------

    public void write(OutputStream outputStream, List<FileMetadata> fileMetadataList) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream);
             BufferedWriter streamWriter = new BufferedWriter(writer);
             CSVPrinter csvPrinter = new CSVPrinter(streamWriter, CSVFormat.DEFAULT)) {

            csvPrinter.printRecord(FileCSVRecord.getHeaders());
            for(FileMetadata fileMetadata : fileMetadataList) {
                csvPrinter.printRecord(buildRecord(fileMetadata).getValues());
            }
        } catch (IOException ioe) {
            logger.error("Couldn't write download urls to csv", ioe);
            throw ioe;
        }
    }

    // -------------------- PRIVATE --------------------

    private FileCSVRecord buildRecord(FileMetadata fileMetadata) {
        FileCSVRecord record = new FileCSVRecord();
        record.setFileName(fileMetadata.getLabel());

        boolean openAccess = FileUtil.isPubliclyDownloadable(fileMetadata);
        record.setOpenAccess(openAccess);

        if (openAccess) {
            record.setUrl(systemConfig.getDataverseSiteUrl() + FileUtil.getFileDownloadUrlPath(FileUtil.ApiDownloadType.DEFAULT, fileMetadata.getDataFile().getId(), false));
        }
        return record;
    }

    // -------------------- INNER CLASSES --------------------

    enum FileDownloadCsvColumn {
        FILE_NAME("Filename"),
        OPEN_ACCESS("openAccess"),
        URL("url");

        final String columnName;

        FileDownloadCsvColumn(String columnName) {
            this.columnName = columnName;
        }

        public String getColumnName() {
            return columnName;
        }
    }

    static class FileCSVRecord {

        private static final List<String> CSV_HEADERS = Arrays.stream(FileDownloadCsvColumn.values())
                .map(FileDownloadCsvColumn::getColumnName)
                .collect(Collectors.toList());

        private Map<FileDownloadCsvColumn, String> data = new HashMap<>();

        public static List<String> getHeaders() {
            return CSV_HEADERS;
        }

        public List<String> getValues() {
            return Arrays.stream(FileDownloadCsvColumn.values())
                    .map(data::get)
                    .collect(Collectors.toList());
        }

        public void setOpenAccess(Boolean openAccess) {
            data.put(FileDownloadCsvColumn.OPEN_ACCESS, openAccess.toString());
        }

        public void setUrl(String url) {
            data.put(FileDownloadCsvColumn.URL, url);
        }

        public void setFileName(String fileName) {
            data.put(FileDownloadCsvColumn.FILE_NAME, fileName);
        }
    }
}
