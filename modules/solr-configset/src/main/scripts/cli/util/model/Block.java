package cli.util.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Block {
    public static final String KEYWORD = "metadataBlock";
    public static final String NAME_PATTERN = "[a-z]+(([\\d_])|([A-Z0-9][a-z0-9]+))*([A-Z])?";
    
    /**
     * Programmatic variant of the spec of a #metadataBlock. List all the column headers and associated restrictions
     * on the values of a column.
     */
    public enum Header {
        KEYWORD(
            Block.KEYWORD,
            String::isEmpty,
            "must have no value (be empty)"
        ),
        NAME(
            "name",
            Predicate.not(String::isBlank).and(Pattern.compile(Block.NAME_PATTERN).asMatchPredicate()),
            "must not be blank and match regex pattern " + Block.NAME_PATTERN
        ),
        DATAVERSE_ALIAS(
            "dataverseAlias",
            Predicate.not(String::isBlank).or(String::isEmpty),
            "must be either empty or not blank"
        ),
        DISPLAY_NAME(
            "displayName",
            Predicate.not(String::isBlank).and(h -> h.length() < 257),
            "must not be blank and shorter than 256 chars"
        ),
        BLOCK_URI(
            "blockURI",
            Validator::isValidUrl,
            "must be a valid URL"
        );
        
        private final String value;
        private final Predicate<String> test;
        private final String errorMessage;
        
        Header(final String value, final Predicate<String> test, final String errorMessage) {
            this.value = value;
            this.test = test;
            this.errorMessage = errorMessage;
        }
        
        private static final Map<String, Header> valueMap;
        static {
            Map<String, Header> map = new ConcurrentHashMap<>();
            Arrays.stream(Header.values()).forEach(h -> map.put(h.toString(), h));
            valueMap = Collections.unmodifiableMap(map);
        }
        
        /**
         * Inverse lookup of a {@link Header} from a {@link String}.
         *
         * @param value A textual string to look up.
         * @return Matching {@link Header} wrapped in {@link Optional} or an empty {@link Optional}.
         */
        public static Optional<Header> getByValue(String value) {
            return Optional.ofNullable(valueMap.get(value));
        }
        
        /**
         * Retrieve all column headers of a metadata block definition as a spec-like list of strings,
         * usable for validation and more. The list is ordered as the spec defines the order of the headers.
         *
         * @return List of the column headers, in order
         */
        public static List<String> getHeaders() {
            return Arrays.stream(Header.values()).map(Header::toString).collect(Collectors.toUnmodifiableList());
        }
        
        /**
         * Parse a {@link String} as a header of a metadata block definition. Will validate the presence or absence
         * of column headers as defined by the spec. This is not a lenient parser - headers need to comply with order
         * from the spec. On the other hand, it is case-insensitive.
         *
         * @param line The textual line to parse for column headers
         * @param config The parser configuration to be used
         * @return A list of {@link Header} build from the line of text
         * @throws ParserException When presented column headers are missing, invalid or the complete line is just wrong.
         * @throws IllegalStateException When a column header cannot be found within the enum {@link Header}.
         *                               This should never happen, as the validation would fail before!
         */
        public static List<Header> parseAndValidate(final String line, final Configuration config) throws ParserException {
            List<String> validatedColumns = Validator.validateHeaderLine(line, getHeaders(), config);
            // the IllegalStateException shall never happen, as we already validated the header!
            return validatedColumns.stream()
                .map(Header::getByValue)
                .map(o -> o.orElseThrow(IllegalStateException::new))
                .collect(Collectors.toUnmodifiableList());
        }
        
        @Override
        public String toString() {
            return value;
        }
        
        /**
         * Test a given {@link String} if it matches the restrictions applied on values of this type.
         *
         * @param sut The textual string to test.
         * @return True if matching or false in every other case.
         */
        public boolean isValid(final String sut) {
            return test.test(sut);
        }
        
        /**
         * Receive a proper error message for this type of value (should be extended with more context in calling code!).
         *
         * @return The error message, always in the form of "must ...". (Create a sentence with it.)
         */
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Blocks should not be build directly, but using this builder pattern. This allows for validation before handing
     * over an object to work with and containing a complete POJO representation of a (custom) metadata block.
     */
    public static final class BlockBuilder {
        private final Configuration config;
        private final List<Header> header;
        private final Map<Header,String> settings = new EnumMap<>(Header.class);
        private final List<Field.FieldBuilder> fieldBuilders = new ArrayList<>();
        private boolean hasErrors = false;
        private boolean hasParsedALine = false;
        
        private BlockBuilder() {
            this.config = Configuration.defaultConfig();
            this.header = new ArrayList<>();
        }
        
        /**
         * Create a builder with a line containing the header of the metadata block definition, so the order of
         * the columns can be determined from it. (The builder is stateful.)
         *
         * @param header The textual line with the column headers.
         * @throws ParserException
         */
        public BlockBuilder(final String header, final Configuration config) throws ParserException {
            this.config = config;
            this.header = Block.Header.parseAndValidate(header, config);
        }
        
        /**
         * Analyse a line containing a concrete metadata block definition by parsing and validating it.
         *
         * This will fail:
         * - when the line is null or blanks only
         * - when another line has been analysed before (spec allows only 1 definition in a single custom metadata block)
         * - when the columns within the line do not match the length of the header
         * - when the column values do not match the column type restrictions (as implied by the header)
         *
         * The exception might contain sub-exceptions, as the parser will do its best to keep going and find as many
         * problems as possible to avoid unnecessary (pesky) re-iterations.
         *
         * @param line The metadata block definition line to analyse.
         * @throws ParserException If the parsing fails (see description).
         */
        public void parseAndValidateLine(final String line) throws ParserException {
            // no null or blank lines for the parser. (blank lines can be skipped and not sent here by calling code)
            if (line == null || line.isBlank()) {
                this.hasErrors = true;
                throw new ParserException("must not be empty nor blanks only nor null.");
            }
            
            // only 1 block definition allowed as per spec
            if (this.hasParsedALine) {
                this.hasErrors = true;
                throw new ParserException("must not try to add another metadata block definition");
            } else {
                // need to set this here, even BEFORE actual parsing. If the line cannot be parsed,
                // the exception is catched and another line is sent this way, we need to make sure
                // we do not accept it!
                this.hasParsedALine = true;
            }
            
            validateColumns(line.split(config.columnSeparator()));
        }
        
        /**
         * Validate the columns (usually given by {@code parseAndValidateLine}).
         *
         * @param lineParts
         * @throws ParserException
         */
        void validateColumns(final String[] lineParts) throws ParserException {
            if (lineParts == null || lineParts.length != header.size()) {
                throw new ParserException("does not match length of metadata block headline");
            }
            
            ParserException parserException = new ParserException("has validation errors");
            for (int i = 0; i < lineParts.length; i++) {
                Header column = header.get(i);
                String value = lineParts[i];
                if( ! column.isValid(value)) {
                    parserException.addSubException(
                        "Invalid value '" + value + " for column '" + column + "', " + column.getErrorMessage());
                } else {
                    this.settings.put(column, value);
                }
            }
            
            if (parserException.hasSubExceptions()) {
                // setting this to true to ensure no block will be created accidentally via build().
                this.hasErrors = true;
                throw parserException;
            }
        }
        
        public boolean hasFailed() {
            return this.hasErrors || ! this.hasParsedALine;
        }
        
        public boolean hasSucceeded() {
            return ! this.hasErrors && this.hasParsedALine;
        }
        
        /**
         * Execute the builder to create the {@link Block} POJO, containing the representation of the custom metadata
         * block that has been analysed. Will execute associated field builders (which will execute associated
         * vocabulary builders).
         */
        public Block build() {
            if (hasSucceeded()) {
                // TODO: extend with adding the necessary bits about block properties, contained fields etc.
                return new Block();
            } else {
                throw new IllegalStateException("Trying to build a block with errors or without parsing a line first");
            }
        }
    }
    
    private Block() {}
    
    Optional<List<Field>> fields = Optional.empty();
}
