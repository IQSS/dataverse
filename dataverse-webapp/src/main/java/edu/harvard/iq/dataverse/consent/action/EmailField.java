package edu.harvard.iq.dataverse.consent.action;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Class used for Jackson deserialization
 */
class EmailField {

    private String email;

    // -------------------- CONSTRUCTORS --------------------

    public EmailField(@JsonProperty(value = "email", required = true) String email) {
        Objects.requireNonNull(email);

        this.email = email;
    }

    // -------------------- GETTERS --------------------

    String getEmail() {
        return email;
    }
}
