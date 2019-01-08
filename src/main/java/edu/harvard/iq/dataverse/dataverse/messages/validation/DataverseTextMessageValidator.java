package edu.harvard.iq.dataverse.dataverse.messages.validation;

import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseTextMessageDto;
import edu.harvard.iq.dataverse.util.DataverseClock;
import edu.harvard.iq.dataverse.util.DateUtil;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Validator for new and reuse dataverse text messages.
 * @author tjanek
 */
public class DataverseTextMessageValidator {

    public static void validateEndDate(DataverseTextMessageDto dto) {
        if (dto.getFromTime() == null || dto.getToTime() == null) {
            return;
        }
        if (dto.getToTime().before(dto.getFromTime())) {
            throw new EndDateMustNotBeEarlierThanStartingDate();
        }
        LocalDateTime now = DataverseClock.now();
        if (!dto.getToTime().after(DateUtil.convertToDate(now))) {
            throw new EndDateMustBeAFutureDate();
        }
    }
}
