package edu.harvard.iq.dataverse.search.advanced;

import edu.harvard.iq.dataverse.common.BundleUtil;
import io.vavr.control.Either;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@FacesValidator(value = "dateRangeValidator")
public class DateRangeValidator implements Validator {

    private static final String[] DATE_FORMATS = {"yyyy", "-yyyy", "yyyy-MM", "-yyyy-MM", "yyyy-MM-dd", "-yyyy-MM-dd"};
    private static final String DATE_PATTERN = "^(\\-?)[0-9]{4}((\\-)([0-9]{2})){0,2}$";

    // -------------------- LOGIC --------------------
    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        UIInput dateFromInput = (UIInput) component.getAttributes().get("dateFrom");
        UIInput dateToInput = (UIInput) component.getAttributes().get("dateTo");

        Object dateFromValue = dateFromInput.getSubmittedValue();
        Object dateToValue = dateToInput.getSubmittedValue();

        Optional<LocalDate> dateFrom = validateDate(context, dateFromInput, dateFromValue);
        Optional<LocalDate> dateTo = validateDate(context, dateToInput, dateToValue);

        if(dateFrom.isPresent() && dateTo.isPresent() && !isValidDateRange(dateFrom.get(), dateTo.get(), dateToValue.toString())) {
            dateFromInput.setValid(false);
            dateToInput.setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("advanced.search.wrong.daterange.badRange"));
            context.addMessage(dateFromInput.getClientId(context), message);
            context.addMessage(dateToInput.getClientId(context), message);
        }
    }

    // -------------------- PRIVATE ---------------------
    private Optional<LocalDate> validateDate(FacesContext context, UIComponent comp, Object value) {
        if(value == null || value.toString().isEmpty()) {
            return Optional.empty();
        }

        if(isDatePatternCorrect(value).isLeft() || isDateValueCorrect(value).isLeft()) {
            ((UIInput) comp).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("advanced.search.wrong.daterange.format"));
            context.addMessage(comp.getClientId(context), message);
            return Optional.empty();
        }

        return Optional.ofNullable(LocalDate.parse(getFullDateLiteral(value.toString()), DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private boolean isValidDateRange(LocalDate dateFrom, LocalDate dateTo, String dateToValue) {
        boolean isValid = true;
        if(dateFrom != null && dateTo != null) {
            isValid = dateFrom.isBefore(movePartialDateToUpperLimit(dateTo, dateToValue));
        }
        return isValid;
    }

    private Either<String, Boolean> isDateValueCorrect(Object value) {
        try {
            DateUtils.parseDateStrictly(value.toString(), DATE_FORMATS);
            return Either.right(true);
        } catch(ParseException  pe) {
            return Either.left("Invalid date inserted. Given value: " + value.toString() + ". Acceptable formats: " + DATE_FORMATS);
        }
    }

    private Either<String, Boolean> isDatePatternCorrect(Object value) {
        return value.toString().matches(DATE_PATTERN) ?  Either.right(true) : Either.left("Invalid date pattern. Passed value: " + value.toString() + " doesn't match pattern " + DATE_PATTERN);
    }

    private LocalDate movePartialDateToUpperLimit(LocalDate date, String partialDateString) {
        if(date == null) {
            return null;
        }
        int formatMode = StringUtils.countMatches(partialDateString.substring(1), "-"); // we can skip counting first '-' indicating BC date

        if(formatMode == 1) { // partial date YYYY-MM
            return moveToLastDayOfMonth(date);
        } else if(formatMode == 0) { // partial date YYYY
            return moveToLastDayOfMonth(moveToLastMonthOfYear(date));
        }

        return date; // full date YYYY-MM-DD
    }

    private LocalDate moveToLastMonthOfYear(LocalDate date) {
        return date.plus(1, ChronoUnit.YEARS).minus(1, ChronoUnit.MONTHS);
    }

    private LocalDate moveToLastDayOfMonth(LocalDate date) {
        return date.plus(1, ChronoUnit.MONTHS).minus(1, ChronoUnit.DAYS);
    }

    private String getFullDateLiteral(String partialStringLiteral) {
        int formatMode = StringUtils.countMatches(partialStringLiteral.substring(1), "-");

        for(int i=formatMode; i<2; i++) {
            partialStringLiteral += "-01";
        }
        return partialStringLiteral;
    }
}
