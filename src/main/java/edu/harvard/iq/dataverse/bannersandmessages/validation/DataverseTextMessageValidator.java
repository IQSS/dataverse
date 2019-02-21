package edu.harvard.iq.dataverse.bannersandmessages.validation;

import edu.harvard.iq.dataverse.util.DataverseClock;
import edu.harvard.iq.dataverse.util.DateUtil;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Validator for new and reuse dataverse text messages.
 * @author tjanek
 */
public class DataverseTextMessageValidator {

    public static void validateEndDate(Date fromTime, Date toTime) {
        if (fromTime == null || toTime == null) {
            return;
        }
        if (toTime.before(fromTime)) {
            throw new EndDateMustNotBeEarlierThanStartingDate();
        }
        LocalDateTime now = DataverseClock.now();
        if (!toTime.after(DateUtil.convertToDate(now))) {
            throw new EndDateMustBeAFutureDate();
        }
    }
}
