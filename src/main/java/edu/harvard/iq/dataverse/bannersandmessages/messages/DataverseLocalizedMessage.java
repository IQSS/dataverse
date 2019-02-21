package edu.harvard.iq.dataverse.bannersandmessages.messages;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(indexes = {@Index(columnList = "dataversetextmessage_id")})
public class DataverseLocalizedMessage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    private DataverseTextMessage dataverseTextMessage;

    public Long getId() {
        return id;
    }

    public String getLocale() {
        return locale;
    }

    public String getMessage() {
        return message;
    }

    public DataverseTextMessage getDataverseTextMessage() {
        return dataverseTextMessage;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDataverseTextMessage(DataverseTextMessage dataverseTextMessage) {
        this.dataverseTextMessage = dataverseTextMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataverseLocalizedMessage that = (DataverseLocalizedMessage) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
