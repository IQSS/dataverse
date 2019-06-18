package edu.harvard.iq.dataverse.bannersandmessages.messages.dto;

import org.hibernate.validator.constraints.NotBlank;

import java.util.Objects;

public class DataverseLocalizedMessageDto {

    public DataverseLocalizedMessageDto(String locale, String message, String language) {
        this.locale = locale;
        this.message = message;
        this.language = language;
    }

    private String locale;

    @NotBlank(message = "{field.required}")
    private String message;

    private String language;

    public String getLocale() {
        return locale;
    }

    public String getMessage() {
        return message;
    }

    public String getLanguage() {
        return language;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataverseLocalizedMessageDto that = (DataverseLocalizedMessageDto) o;
        return Objects.equals(locale, that.locale) &&
                Objects.equals(message, that.message) &&
                Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locale, message, language);
    }
}
