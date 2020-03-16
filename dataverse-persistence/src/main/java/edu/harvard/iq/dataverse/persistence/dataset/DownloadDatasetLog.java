package edu.harvard.iq.dataverse.persistence.dataset;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
public class DownloadDatasetLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "downloaddatasetlog_id_seq_gen")
    @SequenceGenerator(name = "downloaddatasetlog_id_seq_gen", sequenceName = "downloaddatasetlog_id_seq", allocationSize = 50)
    @Column(name = "id")
    private Long id;

    @Column(name = "dataset_id")
    private Long datasetId;

    @Column(name = "downloaddate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date downloadDate;


    public Long getId() {
        return id;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public Date getDownloadDate() {
        return downloadDate;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public void setDownloadDate(Date downloadDate) {
        this.downloadDate = downloadDate;
    }
}
