package edu.harvard.iq.dataverse.persistence.config;

import io.vavr.control.Try;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converter used to convert string to json for Postgres db.
 * If you would try to use string directly, you would receive db exception "is of type json but expression is of type character varying".
 */
@Converter
public class PostgresJsonConverter implements AttributeConverter<String, PGobject> {

    private static final Logger logger = Logger.getLogger(PostgresJsonConverter.class.getName());

    @Override
    public PGobject convertToDatabaseColumn(String attribute) {

        PGobject pGobject = new PGobject();
        pGobject.setType("json");
        Try.run(() -> pGobject.setValue(attribute))
                .onFailure(throwable -> logger.log(Level.SEVERE,
                                                   "There was a problem with converting string to postgres json column ",
                                                   throwable));

        return pGobject;
    }

    @Override
    public String convertToEntityAttribute(PGobject dbData) {
        return dbData.getValue();
    }
}
