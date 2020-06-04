package edu.harvard.iq.dataverse.persistence.config;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.Instant;

@Converter(autoApply = true)
public class InstantConverter implements AttributeConverter<Instant, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(Instant attribute) {
        if (attribute == null) {
            return null;
        }
        return new Timestamp(attribute.toEpochMilli());
    }

    @Override
    public Instant convertToEntityAttribute(Timestamp dbData) {
        if (dbData == null) {
            return null;
        }
        return Instant.ofEpochMilli(dbData.getTime());
    }
}
