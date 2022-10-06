package edu.harvard.iq.dataverse.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Stateless
public class NotificationParametersUtil {
    private ObjectMapper objectMapper;
    private static TypeReference<HashMap<String, String>> parametersTypeRef = new TypeReference<HashMap<String, String>>() {};

    // -------------------- CONSTRUCTORS --------------------

    public NotificationParametersUtil() {
        this.objectMapper = new ObjectMapper();
    }

    // -------------------- LOGIC --------------------

    public Map<String, String> getParameters(UserNotification notification) {
        String json = notification.getParameters();
        return StringUtils.isNotBlank(json) ? getParametersMap(json) : new HashMap<>();
    }

    public void setParameters(UserNotification notification, Map<String, String> parameters) {
        notification.setParameters(parametersToString(parameters));
    }

    // -------------------- PRIVATE --------------------

    private Map<String, String> getParametersMap(String json) {
        try {
            return objectMapper.readValue(json, parametersTypeRef);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private String parametersToString(Map<String, String> parametersMap) {
        try {
            return objectMapper.writeValueAsString(parametersMap);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }
}
