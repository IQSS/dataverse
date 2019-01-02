package edu.harvard.iq.dataverse.dataverse.messages.dto;

public class DataverseLocalizedMessageDto {

    public DataverseLocalizedMessageDto(String locale, String message) {
        this.locale = locale;
        this.message = message;
    }

    private String locale;

    private String message;

    public String getLocale() {
        return locale;
    }

    public String getMessage() {
        return message;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
