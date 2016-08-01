package edu.harvard.iq.dataverse.validation;

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

    /** Error code for password big lenght. */
    static final String ERROR_CODE_BIG = "BIG_TOO_SHORT";
    static final String ERROR_MESSAGE_BIG = "Note: password are always valid with a %1$s or more character length regardless.";

    @Override
    public RuleResult validate(PasswordData passwordData) {
        final RuleResult result = super.validate(passwordData);
        if ( !result.isValid()) {
            result.getDetails().clear();
            result.getDetails().add(new RuleResultDetail(ERROR_CODE_BIG, createRuleResultDetailParameters()));
        }
        return result;
    }

}
