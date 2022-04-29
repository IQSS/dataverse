package cli.util.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ControlledVocabulary {
    
    public static final String KEYWORD = "controlledVocabulary";

    /**
     * Programmatic variant of the spec of a #controlledVocabulary. List all the column headers and associated restrictions
     * on the values of a column.
     */
    public enum Header {
        // TODO: add the rest of the fields
        KEYWORD(
            ControlledVocabulary.KEYWORD,
            String::isEmpty,
            "must have no value (be empty)"
        );
    
        private final String value;
        private final Predicate<String> test;
        private final String errorMessage;
    
        Header(final String value, final Predicate<String> test, final String errorMessage) {
            this.value = value;
            this.test = test;
            this.errorMessage = errorMessage;
        }
    
        Header(final String value) {
            this.value = value;
            this.test = Predicate.not(String::isBlank);
            this.errorMessage = "must not be empty or blank";
        }
    
        private static final Map<String, Header> valueMap;
        static {
            Map<String, Header> map = new ConcurrentHashMap<>();
            Arrays.stream(Header.values()).forEach(h -> map.put(h.toString(), h));
            valueMap = Collections.unmodifiableMap(map);
        }
    
        /**
         * Inverse lookup of a {@link ControlledVocabulary.Header} from a {@link String}.
         *
         * @param value A textual string to look up.
         * @return Matching {@link ControlledVocabulary.Header} wrapped in {@link Optional} or an empty {@link Optional}.
         */
        public static Optional<ControlledVocabulary.Header> getByValue(String value) {
            return Optional.ofNullable(valueMap.get(value));
        }
    
        /**
         * Retrieve all column headers of controlled vocabulary definitions as a spec-like list of strings,
         * usable for validation and more. The list is ordered as the spec defines the order of the headers.
         *
         * @return List of the column headers, in order
         */
        public static List<String> getHeaders() {
            return List.copyOf(valueMap.keySet());
        }
    
        /**
         * Parse a {@link String} as a header of a metadata block definition. Will validate the presence or absence
         * of column headers as defined by the spec. This is not a lenient parser - headers need to comply with order
         * from the spec. On the other hand, it is case-insensitive.
         *
         * @param line The textual line to parse for column headers
         * @param config The parser configuration to be used
         * @return A list of {@link ControlledVocabulary.Header} build from the line of text
         * @throws ParserException When presented column headers are missing, invalid or the complete line is just wrong.
         * @throws IllegalStateException When a column header cannot be found within the enum {@link ControlledVocabulary.Header}.
         *                               This should never happen, as the validation would fail before!
         */
        public static List<ControlledVocabulary.Header> parseAndValidate(final String line, final Configuration config) throws ParserException {
            List<String> validatedColumns = Validator.validateHeaderLine(line, getHeaders(), config);
            // the exception shall never happen, as we already validated the header!
            return validatedColumns.stream()
                .map(ControlledVocabulary.Header::getByValue)
                .map(o -> o.orElseThrow(IllegalStateException::new))
                .collect(Collectors.toUnmodifiableList());
        }
    
        @Override
        public String toString() {
            return value;
        }
    
        public boolean isValid(final String sut) {
            return test.test(sut);
        }
    
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    public static final class ControlledVocabularyBuilder {
        // TODO: extend!
        public ControlledVocabulary build() {
            return new ControlledVocabulary();
        }
    }
    
    private ControlledVocabulary() {}
}
