package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.faces.context.FacesContext;

import java.time.Instant;
import java.util.Date;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DateUIUtilsTest {

    private FacesContext facesContext;
    
    private Date date = Date.from(Instant.parse("2007-12-03T16:15:30.00Z"));
    
    @BeforeEach
    void beforeEach() {
        facesContext = FacesContextMocker.mockContext();
    }
    
    @AfterEach
    void afterEach() {
        facesContext.release();
    }

    // -------------------- TESTS --------------------

    @Test
    void formatDate() {
        // given
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(Locale.ENGLISH);
        // when & then
        assertThat(DateUIUtils.formatDate(date)).isEqualTo("Dec 3, 2007");
    }

    @Test
    void formatDate_different_locale() {
        // given
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(Locale.forLanguageTag("pl"));
        // when & then
        assertThat(DateUIUtils.formatDate(date)).isEqualTo("2007-12-03");
    }
    
    @Test
    void formatDateTime() {
        // given
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(Locale.ENGLISH);
        // when & then
        assertThat(DateUIUtils.formatDateTime(date)).isEqualTo("Dec 3, 2007 4:15:30 PM UTC");
    }
    
    @Test
    void formatDateTime_different_locale() {
        // given
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(Locale.forLanguageTag("pl"));
        // when & then
        assertThat(DateUIUtils.formatDateTime(date)).isEqualTo("2007-12-03 16:15:30 UTC");
    }
}
