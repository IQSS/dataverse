package cli.util.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Field {
    
    public static final String KEYWORD = "datasetField";
    
    /**
     * Programmatic variant of the spec of a #datasetField. List all the column headers and associated restrictions
     * on the values of a column.
     */
    public enum Header {
        // TODO: extend!
        KEYWORD(
            Field.KEYWORD,
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
         * Inverse lookup of a {@link Field.Header} from a {@link String}.
         *
         * @param value A textual string to look up.
         * @return Matching {@link Field.Header} wrapped in {@link Optional} or an empty {@link Optional}.
         */
        public static Optional<Header> getByValue(String value) {
            return Optional.ofNullable(valueMap.get(value));
        }
    
        /**
         * Retrieve all column headers of field definitions as a spec-like list of strings,
         * usable for validation and more. The list is ordered as the spec defines the order of the headers.
         *
         * @return List of the column headers, in order
         */
        public static List<String> getHeaders() {
            return List.copyOf(valueMap.keySet());
        }
    
        /**
         * Parse a {@link String} as a header of field definitions. Will validate the presence or absence
         * of column headers as defined by the spec. This is not a lenient parser - headers need to comply with order
         * from the spec. On the other hand, it is case-insensitive.
         *
         * @param line The textual line to parse for column headers
         * @param config The parser configuration to be used
         * @return A list of {@link Field.Header} build from the line of text
         * @throws ParserException When presented column headers are missing, invalid or the complete line is just wrong.
         * @throws IllegalStateException When a column header cannot be found within the enum {@link Field.Header}.
         *                               This should never happen, as the validation would fail before!
         */
        public static List<Header> parseAndValidate(final String line, final Configuration config) throws ParserException {
            List<String> validatedColumns = Validator.validateHeaderLine(line, getHeaders(), config);
            // the exception shall never happen, as we already validated the header!
            return validatedColumns.stream()
                .map(Header::getByValue)
                .map(o -> o.orElseThrow(IllegalStateException::new))
                .collect(Collectors.toUnmodifiableList());
        }
        
        @Override
        public String toString() {
            return value;
        }
        
        public boolean isValid(final String sut) {
            return sut != null && test.test(sut);
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    public enum Types implements Predicate<String> {
        NONE("none"),
        DATE("date"),
        EMAIL("email"),
        TEXT("text"),
        TEXTBOX("textbox"),
        URL("url"),
        INT("int"),
        FLOAT("float");
        
        private final String name;
        
        Types(final String name) {
            this.name = name;
        }
    
        private static final Map<String, Types> valueMap;
        static {
            Map<String, Types> map = new ConcurrentHashMap<>();
            Arrays.stream(Types.values()).forEach(type -> map.put(type.toString(), type));
            valueMap = Collections.unmodifiableMap(map);
        }
    
        @Override
        public boolean test(String sut) {
            // we demand correct case!
            return this.toString().equals(sut);
        }
        
        public static Predicate<String> matchesTypes() {
            Predicate<String> test = NONE;
            for (Types type : Types.values()) {
                test = test.or(type);
            }
            return test;
        }
        
        public static List<String> getTypesList() {
            return valueMap.keySet().stream().collect(Collectors.toUnmodifiableList());
        }
        
        @Override
        public String toString() {
            return this.name;
        }
    }
    
    
    Optional<List<ControlledVocabulary>> controlledVocabularyValues = Optional.empty();
}
