package edu.harvard.iq.dataverse.export;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RelatedIdentifierTypeConstants {
    public static Map<String, String> ALTERNATIVE_TO_MAIN_ID_TYPE_INDEX = Initializer.createAlternativeToMainIdTypeIndex();

    // -------------------- INNER CLASSES --------------------

    private static class Initializer {
        public static Map<String, String> createAlternativeToMainIdTypeIndex() {
            return Collections.unmodifiableMap(Stream.of("ARK", "arXiv", "bibcode", "DOI", "EAN13", "EISSN", "Handle", "ISBN", "ISSN", "ISTC",
                                "LISSN", "LSID", "PISSN", "PMID", "PURL", "UPC", "URL", "URN", "WOS")
                    .collect(Collectors.toMap(String::toLowerCase, v -> v)));
        }
    }
}
