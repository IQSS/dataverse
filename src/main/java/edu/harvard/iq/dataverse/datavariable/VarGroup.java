package edu.harvard.iq.dataverse.datavariable;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Index;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import java.util.HashSet;
import java.util.Set;


import edu.harvard.iq.dataverse.FileMetadata;

@Entity
@Table(indexes = {@Index(columnList="filemetadata_id")})
public class VarGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition="TEXT")
    private String label;

    @ManyToOne
    @JoinColumn(nullable=false)
    private FileMetadata fileMetadata;

    private Set<DataVariable> varsInGroup;


    public VarGroup () {
        varsInGroup = new HashSet<DataVariable>();
    }

    public VarGroup (FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
        varsInGroup = new HashSet<DataVariable>();
    }

    public VarGroup (FileMetadata fileMetadata, Set<DataVariable> varsInGroup ) {
        this.fileMetadata = fileMetadata;
        this.varsInGroup = varsInGroup;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setVarsInGroup(Set<DataVariable> varsInGroup) {
        this.varsInGroup = varsInGroup;
    }

    public Set<DataVariable> getVarsInGroup() {
        return varsInGroup;
    }

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }
}
