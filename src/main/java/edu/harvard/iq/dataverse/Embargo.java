package edu.harvard.iq.dataverse;


import javax.persistence.*;

import edu.harvard.iq.dataverse.util.DateUtil;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author mderuijter
 */
@NamedQueries({
        @NamedQuery( name="Embargo.findAll",
                query = "SELECT e FROM Embargo e"),
        @NamedQuery( name="Embargo.findById",
                query = "SELECT e FROM Embargo e WHERE e.id=:id"),
        @NamedQuery( name="Embargo.findByDateAvailable",
                query = "SELECT e FROM Embargo e WHERE e.dateAvailable=:dateAvailable"),
        @NamedQuery( name="Embargo.deleteById",
                query = "DELETE FROM Embargo e WHERE e.id=:id")
})
@Entity
public class Embargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateAvailable;

    @Column(columnDefinition="TEXT")
    private String reason;

    @OneToMany(mappedBy="embargo", cascade={ CascadeType.REMOVE, CascadeType.PERSIST})
    private List<DataFile> dataFiles;

    public Embargo(){
        dateAvailable = LocalDate.now().plusDays(1);
    }

    public Embargo(LocalDate dateAvailable, String reason) {
        this.dateAvailable = dateAvailable;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateAvailable() {
        return dateAvailable;
    }

    public void setDateAvailable(LocalDate dateAvailable) {
        this.dateAvailable = dateAvailable;
    }

    public String getFormattedDateAvailable() {
        return DateUtil.formatDate(Timestamp.valueOf(getDateAvailable().atStartOfDay()));
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
        Embargo embargo = (Embargo) o;
        return id.equals(embargo.id) && dateAvailable.equals(embargo.dateAvailable) && Objects.equals(reason, embargo.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateAvailable, reason);
    }

    @Override
    public String toString() {
        return "Embargo{" +
                "id=" + id +
                ", dateAvailable=" + dateAvailable +
                ", reason='" + reason + '\'' +
                '}';
    }
}
