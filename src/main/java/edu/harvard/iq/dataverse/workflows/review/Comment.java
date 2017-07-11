package edu.harvard.iq.dataverse.workflows.review;

import edu.harvard.iq.dataverse.DatasetVersion;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

@Entity
public class Comment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @JoinColumn(nullable = false)
    private DatasetVersion datasetVersion;

    public Comment(String text, DatasetVersion datasetVersion) {
        this.text = text;
        this.datasetVersion = datasetVersion;
    }

    /**
     * This default constructor is only here to prevent this error at
     * deployment:
     *
     * Exception Description: The instance creation method
     * [...Comments.<Default Constructor>], with no parameters, does not exist,
     * or is not accessible
     *
     * Don't use it.
     */
    @Deprecated
    public Comment() {
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

}
