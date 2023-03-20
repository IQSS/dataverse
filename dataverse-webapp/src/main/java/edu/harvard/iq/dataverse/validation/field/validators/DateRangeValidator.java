package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class DateRangeValidator extends FieldValidatorBase {

    private static final String[] DATE_FORMATS = {"yyyy", "-yyyy", "yyyy-MM", "-yyyy-MM", "yyyy-MM-dd", "-yyyy-MM-dd"};
    private static final String DATE_PATTERN = "^-?[0-9]{4}(-[0-9]{2}){0,2}$";

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return "date_range";
    }

    @Override
    public ValidationResult validate(ValidatableField field, Map<String, Object> params,
                                     Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        List<String> validatableValues = field.getValidatableValues();
        if (validatableValues.size() != 2) {
            throw new IllegalArgumentException("The field is wrongly configured – exactly two values are expected!");
        }

        String dateFromValue = validatableValues.get(0);
        String dateToValue = validatableValues.get(1);

        Tuple2<LocalDate, Boolean> dateFrom = validateAndParse(dateFromValue);
        if (!dateFrom._2()) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("advanced.search.wrong.daterange.format", dateFromValue));
        }
        Tuple2<LocalDate, Boolean> dateTo = validateAndParse(dateToValue);
        if (!dateTo._2()) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("advanced.search.wrong.daterange.format", dateToValue));
        }
        if (!isValidDateRange(dateFrom._1(), dateTo._1(), dateToValue)) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("advanced.search.wrong.daterange.badRange"));
        }
        return ValidationResult.ok();
    }

    // -------------------- PRIVATE ---------------------

    private Tuple2<LocalDate, Boolean> validateAndParse(String value) {
        if (StringUtils.isBlank(value)) {
            return Tuple.of(null, true);
        }
        if (!isDatePatternCorrect(value) || !isDateCorrect(value)) {
            return Tuple.of(null, false);
        }
        return Tuple.of(LocalDate.parse(getFullDateLiteral(value), DateTimeFormatter.ISO_LOCAL_DATE), true);
    }


    private String getFullDateLiteral(String value) {
        int count = StringUtils.countMatches(value.substring(1), "-");
        switch (count) {
            case 2: return value;
            case 1: return value + "-01";
            case 0: return value + "-01-01";
            default: throw new IllegalArgumentException("Wrong date format");
        }
    }

    private boolean isDatePatternCorrect(String value) {
        return value.matches(DATE_PATTERN);
    }

    private boolean isDateCorrect(Object value) {
        try {
            DateUtils.parseDateStrictly(value.toString(), DATE_FORMATS);
            return true;
        } catch(ParseException pe) {
            return false;
        }
    }

    private boolean isValidDateRange(LocalDate dateFrom, LocalDate dateTo, String dateToValue) {
        return dateFrom == null || dateTo == null || dateFrom.isBefore(movePartialDateToUpperLimit(dateTo, dateToValue));
    }

    private LocalDate movePartialDateToUpperLimit(LocalDate date, String partialDateString) {
        int formatMode = StringUtils.countMatches(partialDateString.substring(1), "-"); // we skip first '-' that indicates BC date

        if (formatMode == 1) {
            // YYYY-MM case
            return moveToLastDayOfMonth(date);
        } else if (formatMode == 0) {
            // YYYY case
            return moveToLastDayOfMonth(moveToLastMonthOfYear(date));
        }
        return date;
    }

    private LocalDate moveToLastMonthOfYear(LocalDate date) {
        return date.plus(1, ChronoUnit.YEARS)
                .minus(1, ChronoUnit.MONTHS);
    }

    private LocalDate moveToLastDayOfMonth(LocalDate date) {
        return date.plus(1, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS);
    }
}
