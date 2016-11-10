package edu.harvard.iq.dataverse.batch.jobs.importer.checksum;

import org.beanio.stream.RecordIOException;
import org.beanio.stream.RecordReader;
import org.beanio.stream.delimited.DelimitedRecordParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class ChecksumManifestParser extends DelimitedRecordParserFactory {

    @Override
    public RecordReader createReader(Reader in) throws IllegalArgumentException {

        if (!(in instanceof BufferedReader)) {
            in = new BufferedReader(in);
        }

        final BufferedReader reader = (BufferedReader) in;

        return new RecordReader() {
            private String recordText;
            private int lineNumber;

            public Object read() throws IOException, RecordIOException {
                ++lineNumber;
                recordText = reader.readLine();
                return recordText == null ? null : recordText.split(" +", 2); // split on first whitespace
            }
            public void close() throws IOException {
                reader.close();
            }
            public int getRecordLineNumber() {
                return lineNumber;
            }
            public String getRecordText() {
                return recordText;
            }
        };
    }
}

