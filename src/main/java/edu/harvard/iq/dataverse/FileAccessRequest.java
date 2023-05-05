package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "fileaccessrequests")
public class FileAccessRequest {
    @EmbeddedId
    private FileAccessRequestKey id;
    @ManyToOne
    @MapsId("dataFile")
    @JoinColumn(name = "datafile_id")
    private DataFile dataFile;
    @ManyToOne
    @MapsId("authenticatedUser")
    @JoinColumn(name = "authenticated_user_id")
    private AuthenticatedUser authenticatedUser;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "creation_time")
    private Date creationTime;

    public FileAccessRequestKey getId() {
        return id;
    }

    public void setId(FileAccessRequestKey id) {
        this.id = id;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    @Embeddable
    public static class FileAccessRequestKey implements Serializable {
        @Column(name = "datafile_id")
        private Long dataFile;
        @Column(name = "authenticated_user_id")
        private Long authenticatedUser;

        public Long getDataFile() {
            return dataFile;
        }

        public void setDataFile(Long dataFile) {
            this.dataFile = dataFile;
        }

        public Long getAuthenticatedUser() {
            return authenticatedUser;
        }

        public void setAuthenticatedUser(Long authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
        }
    }
}
