package edu.harvard.iq.dataverse.persistence.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, String>, String> {
    private static final Logger logger = LoggerFactory.getLogger(JsonMapConverter.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EMPTY_VALUE = "{}";

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        try {
            return attribute.isEmpty() ? EMPTY_VALUE : objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException jpe) {
            logger.warn("Problem with converting to database column: " + attribute, jpe);
            return EMPTY_VALUE;
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        try {
            return EMPTY_VALUE.equals(dbData)
                    ? Collections.emptyMap()
                    : objectMapper.readValue(dbData, new TypeReference<HashMap<String, String>>() {});
        } catch (IOException ioe) {
            logger.warn("Problem with converting to attribute: " + dbData, ioe);
            return Collections.emptyMap();
        }
    }
}
