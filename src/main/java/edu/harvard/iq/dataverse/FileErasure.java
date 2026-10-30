package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.persistence.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

/**
 * Represents the erasure of a physical file from storage while retaining the
 * DataFile database record with audit metadata about who erased it, when, and why.
 * This supports GDPR "right to be forgotten" requests and similar compliance scenarios.
 */
@NamedQueries({
        @NamedQuery(name = "FileErasure.findAll",
                query = "SELECT fe FROM FileErasure fe"),
        @NamedQuery(name = "FileErasure.findById",
                query = "SELECT fe FROM FileErasure fe WHERE fe.id=:id"),
        @NamedQuery(name = "FileErasure.findByDataFileId",
                query = "SELECT fe FROM FileErasure fe WHERE fe.dataFile.id=:dataFileId")
})
@Entity
public class FileErasure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "datafile_id", unique = true)
    private DataFile dataFile;

    @ManyToOne
    @JoinColumn(name = "erased_by_id")
    private AuthenticatedUser erasedBy;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date erasedOn;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private boolean manuallyDeleted = false;

    public FileErasure() {
        this.erasedOn = new Date();
    }

    public FileErasure(DataFile dataFile, AuthenticatedUser erasedBy, String reason, boolean manuallyDeleted) {
        this.dataFile = dataFile;
        this.erasedBy = erasedBy;
        this.erasedOn = new Date();
        this.reason = reason;
        this.manuallyDeleted = manuallyDeleted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public AuthenticatedUser getErasedBy() {
        return erasedBy;
    }

    public void setErasedBy(AuthenticatedUser erasedBy) {
        this.erasedBy = erasedBy;
    }

    public Date getErasedOn() {
        return erasedOn;
    }

    public void setErasedOn(Date erasedOn) {
        this.erasedOn = erasedOn;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isManuallyDeleted() {
        return manuallyDeleted;
    }

    public void setManuallyDeleted(boolean manuallyDeleted) {
        this.manuallyDeleted = manuallyDeleted;
    }

    public String getFormattedErasedOn() {
        return java.sql.Timestamp.valueOf(erasedOn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a").withLocale(BundleUtil.getCurrentLocale()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileErasure that = (FileErasure) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FileErasure{" +
                "id=" + id +
                ", dataFile=" + (dataFile != null ? dataFile.getId() : "null") +
                ", erasedBy=" + (erasedBy != null ? erasedBy.getId() : "null") +
                ", erasedOn=" + erasedOn +
                ", reason='" + reason + '\'' +
                ", manuallyDeleted=" + manuallyDeleted +
                '}';
    }
}
