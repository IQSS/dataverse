package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class OrcidValidator {

    public static final String INVALID_FORMAT_ERROR_CODE = "orcid.invalid.format";
    public static final String INVALID_CHECKSUM_ERROR_CODE = "orcid.invalid.checksum";

    private static final Pattern ORCID_FORMAT_PATTERN = Pattern.compile("([0-9]{4})-([0-9]{4})-([0-9]{4})-([0-9]{3})([0-9X])");

    // -------------------- LOGIC --------------------

    public ValidationResult validate(String orcid) {
        if (StringUtils.isEmpty(orcid)) {
            return ValidationResult.invalid(INVALID_FORMAT_ERROR_CODE);
        }

        Matcher matcher = ORCID_FORMAT_PATTERN.matcher(orcid);
        if (StringUtils.isBlank(orcid) || !matcher.matches()) {
            return ValidationResult.invalid(INVALID_FORMAT_ERROR_CODE);
        }

        String encoded = matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4);
        String checksum = matcher.group(5);

        return checksum.equals(computeChecksum(encoded))
                ? ValidationResult.ok()
                : ValidationResult.invalid(INVALID_CHECKSUM_ERROR_CODE);
    }

    // -------------------- PRIVATE --------------------

    public String computeChecksum(String baseDigits) {
        int total = Arrays.stream(baseDigits.split(""))
                .mapToInt(Integer::parseInt)
                .reduce(0, (accumulated, digit) -> (accumulated + digit) * 2);

        int result = (12 - total % 11) % 11;
        return result == 10 ? "X" : String.valueOf(result);
    }
}
