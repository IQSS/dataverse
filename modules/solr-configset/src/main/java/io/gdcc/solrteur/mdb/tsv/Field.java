package io.gdcc.solrteur.mdb.tsv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Field {
    
    public static final String KEYWORD = "datasetField";
    
    /**
     * Currently, the spec says we need to comply with Solr and DDI rules.
     * De-facto commonly used are camel-cased names.
     * See also https://solr.apache.org/guide/8_11/defining-fields.html#field-properties
     *
     * This regex matches the strict Solr name spec plus adds "." as valid chars
     * (we need it for comptability with astrophysics.tsv)
     */
    public static final String NAME_PATTERN = "^(?=_[\\w.]+[^_]$|[^_][\\w.]+_?$)[a-zA-Z_][\\w.]+$";
    public static final String WHITESPACE_ONLY = "^\\s+$";
    
    public static final Predicate<String> matchBoolean = s -> "TRUE".equals(s) || "FALSE".equals(s);
    public static final Predicate<String> matchParentDisplayFormat = Pattern.compile("^[;,:]?$").asMatchPredicate();
    
    /**
     * Programmatic variant of the spec of a #datasetField. List all the column headers and associated restrictions
     * on the values of a column.
     */
    public enum Header {
        KEYWORD(
            Field.KEYWORD,
            String::isEmpty,
            "must have no value (be empty)"
        ),
        
        NAME(
            "name",
            Predicate.not(String::isBlank).and(Pattern.compile(Field.NAME_PATTERN).asMatchPredicate()),
            "must not be blank and match regex pattern " + Field.NAME_PATTERN
        ),
        TITLE("title"),
        DESCRIPTION("description"),
        WATERMARK(
            "watermark",
            Predicate.not(Pattern.compile(WHITESPACE_ONLY).asMatchPredicate()),
            "must not be whitespace only"
        ),
        
        FIELDTYPE(
            "fieldType",
            Types.matchesTypes(),
            "must be one of [" + String.join(", ", Types.getTypesList()) + "]"
        ),
        
        DISPLAY_ORDER(
            "displayOrder",
            Pattern.compile("\\d+").asMatchPredicate(),
            "must be a non-negative integer"
        ),
        DISPLAY_FORMAT(
            "displayFormat",
            Predicate.not(Pattern.compile(WHITESPACE_ONLY).asMatchPredicate()),
            "must not be whitespace only"
        ),
        
        ADVANCED_SEARCH_FIELD("advancedSearchField", matchBoolean, "must be 'TRUE' or 'FALSE"),
        ALLOW_CONTROLLED_VOCABULARY("allowControlledVocabulary", matchBoolean, "must be 'TRUE' or 'FALSE"),
        ALLOW_MULTIPLES("allowmultiples", matchBoolean, "must be 'TRUE' or 'FALSE"),
        FACETABLE("facetable", matchBoolean, "must be 'TRUE' or 'FALSE"),
        DISPLAY_ON_CREATE("displayoncreate", matchBoolean, "must be 'TRUE' or 'FALSE"),
        REQUIRED("required", matchBoolean, "must be 'TRUE' or 'FALSE"),
        
        PARENT(
            "parent",
            Pattern.compile(Field.NAME_PATTERN).asMatchPredicate().or(String::isEmpty),
            "must be either empty or match regex pattern " + Field.NAME_PATTERN
        ),
        METADATABLOCK_ID(
            "metadatablock_id",
            Predicate.not(String::isBlank).and(Pattern.compile(Block.NAME_PATTERN).asMatchPredicate()),
            "must not be blank and match regex pattern " + Block.NAME_PATTERN
        ),
        TERM_URI(
            "termURI",
            s -> s.isEmpty() || Validator.isValidUrl(s),
            "must be empty or a valid URL"
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
            return Arrays.stream(Header.values()).map(Header::toString).collect(Collectors.toUnmodifiableList());
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
    
    public static final class FieldsBuilder {
        private final Configuration config;
        private final String containingBlockName;
        private final List<Header> header;
        private final List<Field> fields;
        
        private int parsedLines = 0;
    
        /**
         * Create a parser for field lines. As this is  on the block it
         * @param headerLine
         * @param containingBlockName
         * @param config
         * @throws ParserException
         */
        public FieldsBuilder(final String headerLine, String containingBlockName, final Configuration config) throws ParserException {
            this.header = Header.parseAndValidate(headerLine, config);
            this.containingBlockName = containingBlockName;
            this.config = config;
            this.fields = new ArrayList<>();
        }
        
        /**
         * Analyse a line containing a concrete dataset field definition by parsing and validating it.
         *
         * This will fail:
         * - when the line is null or blanks only
         * - when the columns within the line do not match the length of the header
         * - when the column values do not match the column type restrictions (as implied by the header)
         *
         * The exception might contain sub-exceptions, as the parser will do its best to keep going and find as many
         * problems as possible to avoid unnecessary (pesky) re-iterations.
         *
         * Beware:
         * 1. This method does not check for parent/child dependencies, as the spec does not say parent fields
         *    must be defined first. This needs to be done by {@link #build()}, as this is the indication all lines
         *    have been attempted to parse.
         *
         * @param line The dataset field definition line to analyse.
         * @throws ParserException If the parsing fails (see description).
         */
        public void parseAndValidateLine(final int lineIndex, final String line) throws ParserException {
            // no null or blank lines for the parser. (blank lines can be skipped and not sent here by calling code)
            if (line == null || line.isBlank()) {
                throw new ParserException("must not be empty nor blanks only nor null.").withLineNumber(lineIndex);
            }
            
            // save the field for further builder internal manipulation
            // (if validation fails, the exception will prevent it)
            this.fields.add(parseAndValidateColumns(lineIndex, line.split(config.columnSeparator())));
        }
        
        /**
         * Parse and validate the columns (usually given by {@code parseAndValidateLine}).
         * This is package private, becoming testable this way.
         *
         * @param lineParts
         * @return A {@link Block} object (modifiable for builder internal use)
         * @throws ParserException
         */
        Field parseAndValidateColumns(final int lineIndex, final String[] lineParts) throws ParserException {
            if (lineParts == null || lineParts.length > header.size()) {
                throw new ParserException("Not matching length of dataset fields headline");
            }
            
            Field field = new Field(lineIndex);
            ParserException parserException = new ParserException("Has validation errors:").withLineNumber(lineIndex);
            
            for (int i = 0; i < lineParts.length; i++) {
                Field.Header column = header.get(i);
                String value = lineParts[i];
                if( ! column.isValid(value)) {
                    parserException.addSubException(
                        "Invalid value '" + value + "' for column '" + column + "', " + column.getErrorMessage());
                } else {
                    field.set(column, value);
                }
            }
            
            // TODO: extend with the possibility to reference a different, but existing other block (also not recommended)
            // check the metadata block reference matches the block this field is defined in
            field.get(Header.METADATABLOCK_ID).ifPresent(id -> {
                if (! this.containingBlockName.equals(id)) {
                    parserException.addSubException(
                        "Metadata block reference '" + id +
                            "' does not match parsed containing block '" + this.containingBlockName + "'");
                }
            });
            
            if (parserException.hasSubExceptions()) {
                throw parserException;
            } else {
                return field;
            }
        }
        
        // TODO: extract distinct checks and move to separate methods to reduce complexity
        public List<Field> build() throws ParserException {
            // Check if all fields are unique within this block
            List<String> duplicates = fields.stream()
                .collect(Collectors.groupingBy(Field::getName))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableList());
            
            // This is critical - we cannot build relations when duplicates are present. Step away now.
            if (!duplicates.isEmpty()) {
                throw new ParserException("Found duplicate field definitions for '" + String.join("', '", duplicates) + "'");
            }
            
            // Create parent / child relations for compound fields and validate
            // a) Get all fields that claim to have a parent
            List<Field> children = fields.stream()
                .filter(field -> !field.get(Header.PARENT, "").isEmpty())
                .collect(Collectors.toUnmodifiableList());
    
            ParserException parentException = new ParserException("Compound field errors:");
            // b) Search and associate the parent field to these children
            for (Field child : children) {
                // Always non-null as we filtered for this before, so will never be "" (but we avoid the Optional)
                String parentName = child.get(Header.PARENT, "");
                
                Optional<Field> parentCandidate = fields.stream()
                    .filter(field -> field.getName().equals(parentName))
                    .findFirst();
                
                if (parentCandidate.isEmpty()) {
                    parentException.addSubException(
                        new ParserException("'" + child.getName() + "' misses its parent '" + parentName + "'")
                            .withLineNumber(child.lineIndex));
                } else {
                    // TODO: when this parsing is extended to allow fields belonging to other blocks, this would
                    //       need extension to verify the parent candidate is within the same metadata block
                    Field parent = parentCandidate.get();
                    
                    // Associate children and parents
                    child.parent = parent;
                    parent.children.add(child);
                }
            }
            
            // Check no cyclic dependencies present / deep nesting is optional.
            // First, fetch fields that are children and parents at the same time:
            List<Field> parentChilds = fields.stream()
                .filter(field -> field.isParent() && field.isChild())
                .collect(Collectors.toUnmodifiableList());
            
            // When deep nesting is disabled, this list must be empty (only 1 level of nesting allowed means
            // there cannot be any parents that are children at the same time)
            if (!config.deepFieldNestingEnabled() && !parentChilds.isEmpty()) {
                parentChilds.forEach(field ->
                    parentException.addSubException(
                        new ParserException("'" + field.getName() + "' is not allowed to be parent and child at once")
                            .withLineNumber(field.lineIndex)));
            // With deep nesting enabled, cyclic dependencies might happen. Need to recurse over these elements.
            } else if (config.deepFieldNestingEnabled() && !parentChilds.isEmpty()) {
                parentChilds.forEach(field -> {
                    if (hasCyclicDependency(field, field.parent)) {
                        parentException.addSubException(
                            new ParserException("'" + field.getName() + "' is part of a cyclic dependency")
                                .withLineNumber(field.lineIndex));
                    }
                });
            }
            
            // Ensure compound parents are a) of type "none", b) have no displayFormat other than ":", ";" or "," and
            // are c) not facetable and d) not allowed to use a CV
            List<Field> parents = fields.stream()
                .filter(Field::isParent)
                .collect(Collectors.toUnmodifiableList());
            
            parents.forEach(field -> {
                // a) type none
                // The first check shall never be true, but just in case...
                if (field.get(Header.FIELDTYPE).isEmpty() || !Types.NONE.test(field.get(Header.FIELDTYPE).get())) {
                    parentException.addSubException(
                        new ParserException("'" + field.getName() + "' is a parent but does not have 'fieldType'='none'")
                            .withLineNumber(field.lineIndex));
                }
                // b) displayFormat contains only ":", ";" or "," or empty
                if (field.get(Header.DISPLAY_FORMAT).isEmpty() || !matchParentDisplayFormat.test(field.get(Header.DISPLAY_FORMAT).get())) {
                    parentException.addSubException(
                        new ParserException("'" + field.getName() + "' is a parent but 'displayFormat' is not empty or from [;,:]")
                            .withLineNumber(field.lineIndex));
                }
                // c) not facetable
                if (field.get(Header.FACETABLE).isEmpty() || field.get(Header.FACETABLE).get().equals("TRUE")) {
                    parentException.addSubException(
                        new ParserException("'" + field.getName() + "' is a parent but has 'facetable'='TRUE'")
                            .withLineNumber(field.lineIndex));
                }
                // c) not using a CV
                if (field.get(Header.ALLOW_CONTROLLED_VOCABULARY).isEmpty() || field.get(Header.ALLOW_CONTROLLED_VOCABULARY).get().equals("TRUE")) {
                    parentException.addSubException(
                        new ParserException("'" + field.getName() + "' is a parent but has 'allowControlledVocabulary'='TRUE'")
                            .withLineNumber(field.lineIndex));
                }
            });
    
            // TODO: Extend check here with:
            //       1) Unique numbers in displayOrder (attention: compound fields!) - should it print warnings when unsorted?
            //       2) Warnings for facetable fields with suboptimal field types
            //       3) Check if any compound fields are optional but have required children and warn about potential unwanted conditional requirements
            //       4) Check no primitive (non-parent) field has type "none"
            //       5) ...
            
            // Now either die or return fields
            if (parentException.hasSubExceptions()) {
                throw parentException;
            }
            return List.copyOf(fields);
        }
        
        private boolean hasCyclicDependency(final Field root, final Field parent) {
            // Found ourselves - stop recursion here.
            if (parent == root) {
                return true;
            }
            // Simple recursive search. Should be cheap enough for our expected workload.
            if (parent.parent != null) {
                return hasCyclicDependency(root, parent.parent);
            } else {
                return false;
            }
        }
    }
    
    /* ---- Actual Field Class starting here ---- */
    
    private Field parent;
    private final List<Field> children = new ArrayList<>();
    private final Map<Header, String> properties = new EnumMap<>(Header.class);
    private final int lineIndex;
    
    private Field(int lineIndex) {
        this.lineIndex = lineIndex;
    }
    
    private void set(final Header column, final String value) {
        this.properties.put(column, value);
    }
    public Optional<String> get(final Header column) {
        return Optional.ofNullable(properties.get(column));
    }
    public String get(final Header column, final String defaultValue) {
        return properties.getOrDefault(column, defaultValue);
    }
    
    public String getName() {
        // Will always be non-null after creating the field with the builder
        return properties.get(Header.NAME);
    }
    
    public boolean isParent() {
        return !this.children.isEmpty();
    }
    
    public boolean isChild() {
        return this.parent != null;
    }
    
    List<ControlledVocabulary> controlledVocabularyValues = List.of();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Field)) return false;
        Field field = (Field) o;
        return field.getName().equals(this.getName());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }
}
