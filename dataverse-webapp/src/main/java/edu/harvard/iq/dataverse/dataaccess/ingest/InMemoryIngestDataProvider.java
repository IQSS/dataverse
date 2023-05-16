package edu.harvard.iq.dataverse.dataaccess.ingest;

import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

public class InMemoryIngestDataProvider implements IngestDataProvider {

    private String data[][];
    private int totalCases = 0;
    private boolean initialized = false;

    // -------------------- LOGIC --------------------

    @Override
    public void initialize(DataTable dataTable, File generatedTabularFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(generatedTabularFile))) {
            totalCases = dataTable.getCaseQuantity().intValue();
            data = new String[totalCases][];
            String line;
            for (int i = 0; i < dataTable.getCaseQuantity(); i++) {
                line = reader.readLine();
                if (line != null) {
                    data[i] = line.split("\t", -1);
                } else {
                    throw new RuntimeException("The file has fewer rows than the determined number of cases.");
                }
            }
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isNotBlank(line)) {
                    throw new RuntimeException("The file has extra non-empty rows than the determined number of cases.");
                }
            }
            initialized = true;
        } catch (IOException ioe) {
            throw new RuntimeException("Exception while reading file", ioe);
        }
    }

    @Override
    public CloseableIterable<String> getColumnIterable(int columnNumber) {
        if (!initialized) {
            throw new IllegalStateException("Provider not initialized!");
        }
        return new ArrayIterable(columnNumber, data);
    }

    @Override
    public int getCasesNumber() {
        return totalCases;
    }

    // -------------------- INNER CLASSES --------------------

    private static class ArrayIterable implements CloseableIterable<String> {

        private String[][] data;
        private int columnNumber;

        // -------------------- CONSTRUCTORS --------------------

        public ArrayIterable(int columnNumber, String[][] data) {
            this.columnNumber = columnNumber;
            this.data = data;
        }

        // -------------------- LOGIC --------------------

        @Override
        public void close() {
            data = null;
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                int currentRow = 0;

                @Override
                public boolean hasNext() {
                    return currentRow < data.length;
                }

                @Override
                public String next() {
                    return data[currentRow++][columnNumber];
                }
            };
        }
    }
}
