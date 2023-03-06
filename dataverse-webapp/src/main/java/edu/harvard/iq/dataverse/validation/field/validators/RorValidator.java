package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Eager
@ApplicationScoped
public class RorValidator extends MultiValueValidatorBase {

    private static final Map<String, Integer> DECODE_SYMBOL_VALUES = Initializer.initializeDecodeSymbolValues();

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return "ror_validator";
    }

    @Override
    public ValidationResult isValueValid(String fullRor, ValidatableField field, Map<String, Object> params,
                                         Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        String fieldName = field.getDatasetFieldType().getDisplayName();
        if (StringUtils.isBlank(fullRor)
            || !fullRor.matches("https://ror\\.org/0[a-hjkmnp-tv-z0-9]{6}[0-9]{2}")) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("ror.invalid.format", fieldName));
        }
        String value = fullRor.substring(fullRor.lastIndexOf("/") + 1);
        String encoded = value.substring(0, 7);
        long checksum = Long.parseLong(value.substring(7));
        return checksum == computeChecksum(encoded)
                ? ValidationResult.ok()
                : ValidationResult.invalid(field, BundleUtil.getStringFromBundle("ror.invalid.checksum", fieldName));
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
