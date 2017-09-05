package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.util.BundleUtil;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.RuleResult;
import org.passay.RuleResultDetail;


/**
 * GoodStrengthRule
 *
 * This is the same as the LengthRule, expect with a different positive error message.
 */
class GoodStrengthRule extends LengthRule {

    /** Error code for password big length. */
    static final String ERROR_CODE_GOODSTRENGTH = BundleUtil.getStringFromBundle("passwdVal.goodStrengthRule.errorCode");
    static final String ERROR_MESSAGE_GOODSTRENGTH = BundleUtil.getStringFromBundle("passwdVal.goodStrengthRule.errorMsg");

    @Override
    public RuleResult validate(PasswordData passwordData) {
        final RuleResult result = super.validate(passwordData);
        if ( !result.isValid()) {
            result.getDetails().clear();
            result.getDetails().add(new RuleResultDetail(ERROR_CODE_GOODSTRENGTH, createRuleResultDetailParameters()));
        }
        return result;
    }

}
