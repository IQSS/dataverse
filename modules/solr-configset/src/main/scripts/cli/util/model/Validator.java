package cli.util.model;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Validator {
    
    /**
     * Test if a given value is a valid {@link java.net.URL}
     *
     * Remember, Java only supports HTTP/S, file and JAR protocols by default!
     * Any URL not using such a protocol will not be considered a valid URL!
     * {@see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/URL.html#%3Cinit%3E(java.lang.String,java.lang.String,int,java.lang.String)">URL Constructor Summary</a>}
     *
     * @param url The value to test
     * @return True if valid URL, false otherwise
     */
    public static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
    
    /**
     * Split and validate a textual line declared to be a header of custom metadata block definition section
     * (the block, the fields or controlled vocabularies). Will return a list of the headers found (if they match) and
     * when being a spec conform header line.
     *
     * As this function retrieves the relevant spec parts as parameters, it can be reused for all sections.
     * You will need to transform into the resulting list into real Header enum values within calling code.
     *
     * This validator is strict with naming and order of appearance (must be same as spec), but is lenient
     * about case (so you might use camel/pascal case variants).
     *
     * @param headerLine The textual line to analyse.
     * @param startsWith A String which needs to be present at the start of the headerLine.
     * @param validOrderedHeaders A list of Strings with the column headers from the spec in order of appearance.
     * @return A list of the found headers in normalized form if matching the spec
     * @throws ParserException If any validation fails. Contains sub-exceptions with validation details.
     */
    static List<String> validateHeaderLine(final String headerLine,
                                           final String startsWith,
                                           final List<String> validOrderedHeaders) throws ParserException {
        // start a parenting parser exception to be filled with errors as subexceptions
        ParserException ex = new ParserException("contains an invalid column header");
    
        if (headerLine == null || headerLine.isBlank()) {
            ex.addSubException("Header may not be null, empty or whitespace only");
            throw ex;
        }
        
        // the actual split and validate length
        String[] headerSplit = headerLine.split(Constants.COLUMN_SEPARATOR);
        // missing headers?
        if (headerSplit.length < validOrderedHeaders.size()) {
            ex.addSubException(
                "Less fields (" + headerSplit.length + ") found than required (" + validOrderedHeaders.size() + ").");
        } else if (headerSplit.length > validOrderedHeaders.size()) {
            ex.addSubException(
                "More fields (" + headerSplit.length + ") found than required (" + validOrderedHeaders.size() + ").");
        }
    
        // allocate a list of validated columns
        List<String> validatedColumns = new ArrayList<>();
        
        // iterate the found header values
        for (int i = 0; i < headerSplit.length; i++) {
            String columnHeader = headerSplit[i];
            
            // is the value a valid one? (in order of appearance and existing, but ignoring case)
            if (i < validOrderedHeaders.size() && validOrderedHeaders.get(i).equalsIgnoreCase(columnHeader)) {
                // add as entry of validated and present headers (to be used for line mapping)
                // BUT use the normalized variant (makes comparisons easier)
                validatedColumns.add(validOrderedHeaders.get(i));
            // when invalid, mark as such
            } else {
                ex.addSubException(
                    "Column " + (i+1) + " contains '" + columnHeader + "', but spec expects " +
                    (i < validOrderedHeaders.size() ? "'"+validOrderedHeaders.get(i)+"'" : "nothing") + " to be here."
                );
                // additional hint when valid, but accidentally already present
                if (validatedColumns.stream().anyMatch(columnHeader::equalsIgnoreCase)) {
                    ex.addSubException("Column " + (i+1) + " contains valid '" + columnHeader + "' already present.");
                }
            }
        }
        
        // when there are headers missing, report them
        if ( validatedColumns.size() < validOrderedHeaders.size() ) {
            for (int i = 0; i < validOrderedHeaders.size(); i++) {
                String missingHeader = validOrderedHeaders.get(i);
                if (validatedColumns.stream().noneMatch(missingHeader::equalsIgnoreCase)) {
                    ex.addSubException("Missing column '" + missingHeader + "' from position " + (i+1) + ".");
                }
            }
        }
        
        // Will only return the header column mapping if and only if the validation did not find errors.
        // use an unmodifiable version of the list to avoid accidents without notice. Else throw the exception.
        if (ex.hasSubExceptions()) {
            throw ex;
        } else {
            return List.copyOf(validatedColumns);
        }
    }
}
