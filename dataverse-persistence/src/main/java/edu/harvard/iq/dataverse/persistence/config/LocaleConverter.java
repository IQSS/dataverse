package edu.harvard.iq.dataverse.persistence.config;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Locale;

/**
 * JPA converter of {@link Locale}. It allows
 * to have entity with {@link Locale} field which
 * will be saved in database as varchar.
 *
 * @author madryk
 */
@Converter
public class LocaleConverter implements AttributeConverter<Locale, String> {

    //-------------------- LOGIC --------------------

    @Override
    public String convertToDatabaseColumn(Locale locale) {
        if (locale != null) {
            return locale.toLanguageTag();
        }
        return null;
    }

    @Override
    public Locale convertToEntityAttribute(String languageTag) {
        if (languageTag != null && !languageTag.isEmpty()) {
            return Locale.forLanguageTag(languageTag);
        }
        return null;
    }
}
