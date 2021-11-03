package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabAlternate;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;


public class MetadataBlockTsvCreator {

    private static final Logger logger = LoggerFactory.getLogger(MetadataBlockTsvCreator.class);

    private static final CSVFormat TSV = CSVFormat.DEFAULT
            .withEscape('\\')
            .withQuote(null)
            .withQuoteMode(QuoteMode.NONE)
            .withDelimiter('\t');

    // -------------------- LOGIC --------------------

    public void createTsv(MetadataBlock metadataBlock, OutputStream outputStream) {
        try (Writer writer = new OutputStreamWriter(outputStream);
             BufferedWriter streamWriter = new BufferedWriter(writer);
             CSVPrinter csvPrinter = new CSVPrinter(streamWriter, TSV)) {
                printMetadataBlockRecord(csvPrinter, metadataBlock);
                printDatasetFieldTypeRecords(csvPrinter, metadataBlock);
                printVocabularyRecords(csvPrinter, metadataBlock);
        } catch (IOException ioe) {
            logger.warn("Exception during TSV file creation: ", ioe);
        }
    }

    // -------------------- PRIVATE --------------------

    private void printMetadataBlockRecord(CSVPrinter csvPrinter, MetadataBlock metadataBlock) throws IOException {
        MetadataBlockRecord[] columns = MetadataBlockRecord.values();
        printHeader(csvPrinter, columns);
        csvPrinter.getOut().append("\t");
        List<Object> values = Arrays.stream(columns)
                .map(c -> c.valueGetter().apply(metadataBlock))
                .collect(Collectors.toList());
        csvPrinter.printRecord(values);
    }

    private void printHeader(CSVPrinter csvPrinter, Record[] columns) throws IOException {
        if (columns.length < 1) {
            return;
        }
        csvPrinter.getOut().append("#")
                .append(columns[0].recordName())
                .append("\t")
                .append(Arrays.stream(columns)
                        .map(Record::columnName)
                        .collect(Collectors.joining("\t")));
        csvPrinter.println();
    }

    private void printDatasetFieldTypeRecords(CSVPrinter csvPrinter, MetadataBlock metadataBlock) throws IOException {
        List<DatasetFieldType> fieldTypes = metadataBlock.getDatasetFieldTypes();
        fieldTypes.sort(Comparator.comparingInt(DatasetFieldType::getDisplayOrder));
        DatasetFieldTypeRecord[] columns = DatasetFieldTypeRecord.values();
        printHeader(csvPrinter, columns);
        for (DatasetFieldType fieldType : fieldTypes) {
            csvPrinter.getOut().append("\t");
            List<Object> values = Arrays.stream(columns)
                    .map(c -> c.formatter().apply(c.valueGetter().apply(fieldType)))
                    .collect(Collectors.toList());
            csvPrinter.printRecord(values);
        }
    }

    private void printVocabularyRecords(CSVPrinter csvPrinter, MetadataBlock metadataBlock) throws IOException {
        ControlledVocabularyRecord[] columns = ControlledVocabularyRecord.values();
        printHeader(csvPrinter, columns);
        List<DatasetFieldType> fieldTypes = metadataBlock.getDatasetFieldTypes().stream()
                .filter(DatasetFieldType::isControlledVocabulary)
                .sorted(Comparator.comparingInt(DatasetFieldType::getDisplayOrder))
                .collect(Collectors.toList());
        List<ControlledVocabularyValue> vocabularyValues = fieldTypes.stream()
                .flatMap(f -> f.getControlledVocabularyValues().stream()
                        .sorted(Comparator.comparingInt(ControlledVocabularyValue::getDisplayOrder)))
                .collect(Collectors.toList());
        for (ControlledVocabularyValue vocabularyValue : vocabularyValues) {
            csvPrinter.getOut().append("\t");
            List<Object> values = Arrays.stream(columns)
                    .map(c -> c.valueGetter().apply(vocabularyValue))
                    .collect(Collectors.toList());
            Collection<String> alternates = vocabularyValue.getControlledVocabAlternates().stream()
                    .sorted(Comparator.comparingLong(ControlledVocabAlternate::getId))
                    .map(ControlledVocabAlternate::getStrValue)
                    .collect(Collectors.toList());
            if (!alternates.isEmpty()) {
                values.addAll(alternates);
            }
            csvPrinter.printRecord(values);
        }
    }

    // -------------------- INNER CLASSES --------------------

    interface Record {
        String recordName();
        String columnName();
    }

    static class Formatters {
        static UnaryOperator<Object> toUpperFormatter = c -> String.valueOf(c).toUpperCase();
        static UnaryOperator<Object> toLowerFormatter = c -> String.valueOf(c).toLowerCase();
    }

