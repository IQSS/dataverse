package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@NamedQueries({
        @NamedQuery( name="Retention.findAll",
                query = "SELECT r FROM Retention r"),
        @NamedQuery( name="Retention.findById",
                query = "SELECT r FROM Retention r WHERE r.id=:id"),
        @NamedQuery( name="Retention.findByDateUnavailable",
                query = "SELECT r FROM Retention r WHERE r.dateUnavailable=:dateUnavailable"),
        @NamedQuery( name="Retention.deleteById",
                query = "DELETE FROM Retention r WHERE r.id=:id")
})
@Entity
public class Retention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateUnavailable;

    @Column(columnDefinition="TEXT")
    private String reason;

    @OneToMany(mappedBy="retention", cascade={ CascadeType.REMOVE, CascadeType.PERSIST})
    private List<DataFile> dataFiles;

    public Retention(){
        dateUnavailable = LocalDate.now().plusYears(1000); // Most likely valid with respect to configuration
    }

    public Retention(LocalDate dateUnavailable, String reason) {
        this.dateUnavailable = dateUnavailable;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateUnavailable() {
        return dateUnavailable;
    }

    public void setDateUnavailable(LocalDate dateUnavailable) {
        this.dateUnavailable = dateUnavailable;
    }

    public String getFormattedDateUnavailable() {
        return getDateUnavailable().format(DateTimeFormatter.ISO_LOCAL_DATE.withLocale(BundleUtil.getCurrentLocale()));
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<DataFile> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(List<DataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Retention retention = (Retention) o;
        return id.equals(retention.id) && dateUnavailable.equals(retention.dateUnavailable) && Objects.equals(reason, retention.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateUnavailable, reason);
    }

    @Override
    public String toString() {
        return "Retention{" +
                "id=" + id +
                ", dateUnavailable=" + dateUnavailable +
                ", reason='" + reason + '\'' +
                '}';
    }
}
