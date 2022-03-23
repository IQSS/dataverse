package edu.harvard.iq.dataverse.locality;

import java.io.Serializable;
import java.util.Objects;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class StorageSite implements Serializable {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String HOSTNAME = "hostname";
    public static final String PRIMARY_STORAGE = "primaryStorage";
    public static final String TRANSFER_PROTOCOLS = "transferProtocols";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FIXME: Why is nullable=false having no effect?
    @Column(name = "name", columnDefinition = "TEXT", nullable = false)
    private String name;

    /**
     * Sites around the world to which data has been replicated using RSAL
     * (Repository Storage Abstraction Layer). Formerly, the :ReplicationSites
     * database setting.
     *
     * TODO: Think about how this is a duplication of the following JVM options:
     *
     * - dataverse.fqdn
     *
     * - dataverse.siteUrl
     */
    // FIXME: Why is nullable=false having no effect?
    @Column(name = "hostname", columnDefinition = "TEXT", nullable = false)
    private String hostname;

    /**
     * TODO: Consider adding a constraint to only allow one row to be true. The
     * following was suggested...
     *
     * create unique index on my_table (actual)
     *
     * where actual = true;
     *
     * ... at
     * https://stackoverflow.com/questions/28166915/postgresql-constraint-only-one-row-can-have-flag-set/28167225#28167225
     */
    @Column(nullable = false)
    private boolean primaryStorage;

    /**
     * For example, "rsync,posix,globus". A comma-separated list. These
     * protocols are what we might advertise to end users who want to download
     * the data from us. In the future, we can imagine adding S3.
     */
    // FIXME: Why is nullable=false having no effect?
    @Column(name = "transferProtocols", columnDefinition = "TEXT", nullable = false)
    private String transferProtocols;

//    @OneToMany(mappedBy = "storageSite", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
//    private List<DvObjectStorageLocation> dvObjectStorageLocations;
//    public List<DvObjectStorageLocation> getDvObjectStorageLocations() {
//        return dvObjectStorageLocations;
//    }
//
//    public void setDvObjectStorageLocations(List<DvObjectStorageLocation> dvObjectStorageLocations) {
//        this.dvObjectStorageLocations = dvObjectStorageLocations;
//    }
//    
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

    public boolean isPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(boolean primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public String getTransferProtocols() {
        return transferProtocols;
    }

    public void setTransferProtocols(String transferProtocols) {
        this.transferProtocols = transferProtocols;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof StorageSite)) {
            return false;
        }
        StorageSite other = (StorageSite) object;
        return Objects.equals(getId(), other.getId());
    }

    public JsonObjectBuilder toJsonObjectBuilder() {
        return Json.createObjectBuilder()
                .add(ID, id)
                .add(HOSTNAME, hostname)
                .add(NAME, name)
                .add(PRIMARY_STORAGE, primaryStorage)
                .add(TRANSFER_PROTOCOLS, transferProtocols);
    }
}
