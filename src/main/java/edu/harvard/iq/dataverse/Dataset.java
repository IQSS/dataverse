/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import org.hibernate.validator.constraints.NotBlank;
//import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author skraffmiller
 */
@Entity
public class Dataset extends DvObjectContainer {

    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.MERGE)
    private List<DataFile> files = new ArrayList();

    private String protocol;
    private String authority;
    @NotBlank(message = "Please enter an identifier for your dataset.")
    private String identifier;
    @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("id DESC")
    private List<DatasetVersion> versions = new ArrayList();
    @OneToOne(mappedBy = "dataset", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private DatasetLock datasetLock;

    public Dataset() {
        //this.versions = new ArrayList();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(this);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        datasetVersion.setFileMetadatas(new ArrayList());
        datasetVersion.setVersionNumber(new Long(1));
        datasetVersion.setMinorVersionNumber(new Long(0));
        versions.add(datasetVersion);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPersistentURL() {
        if (this.getProtocol().equals("hdl")) {
            return getHandleURL();
        } else if (this.getProtocol().equals("doi")) {
            return getEZIdURL();
        } else {
            return "";
        }
    }

    private String getHandleURL() {
        return "http://hdl.handle.net/" + authority + "/" + getId();
    }

    private String getEZIdURL() {
        return "http://dx.doi.org/" + authority + "/" + getId();
    }

    public List<DataFile> getFiles() {
        return files;
    }

    public void setFiles(List<DataFile> files) {
        this.files = files;
    }

    public DatasetLock getDatasetLock() {
        return datasetLock;
    }

    public void setDatasetLock(DatasetLock datasetLock) {
        this.datasetLock = datasetLock;
    }

    public boolean isLocked() {
        if (datasetLock != null) {
            return true;
        }
        return false;
    }

    public DatasetVersion getLatestVersion() {
        return getVersions().get(0);
    }

    public DatasetVersion getLatestVersionForCopy() {
        for (DatasetVersion testDsv : getVersions()) {
            if (testDsv.isReleased() || testDsv.isArchived()) {
                return testDsv;
            }
        }
        return getVersions().get(0);
    }

    public List<DatasetVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<DatasetVersion> versions) {
        this.versions = versions;
    }

    private DatasetVersion createNewDatasetVersion(Template template) {
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(DatasetVersion.VersionState.DRAFT);
        DatasetVersion latestVersion = null;

        //if the latest version has values get them copied over
        if (!(template == null)) {
            if (!template.getDatasetFields().isEmpty()) {
                dsv.setDatasetFields(dsv.copyDatasetFields(template.getDatasetFields()));
            }
        } else {
            latestVersion = getLatestVersionForCopy();
            if (latestVersion.getDatasetFields() != null && !latestVersion.getDatasetFields().isEmpty()) {
                dsv.setDatasetFields(dsv.copyDatasetFields(latestVersion.getDatasetFields()));
            }
        }

        dsv.setFileMetadatas(new ArrayList());
        if (latestVersion != null) {
            for (FileMetadata fm : latestVersion.getFileMetadatas()) {
                FileMetadata newFm = new FileMetadata();
                newFm.setCategory(fm.getCategory());
                newFm.setDescription(fm.getDescription());
                newFm.setLabel(fm.getLabel());
                newFm.setDataFile(fm.getDataFile());
                newFm.setDatasetVersion(dsv);
                dsv.getFileMetadatas().add(newFm);
            }
        }

        // I'm adding the version to the list so it will be persisted when
        // the study object is persisted.
        if(template == null){
            getVersions().add(0, dsv);
        } else {
            this.setVersions(new ArrayList());
            getVersions().add(0, dsv);
        }

        dsv.setDataset(this);
        return dsv;
    }

    public DatasetVersion getEditVersion() {
        return getEditVersion(null);
    }

    public DatasetVersion getEditVersion(Template template) {
        DatasetVersion latestVersion = this.getLatestVersion();
        if (!latestVersion.isWorkingCopy() || template != null) {
            // if the latest version is released or archived, create a new version for editing
            return createNewDatasetVersion(template);
        } else {
            // else, edit existing working copy
            return latestVersion;
        }
    }

    public Date getMostRecentMajorVersionReleaseDate() {
        for (DatasetVersion version : this.getVersions()) {
            if (version.isReleased() && version.getMinorVersionNumber().equals(new Long(0))) {
                return version.getReleaseTime();
            }
        }
        return null;
    }

    public DatasetVersion getReleasedVersion() {
        for (DatasetVersion version : this.getVersions()) {
            if (version.isReleased()) {
                return version;
            }
        }
        return null;
    }

    public Path getFileSystemDirectory() {
        Path studyDir = null;

        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }

        if (this.getAuthority() != null && this.getIdentifier() != null) {
            studyDir = Paths.get(filesRootDirectory, this.getAuthority().toString(), this.getIdentifier().toString());
        }

        return studyDir;
    }

    public String getNextMajorVersionString() {
        for (DatasetVersion dv : this.getVersions()) {
            if (!dv.isWorkingCopy()) {
                return new Integer(dv.getVersionNumber().intValue() + 1).toString() + ".0";
            }
        }
        return "1.0";
    }

    public String getNextMinorVersionString() {
        for (DatasetVersion dv : this.getVersions()) {
            if (!dv.isWorkingCopy()) {
                return new Integer(dv.getVersionNumber().intValue()).toString() + "."
                        + new Integer(dv.getMinorVersionNumber().intValue() + 1).toString();
            }
        }
        return "1.0";
    }

    public Integer getVersionNumber() {
        for (DatasetVersion dv : this.getVersions()) {
            if (!dv.isWorkingCopy()) {
                return new Integer(dv.getVersionNumber().intValue());
            }
        }
        return new Integer(1);
    }

    public Integer getMinorVersionNumber() {
        for (DatasetVersion dv : this.getVersions()) {
            if (!dv.isWorkingCopy()) {
                return new Integer(dv.getMinorVersionNumber().intValue());
            }
        }
        return 0;
    }

    public String getCitation() {
        return getCitation(false, getLatestVersion());
    }

    public String getCitation(DatasetVersion version) {
        return version.getCitation();
    }

    public String getCitation(boolean isOnlineVersion, DatasetVersion version) {
        return version.getCitation(isOnlineVersion);
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Dataset)) {
            return false;
        }
        Dataset other = (Dataset) object;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    public String getGlobalId() {
        return protocol + ":" + authority + "/" + getIdentifier();
    }

}
