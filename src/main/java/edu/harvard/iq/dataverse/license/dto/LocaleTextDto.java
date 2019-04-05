package edu.harvard.iq.dataverse.license.dto;

import java.util.Locale;

public class LocaleTextDto {

    private Locale locale;

    private String text;

    // -------------------- CONSTRUCTORS --------------------

    public LocaleTextDto(Locale locale, String text) {
        this.locale = locale;
        this.text = text;
    }

    // -------------------- GETTERS --------------------

    public Locale getLocale() {
        return locale;
    }

    public String getText() {
        return text;
    }

    // -------------------- SETTERS --------------------

    public void setText(String text) {
        this.text = text;
    }
}
