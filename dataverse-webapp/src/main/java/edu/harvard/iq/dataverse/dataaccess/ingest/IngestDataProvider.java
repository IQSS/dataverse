package edu.harvard.iq.dataverse.dataaccess.ingest;

import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.regex.Matcher;

public interface IngestDataProvider {
    void initialize(DataTable dataTable, File generatedTabularFile);

    CloseableIterable<String> getColumnIterable(int columnNumber);

    int getCasesNumber();

    default Double[] getDoubleColumn(int columnNumber) {
        int totalCases = getCasesNumber();
        Double[] result = new Double[totalCases];
        int caseIndex = 0;
        try (CloseableIterable<String> column = getColumnIterable(columnNumber)) {
            for (String value : column) {
                if ("inf".equalsIgnoreCase(value) || "+inf".equalsIgnoreCase(value)) {
                    result[caseIndex] = Double.POSITIVE_INFINITY;
                } else if ("-inf".equalsIgnoreCase(value)) {
                    result[caseIndex] = Double.NEGATIVE_INFINITY;
                } else if (StringUtils.isBlank(value)) {
                    result[caseIndex] = null;
                } else {
                    try {
                        result[caseIndex] = new Double(value);
                    } catch (NumberFormatException ex) {
                        result[caseIndex] = null; // missing value
                    }
                }
                caseIndex++;
            }
        }
        if (caseIndex != totalCases) {
            throw new RuntimeException(String.format("Insufficient cases: [%d] instead of [%d].", caseIndex, totalCases));
        }
        return result;
    }

    default Float[] getFloatColumn(int columnNumber) {
        int totalCases = getCasesNumber();
        Float[] result = new Float[totalCases];
        int caseIndex = 0;
        try (CloseableIterable<String> column = getColumnIterable(columnNumber)) {
            for (String value : column) {
                if ("inf".equalsIgnoreCase(value) || "+inf".equalsIgnoreCase(value)) {
                    result[caseIndex] = Float.POSITIVE_INFINITY;
                } else if ("-inf".equalsIgnoreCase(value)) {
                    result[caseIndex] = Float.NEGATIVE_INFINITY;
                } else if (value == null || value.isEmpty()) {
                    result[caseIndex] = null;
                } else {
                    try {
                        result[caseIndex] = new Float(value);
                    } catch (NumberFormatException ex) {
                        result[caseIndex] = null; // missing value
                    }
                }
                caseIndex++;
            }
        }
        if (caseIndex != totalCases) {
            throw new RuntimeException(String.format("Insufficient cases: [%d] instead of [%d].", caseIndex, totalCases));
        }
        return result;
    }

    default Long[] getLongColumn(int columnNumber) {
        int totalCases = getCasesNumber();
        Long[] result = new Long[totalCases];
        int caseIndex = 0;
        try (CloseableIterable<String> column = getColumnIterable(columnNumber)) {
            for (String value : column) {
                try {
                    result[caseIndex] = Long.valueOf(value);
                } catch (NumberFormatException nfe) {
                    result[caseIndex] = null;
                }
                caseIndex++;
            }
        }
        if (caseIndex != totalCases) {
            throw new RuntimeException(String.format("Insufficient cases: [%d] instead of [%d].", caseIndex, totalCases));
        }
        return result;
    }

    default String[] getStringColumn(int columnNumber) {
        int totalCases = getCasesNumber();
        String[] result = new String[totalCases];
        int caseIndex = 0;
        try (CloseableIterable<String> column = getColumnIterable(columnNumber)) {
            for (String value : column) {
                if (StringUtils.EMPTY.equals(value)) {
                    // An empty string is a string missing value!
                    // An empty string in quotes is an empty string!
                    result[caseIndex] = null;
                } else {
                    // Strip the outer quotes:
                    value = value.replaceFirst("^\"", "")
                            .replaceFirst("\"$", "");

                    // We need to restore the special characters that are stored in tab files escaped - quotes, new lines
                    // and tabs. Before we do that however, we need to take care of any escaped backslashes stored in
                    // the tab file. I.e., "foo\t" should be transformed to "foo<TAB>"; but "foo\\t" should be transformed
                    // to "foo\t". This way new lines and tabs that were already escaped in the original data are not
                    // going to be transformed to unescaped tab and new line characters!
                    String[] splitTokens = value.split(Matcher.quoteReplacement("\\\\"), -2);

                    // (note that it's important to use the 2-argument version of String.split(), and set the limit argument
                    // to a negative value; otherwise any trailing backslashes are lost.)
                    for (int i = 0; i < splitTokens.length; i++) {
                        splitTokens[i] = splitTokens[i].replaceAll(Matcher.quoteReplacement("\\\""), "\"")
                                .replaceAll(Matcher.quoteReplacement("\\t"), "\t")
                                .replaceAll(Matcher.quoteReplacement("\\n"), "\n")
                                .replaceAll(Matcher.quoteReplacement("\\r"), "\r");
                    }
                    // TODO:
                    // Make (some of?) the above optional; for ex., we do need to restore the newlines when calculating
                    // UNFs; But if we are subsetting these vectors in order to create a new tab-delimited file, they will
                    // actually break things! -- L.A. Jul. 28 2014
                    value = StringUtils.join(splitTokens, '\\');
                    result[caseIndex] = value;
                }
                caseIndex++;
            }
        }
        if (caseIndex != totalCases) {
            throw new RuntimeException(String.format("Insufficient cases: [%d] instead of [%d].", caseIndex, totalCases));
        }
        return result;
    }

    interface CloseableIterable<T> extends AutoCloseable, Iterable<T> {
        @Override void close();
    };
}
