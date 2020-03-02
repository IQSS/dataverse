package edu.harvard.iq.dataverse.persistence.dataset;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class DownloadDatasetLog {
    @Id
    @Column(name = "dataset_id")
    private Long datasetId;

    @Column(name = "downloadcount")
    private Integer downloadCount;

    public Long getDatasetId() {
        return datasetId;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }
}
