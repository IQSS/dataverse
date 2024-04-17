package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Stateless
public class RorValidator {
    public static final String INVALID_FORMAT_ERROR_CODE = "ror.invalid.format";
    public static final String INVALID_CHECKSUM_ERROR_CODE = "ror.invalid.checksum";
    private static final Map<String, Integer> DECODE_SYMBOL_VALUES = Initializer.initializeDecodeSymbolValues();

    // -------------------- LOGIC --------------------

    public ValidationResult validate(String fullRor) {
        if (StringUtils.isBlank(fullRor)
            || !fullRor.matches("https://ror\\.org/0[a-hjkmnp-tv-z0-9]{6}[0-9]{2}")) {
            return ValidationResult.invalid(INVALID_FORMAT_ERROR_CODE);
        }
        String value = fullRor.substring(fullRor.lastIndexOf("/") + 1);
        String encoded = value.substring(0, 7);
        long checksum = Long.parseLong(value.substring(7));
        return checksum == computeChecksum(encoded)
                ? ValidationResult.ok()
                : ValidationResult.invalid(INVALID_CHECKSUM_ERROR_CODE);
    }

    // -------------------- PRIVATE --------------------

    private long computeChecksum(String encoded) {
        long decoded = Arrays.stream(encoded.split(""))
                .mapToLong(DECODE_SYMBOL_VALUES::get)
                .reduce(0L, (accumulated, element) -> 32L * accumulated + element);
        return 98L - ((decoded * 100L) % 97);
    }

    // -------------------- INNER CLASSES --------------------

    static class Initializer {
        static Map<String, Integer> initializeDecodeSymbolValues() {
            String symbols = "0123456789abcdefghjkmnpqrstvwxyz";
            return Collections.unmodifiableMap(
                    Arrays.stream(symbols.split(""))
                            .collect(Collectors.toMap(Function.identity(), symbols::indexOf)));
        }
    }
}
