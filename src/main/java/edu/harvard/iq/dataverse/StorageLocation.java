package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class StorageLocation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", columnDefinition = "TEXT", nullable = false)
    private String name;

    @Column(name = "hostname", columnDefinition = "TEXT", nullable = false)
    private String hostname;

    @Column(name = "type", columnDefinition = "TEXT", nullable = false)
    private String type;

    @Column(name = "transferProtocols", columnDefinition = "TEXT", nullable = false)
    private String transferProtocols;

    @OneToMany(mappedBy = "storageLocation", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DvObjectStorageLocation> dvObjectStorageLocations;

    public List<DvObjectStorageLocation> getDvObjectStorageLocations() {
        return dvObjectStorageLocations;
    }

    public void setDvObjectStorageLocations(List<DvObjectStorageLocation> dvObjectStorageLocations) {
        this.dvObjectStorageLocations = dvObjectStorageLocations;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTransferProtocols() {
        return transferProtocols;
    }

    public void setTransferProtocols(String transferProtocols) {
        this.transferProtocols = transferProtocols;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof StorageLocation)) {
            return false;
        }
        StorageLocation other = (StorageLocation) object;
        return Objects.equals(getId(), other.getId());
    }

}