    enum MetadataBlockRecord implements Record {
        NAME("name", MetadataBlock::getName),
        DATAVERSE_ALIAS("dataverseAlias", m -> m.getOwner() != null ? m.getOwner().getAlias() : ""),
        DISPLAY_NAME("displayName", MetadataBlock::getDisplayName),
        BLOCK_URI("blockURI", MetadataBlock::getNamespaceUri);

        MetadataBlockRecord(String columnName, Function<MetadataBlock, Object> valueGetter) {
            this.columnName = columnName;
            this.valueGetter = valueGetter;
        }

        private String columnName;

        private Function<MetadataBlock, Object> valueGetter;

        @Override
        public String recordName() {
            return "metadataBlock";
        }

        @Override
        public String columnName() {
            return columnName;
        }

        Function<MetadataBlock, Object> valueGetter() {
            return valueGetter;
        }
    }

    enum DatasetFieldTypeRecord implements Record {
        NAME("name", DatasetFieldType::getName),
        TITLE("title", DatasetFieldType::getTitle),
        DESCRIPTION("description", DatasetFieldType::getDescription),
        WATERMARK("watermark", DatasetFieldType::getWatermark),
        FIELD_TYPE("fieldType", DatasetFieldType::getFieldType, Formatters.toLowerFormatter),
        DISPLAY_ORDER("displayOrder", DatasetFieldType::getDisplayOrder),
        DISPLAY_FORMAT("displayFormat", DatasetFieldType::getDisplayFormat),
        ADVANCED_SEARCH_FIELD("advancedSearchField", DatasetFieldType::isAdvancedSearchFieldType, Formatters.toUpperFormatter),
        ALLOW_CONTROLLED_VOCABULARY("allowControlledVocabulary", DatasetFieldType::isAllowControlledVocabulary, Formatters.toUpperFormatter),
        ALLOW_MULTIPLES("allowmultiples", DatasetFieldType::isAllowMultiples, Formatters.toUpperFormatter),
        FACETABLE("facetable", DatasetFieldType::isFacetable, Formatters.toUpperFormatter),
        DISPLAY_ON_CREATE("displayoncreate", DatasetFieldType::isDisplayOnCreate, Formatters.toUpperFormatter),
        REQUIRED("required", DatasetFieldType::isRequired, Formatters.toUpperFormatter),
        PARENT("parent", t -> t.getParentDatasetFieldType() != null ? t.getParentDatasetFieldType().getName() : ""),
        INPUT_RENDERER_TYPE("inputRendererType", DatasetFieldType::getInputRendererType),
        INPUT_RENDERER_OPTIONS("inputRendererOptions", DatasetFieldType::getInputRendererOptions),
        METADATABLOCK_ID("metadatablock_id", t -> t.getMetadataBlock().getName()),
        TERM_URI("termURI", DatasetFieldType::getUri),
        VALIDATION("validation", DatasetFieldType::getValidation);

        DatasetFieldTypeRecord(String columnName, Function<DatasetFieldType, Object> valueGetter, UnaryOperator<Object> formatter) {
            this.columnName = columnName;
            this.valueGetter = valueGetter;
            this.formatter = formatter;
        }

        DatasetFieldTypeRecord(String columnName, Function<DatasetFieldType, Object> valueGetter) {
            this.columnName = columnName;
            this.valueGetter = valueGetter;
            this.formatter = UnaryOperator.identity();
        }

        private String columnName;

        private Function<DatasetFieldType, Object> valueGetter;

        private UnaryOperator<Object> formatter;

        @Override
        public String recordName() {
            return "datasetField";
        }

        @Override
        public String columnName() {
            return columnName;
        }

        public Function<DatasetFieldType, Object> valueGetter() {
            return valueGetter;
        }

        public UnaryOperator<Object> formatter() {
            return formatter;
        }
    }

    enum ControlledVocabularyRecord implements Record {
        DATASET_FIELD("DatasetField", v -> v.getDatasetFieldType().getName()),
        VALUE("Value", ControlledVocabularyValue::getStrValue),
        IDENTIFIER("identifier", ControlledVocabularyValue::getIdentifier),
        DISPLAY_ORDER("displayOrder", ControlledVocabularyValue::getDisplayOrder);

        ControlledVocabularyRecord(String columnName, Function<ControlledVocabularyValue, Object> valueGetter) {
            this.columnName = columnName;
            this.valueGetter = valueGetter;
        }

        private String columnName;

        private Function<ControlledVocabularyValue, Object> valueGetter;

        @Override
        public String recordName() {
            return "controlledVocabulary";
        }

        @Override
        public String columnName() {
            return columnName;
        }

        public Function<ControlledVocabularyValue, Object> valueGetter() {
            return valueGetter;
        }
    }
}
