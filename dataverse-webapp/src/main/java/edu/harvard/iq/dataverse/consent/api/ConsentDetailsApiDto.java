package edu.harvard.iq.dataverse.consent.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

public class ConsentDetailsApiDto {

    private Long id;
    private Locale language;
    private String text;

    // -------------------- CONSTRUCTORS --------------------

    public ConsentDetailsApiDto(@JsonProperty(value = "id") Long id,
                                @JsonProperty(value = "language", required = true) Locale language,
                                @JsonProperty(value = "text", required = true) String text) {
        Objects.requireNonNull(language);
        Objects.requireNonNull(text);

        this.id = id;
        this.language = language;
        this.text = text;
    }

    // -------------------- GETTERS --------------------

    @Nullable
    public Long getId() {
        return id;
    }

    public Locale getLanguage() {
        return language;
    }

    public String getText() {
        return text;
    }

}
