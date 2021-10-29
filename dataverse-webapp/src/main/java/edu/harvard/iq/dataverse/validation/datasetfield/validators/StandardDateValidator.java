package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Eager
@ApplicationScoped
public class StandardDateValidator extends FieldValidatorBase {
    private static final String YYYY_MM_DD_FORMAT = "yyyy-MM-dd";
    private static final String YYYY_MM_FORMAT = "yyyy-MM";
    private static final String YYYY_FORMAT = "yyyy";

    private static final List<DateTimeFormatter> PARSERS = Initializer.initializeParsers();

    @Override
    public String getName() {
        return "standard_date";
    }

    @Override
    public ValidationResult isValid(DatasetField field, Map<String, String> params, Map<String, List<DatasetField>> fieldIndex) {
        String value = field.getValue();
        value = value.startsWith("-") ? value.substring(1) : value;

        for (DateTimeFormatter parser : PARSERS) {
            if (isValidDate(value, parser)) {
                return ValidationResult.ok();
            }
        }
        return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("isNotValidDate",
                field.getDatasetFieldType().getDisplayName(), YYYY_MM_DD_FORMAT, YYYY_MM_FORMAT, YYYY_FORMAT));
    }

    // -------------------- LOGIC --------------------

    private boolean isValidDate(String value, DateTimeFormatter parser) {
        try {
            TemporalAccessor parsed = parser.parse(value);
            int year = parsed.get(ChronoField.YEAR_OF_ERA);
            if (year > 9999) {
                return false;
            }
        } catch (DateTimeException dte) {
            return false;
        }
        return true;
    }

    // -------------------- INNER CLASSES --------------------

    private static class Initializer {
        static List<DateTimeFormatter> initializeParsers() {
            return Stream.of(YYYY_MM_DD_FORMAT, YYYY_MM_FORMAT, YYYY_FORMAT)
                    .map(p -> new DateTimeFormatterBuilder()
                            .appendPattern(p)
                        // following defaults are required to parse or throw exceptions
                        // in the consistent way
                            .parseDefaulting(ChronoField.ERA, 1)
                            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                            .toFormatter()
                            .withResolverStyle(ResolverStyle.STRICT))
                    .collect(Collectors.toList());
        }
    }
}
