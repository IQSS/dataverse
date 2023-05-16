package edu.harvard.iq.dataverse.dataaccess.ingest;

import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

public class FileIngestDataProvider implements IngestDataProvider {
    private int totalCases = 0;

    private File generatedTabularFile;
    private DataTable dataTable;

    // -------------------- LOGIC --------------------

    @Override
    public void initialize(DataTable dataTable, File generatedTabularFile) {
        this.generatedTabularFile = generatedTabularFile;
        this.dataTable = dataTable;
        totalCases = dataTable.getCaseQuantity().intValue();
    }

    @Override
    public IngestDataProvider.CloseableIterable<String> getColumnIterable(int columnNumber) {
        return new FileIterable(columnNumber, generatedTabularFile, dataTable);
    }

    @Override
    public int getCasesNumber() {
        return totalCases;
    }

    // -------------------- INNER CLASSES --------------------

    private static class FileIterable implements IngestDataProvider.CloseableIterable<String> {
        private FileIterator iterator;

        // -------------------- CONSTRUCTORS --------------------

        public FileIterable(int columnNumber, File generatedTabFile, DataTable dataTable) {
            iterator = new FileIterator(columnNumber, dataTable, generatedTabFile);
        }

        // -------------------- LOGIC --------------------

        @Override
        public void close() {
            if (iterator != null) {
                iterator.close();
                iterator = null;
            }
        }

        @Override
        public Iterator<String> iterator() {
            iterator.open();
            return iterator;
        }

        // -------------------- INNSER CLASSES --------------------

        private static class FileIterator implements Iterator<String> {
            private static final Logger logger = LoggerFactory.getLogger(FileIterator.class);

            private File generatedTabFile;
            private int currentRow = 0;
            private int totalCases;
            private int currentColumn;

            private BufferedReader reader = null;

            // -------------------- CONSTRUCTORS --------------------

            public FileIterator(int currentColumn, DataTable dataTable, File generatedTabFile) {
                this.currentColumn = currentColumn;
                this.totalCases = dataTable.getCaseQuantity().intValue();
                this.generatedTabFile = generatedTabFile;
            }

            // -------------------- LOGIC --------------------

            public void open() {
                if (reader != null) {
                    return;
                }
                try {
                    reader = new BufferedReader(new FileReader(generatedTabFile));
                } catch (FileNotFoundException fnfe) {
                    throw new RuntimeException(fnfe);
                }
            }

            public void close() {
                IOException exception = null;
                synchronized (this) {
                    if (reader == null) {
                        return;
                    }
                    try {
                        reader.close();
                    } catch (IOException ioe) {
                        exception = ioe;
                    } finally {
                        reader = null;
                    }
                }
                if (exception != null) {
                    logger.warn("Error while closing buffered file reader. Possible resource leak", exception);
                }
            }

            @Override
            public boolean hasNext() {
                return currentRow < totalCases;
            }

            @Override
            public String next() {
                String value = null;
                try {
                    value = reader.readLine()
                            .split("\t", currentColumn + 2)[currentColumn];
                } catch (IOException ioe) {
                    logger.warn("Exception encountered", ioe);
                }
                currentRow++;
                return value;
            }
        }
    }
}
