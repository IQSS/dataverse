package edu.harvard.iq.dataverse.actionlogging;

import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord.ActionType;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord.Result;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionLogServiceBeanTest {

    @InjectMocks
    private ActionLogServiceBean actionLogService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private SystemConfig systemConfig;

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should persist log record")
    void log() {
        // given
        ActionLogRecord logRecord = new ActionLogRecord(ActionType.Command, "subtype");
        // when
        actionLogService.log(logRecord);
        // then
        verify(entityManager).persist(logRecord);
    }

    @Test
    @DisplayName("Should fill log record with default values")
    void log_default_values() {
        // given
        ActionLogRecord logRecord = new ActionLogRecord(ActionType.Command, "subtype");
        // when
        actionLogService.log(logRecord);
        // then
        assertThat(logRecord.getEndTime()).isCloseTo(new Date(), 1000);
        assertThat(logRecord.getActionResult()).isEqualTo(Result.OK);
    }

    @Test
    @DisplayName("Should not overwrite log record values with defaults when already filled")
    void log_default_values_do_not_overwrite() {
        // given
        ActionLogRecord logRecord = new ActionLogRecord(ActionType.Command, "subtype")
                .setEndTime(Date.from(Instant.ofEpochSecond(1_000_000)))
                .setActionResult(Result.BadRequest);
        // when
        actionLogService.log(logRecord);
        // then
        assertThat(logRecord.getEndTime()).isEqualTo(Date.from(Instant.ofEpochSecond(1_000_000)));
        assertThat(logRecord.getActionResult()).isEqualTo(Result.BadRequest);
    }

    @Test
    @DisplayName("Should not persist log record when readonly mode is on")
    void log_readonly_mode() {
        // given
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        ActionLogRecord logRecord = new ActionLogRecord(ActionType.Command, "subtype");
        // when
        actionLogService.log(logRecord);
        // then
        verifyZeroInteractions(entityManager);
    }
}
