package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.util.BundleUtil;
import org.passay.PasswordData;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.RuleResultDetail;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ExpirationRule
 * <p>
 * If the password is less than a certain length, then its expiration must be validated too.
 *
 * @author Lucien van Wouw <lwo@iisg.nl>
 */
class ExpirationRule implements Rule {

    /**
     * Error code for password being too short.
     */
    static final String ERROR_CODE_EXPIRED = BundleUtil.getStringFromBundle("passwdVal.expireRule.errorCode");
    static final String ERROR_MESSAGE_EXPIRED = BundleUtil.getStringFromBundle("passwdVal.expireRule.errorMsg");
    private static final long DAY = 86400000L;

    /**
     * expirationMaxLength
     * <p>
     * Password less than this length should be checked for an expiration.
     */
    private int expirationMaxLength;

    /**
     * expirationDays
     * <p>
     * The number of days the password is valid after the passwords last update or creation time.
     */
    private long expirationDays;

    ExpirationRule() {
        this.expirationDays = 365; // Good for one year.
        this.expirationMaxLength = 10;
    }

    ExpirationRule(int expirationMaxLength) {
        this.expirationDays = 365;
        this.expirationMaxLength = expirationMaxLength;
    }

    ExpirationRule(int expirationMaxLength, int expirationDays) {
        this.expirationMaxLength = expirationMaxLength;
        this.expirationDays = expirationDays;
    }

    @Override
    public RuleResult validate(PasswordData passwordData) {

        final RuleResult result = new RuleResult();

        if (expirationMaxLength > 0 && passwordData.getPassword().length() < expirationMaxLength) {
            long slidingExpiration = DAY * expirationDays;
            long now = new Date().getTime();
            String username = passwordData.getUsername(); // Admittedly, we abuse the username here to hold the modification time.
            long passwordModificationTime = Long.parseLong(username);
            long expirationTime = passwordModificationTime + slidingExpiration;
            boolean valid = passwordModificationTime == 0 || expirationTime >= now;
            result.setValid(valid);
            if (!valid) {
                result.getDetails().add(new RuleResultDetail(ERROR_CODE_EXPIRED, createRuleResultDetailParameters()));
            }
        } else {
            result.setValid(true);
        }

        return result;
    }

    /**
     * Creates the parameter data for the rule result detail.
     *
     * @return map of parameter name to value
     */
    private Map<String, Object> createRuleResultDetailParameters() {
        final Map<String, Object> m = new LinkedHashMap<>(1);
        m.put("expirationDays", expirationDays);
        return m;
    }
}
