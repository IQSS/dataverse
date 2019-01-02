package edu.harvard.iq.dataverse.dataverse.messages;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(indexes = {@Index(columnList = "dataversetextmessage_id")})
public class DataverseLocalizedMessage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}
