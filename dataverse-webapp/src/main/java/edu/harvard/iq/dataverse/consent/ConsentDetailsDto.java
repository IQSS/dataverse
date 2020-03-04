package edu.harvard.iq.dataverse.consent;

import java.util.Locale;

public class ConsentDetailsDto {

    private long id;
    private Locale language;
    private String text;
    private boolean accepted;

    // -------------------- CONSTRUCTORS --------------------

    public ConsentDetailsDto(long id, Locale language, String text) {
        this.id = id;
        this.language = language;
        this.text = text;
    }

    // -------------------- GETTERS --------------------

    public long getId() {
        return id;
    }

    public Locale getLanguage() {
        return language;
    }

    public String getText() {
        return text;
    }

    public boolean isAccepted() {
        return accepted;
    }

    // -------------------- SETTERS --------------------

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
