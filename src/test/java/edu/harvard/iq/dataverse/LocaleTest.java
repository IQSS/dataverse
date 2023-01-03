package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class LocaleTest {
    
    /**
     * Many of our tests require setting the locale and timezone appropriately
     * as formating dates, numbers, etc are based on it. Ensure consistency.
     */
    @Test
    void ensureLocale() {
        Locale l = Locale.getDefault();
        TimeZone tz = TimeZone.getDefault();
    
        System.out.println("user.language="+l.getLanguage());
        System.out.println("user.region="+l.getCountry());
        System.out.println("user.timezone="+tz.getDefault().getID());
        
        assertThat(l.getLanguage(), equalTo("en"));
        assertThat(l.getCountry(), equalTo("US"));
        assertThat(tz.getID(), equalTo("UTC"));
    }
}
