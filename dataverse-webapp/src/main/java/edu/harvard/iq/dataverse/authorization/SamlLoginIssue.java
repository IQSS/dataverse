package edu.harvard.iq.dataverse.authorization;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class SamlLoginIssue implements Serializable {

    public final Type type;
    public final Set<String> messages = new HashSet<>();

    // -------------------- CONSTRUCTORS --------------------

    public SamlLoginIssue(Type type) {
        this.type = type;
    }

    // -------------------- GETTER --------------------

    public Type getType() {
        return type;
    }

    public Set<String> getMessages() {
        return messages;
    }

    // -------------------- LOGIC --------------------

    public SamlLoginIssue addMessages(Set<String> messages) {
        this.messages.addAll(messages);
        return this;
    }

    public SamlLoginIssue addMessage(String message) {
        this.messages.add(message);
        return this;
    }

    // -------------------- INNER CLASSES --------------------

    public enum Type {
        INVALID_DATA,
        INCOMPLETE_DATA,
        DUPLICATED_EMAIL,
        AUTHENTICATION_ERROR
    }
}
