package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.MarkupChecker;
import edu.harvard.iq.dataverse.common.files.mime.PackageMimeType;
import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.config.EntityCustomizer;
import edu.harvard.iq.dataverse.persistence.config.ValidateURL;
import edu.harvard.iq.dataverse.persistence.config.annotations.CustomizeSelectionQuery;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.Customizer;
import org.jsoup.Jsoup;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetAuthor.displayOrderComparator;

/**
 * @author skraffmiller
 */
@Entity
@Table(indexes = {@Index(columnList = "dataset_id")},
        uniqueConstraints = @UniqueConstraint(columnNames = {"dataset_id","versionnumber","minorversionnumber"}))
@ValidateVersionNote(versionNote = "versionNote", versionState = "versionState")
@NamedNativeQueries({
        @NamedNativeQuery(name = "Dataset.findDataForSolrResults2", query =
                "SELECT t0.ID, t0.VERSIONSTATE, t1.ALIAS, t2.THUMBNAILFILE_ID, t2.USEGENERICTHUMBNAIL, t3.STORAGEIDENTIFIER " +
                "FROM DATASETVERSION t0 JOIN DATASET t2 ON t0.DATASET_ID = t2.ID JOIN DVOBJECT t3 ON t2.ID = t3.ID " +
                "JOIN DATAVERSE t1 ON t3.OWNER_ID = t1.ID WHERE t0.ID IN (?, ?)"),
        @NamedNativeQuery(name = "Dataset.findDataForSolrResults6", query =
                "SELECT t0.ID, t0.VERSIONSTATE, t1.ALIAS, t2.THUMBNAILFILE_ID, t2.USEGENERICTHUMBNAIL, t3.STORAGEIDENTIFIER " +
                "FROM DATASETVERSION t0 JOIN DATASET t2 ON t0.DATASET_ID = t2.ID JOIN DVOBJECT t3 ON t2.ID = t3.ID " +
                "JOIN DATAVERSE t1 ON t3.OWNER_ID = t1.ID WHERE t0.ID IN (?, ?, ?, ?, ?, ?)"),
        @NamedNativeQuery(name = "Dataset.findDataForSolrResults10", query =
                "SELECT t0.ID, t0.VERSIONSTATE, t1.ALIAS, t2.THUMBNAILFILE_ID, t2.USEGENERICTHUMBNAIL, t3.STORAGEIDENTIFIER " +
                "FROM DATASETVERSION t0 JOIN DATASET t2 ON t0.DATASET_ID = t2.ID JOIN DVOBJECT t3 ON t2.ID = t3.ID " +
                "JOIN DATAVERSE t1 ON t3.OWNER_ID = t1.ID WHERE t0.ID IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
})
@Customizer(EntityCustomizer.class)
public class DatasetVersion implements Serializable, JpaEntity<Long>, DatasetVersionIdentifier {

    private static final Logger logger = Logger.getLogger(DatasetVersion.class.getCanonicalName());

    // TODO: Determine the UI implications of various version states
    // IMPORTANT: If you add a new value to this enum, you will also have to modify the
    // StudyVersionsFragment.xhtml in order to display the correct value from a Resource Bundle
    public enum VersionState {
        DRAFT, RELEASED, ARCHIVED, DEACCESSIONED;
    }

    public static final int ARCHIVE_NOTE_MAX_LENGTH = 1000;
    public static final int VERSION_NOTE_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String UNF;

    @Version
    private Long version;

    private Long versionNumber;

    private Long minorVersionNumber;

    @Size(min = 0, max = VERSION_NOTE_MAX_LENGTH)
    @Column(length = VERSION_NOTE_MAX_LENGTH)
    private String versionNote;

    /*
     * @todo versionState should never be null so when we are ready, uncomment the `nullable = false` below.
     */
//    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VersionState versionState;

    @ManyToOne
    private Dataset dataset;

    @OneToMany(mappedBy = "datasetVersion", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST}, orphanRemoval = true)
    @OrderBy("label")
    // this is not our preferred ordering, which is with the AlphaNumericComparator,
    // but does allow the files to be grouped by category
    private List<FileMetadata> fileMetadatas = new ArrayList<>();

    @OneToMany(mappedBy = "datasetVersion", orphanRemoval = true,
            cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @CustomizeSelectionQuery(EntityCustomizer.Customizations.DATASET_FIELDS_WITH_PRIMARY_SOURCE)
    @OrderBy("displayOrder ASC")
    private List<DatasetField> datasetFields = new ArrayList<>();

    @OneToMany(mappedBy = "datasetVersion", orphanRemoval = true,
            cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @CustomizeSelectionQuery(EntityCustomizer.Customizations.DATASET_FIELDS_NO_PRIMARY_SOURCE)
    @OrderBy("displayOrder ASC")
    private List<DatasetField> datasetFieldsOptional = new ArrayList<>();

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createTime;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date lastUpdateTime;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date releaseTime;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date archiveTime;

    @Size(min = 0, max = ARCHIVE_NOTE_MAX_LENGTH)
    @Column(length = ARCHIVE_NOTE_MAX_LENGTH)
    @ValidateURL()
    private String archiveNote;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String archivalCopyLocation;

    private String deaccessionLink;

    @OneToMany(mappedBy = "datasetVersion", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetVersionUser> datasetVersionUsers;

    // Is this the right mapping and cascading for when the workflowcomments table
    // is being used for objects other than DatasetVersion?
    @OneToMany(mappedBy = "datasetVersion", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<WorkflowComment> workflowComments;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getUNF() {
        return UNF;
    }

    /**
     * This is JPA's optimistic locking mechanism, and has no semantic meaning in the DV object model.
     *
     * @return the object db version
     */
    public Long getVersion() {
        return version;
    }

    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }

    public List<DatasetField> getDatasetFieldsOptional() {
        return datasetFieldsOptional;
    }

    public Date getArchiveTime() {
        return archiveTime;
    }

    public String getArchiveNote() {
        return archiveNote;
    }

    public String getArchivalCopyLocation() {
        return archivalCopyLocation;
    }

    public String getDeaccessionLink() {
        return deaccessionLink;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public List<DatasetVersionUser> getDatasetVersionUsers() {
        return datasetVersionUsers;
    }

    public String getVersionNote() {
        return versionNote;
    }

    @Override
    public Long getVersionNumber() {
        return versionNumber;
    }

    @Override
    public Long getMinorVersionNumber() {
        return minorVersionNumber;
    }

    public VersionState getVersionState() {
        return versionState;
    }

    public Dataset getDataset() {
        return dataset;
    }

    @Override
    public Long getDatasetId() {
        return dataset.getId();
    }

    public List<WorkflowComment> getWorkflowComments() {
        return workflowComments;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setUNF(String UNF) {
        this.UNF = UNF;
    }

    public void setVersion(Long version) {
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    public void setArchiveTime(Date archiveTime) {
        this.archiveTime = archiveTime;
    }

    public void setArchiveNote(String note) {
        // @todo should this be using bean validation for trsting note length?
        if (note != null && note.length() > ARCHIVE_NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Error setting archiveNote: String length is greater than maximum (" + ARCHIVE_NOTE_MAX_LENGTH + ")."
                    + "  StudyVersion id=" + id + ", archiveNote=" + note);
        }
        this.archiveNote = note;
    }

    public void setArchivalCopyLocation(String location) {
        this.archivalCopyLocation = location;
    }

    public void setDeaccessionLink(String deaccessionLink) {
        this.deaccessionLink = deaccessionLink;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        if (createTime == null) {
            createTime = lastUpdateTime;
        }
        this.lastUpdateTime = lastUpdateTime;
    }

    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }

    public void setUserDatasets(List<DatasetVersionUser> datasetVersionUsers) {
        this.datasetVersionUsers = datasetVersionUsers;
    }

    public void setVersionNote(String note) {
        if (note != null && note.length() > VERSION_NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Error setting versionNote: String length is greater than maximum ("
                    + VERSION_NOTE_MAX_LENGTH + ")."
                    + "  StudyVersion id=" + id + ", versionNote=" + note);
        }
        this.versionNote = note;
    }

    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    public void setMinorVersionNumber(Long minorVersionNumber) {
        this.minorVersionNumber = minorVersionNumber;
    }

    public void setVersionState(VersionState versionState) {
        this.versionState = versionState;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    // -------------------- LOGIC --------------------

    public List<FileMetadata> getAllFilesMetadataSorted() {
        List<FileMetadata> result = newArrayList(fileMetadatas);
        result.sort(FileMetadata.compareByDisplayOrder);
        return result;
    }

    /**
     * Convenience comparator to compare dataset versions by their version number.
     * The draft version is considered the latest.
     */
    public static final Comparator<DatasetVersion> compareByVersion = (o1, o2) ->
            o1.isDraft()
                    ? (o2.isDraft() ? 0 : 1)
                    : (int) Math.signum(o1.getVersionNumber().equals(o2.getVersionNumber())
                        ? o1.getMinorVersionNumber() - o2.getMinorVersionNumber()
                        : o1.getVersionNumber() - o2.getVersionNumber());

    /**
     * Sets the dataset fields for this version. Also updates the fields to
     * have @{code this} as their dataset version.
     */
    public void setDatasetFields(List<DatasetField> datasetFields) {
        datasetFields.forEach(f -> f.setDatasetVersion(this));
        this.datasetFields = datasetFields;
    }

    /**
     * The only time a dataset can be in review is when it is in draft.
     *
     * @return if the dataset is being reviewed
     */
    public boolean isInReview() {
        return VersionState.DRAFT.equals(versionState) && dataset.isLockedFor(DatasetLock.Reason.InReview);
    }

    public VersionState getPriorVersionState() {
        if (isDeaccessioned()) {
            return null;
        }
        List<DatasetVersion> versions = dataset.getVersions();
        int currentVersionIndex = versions.indexOf(this);
        return currentVersionIndex > -1 && currentVersionIndex + 1 < versions.size()
                ? versions.get(currentVersionIndex + 1).getVersionState()
                : null;
    }

    public String getFriendlyVersionNumber() {
        return isDraft()
                ? "DRAFT"
                : versionNumber.toString() + "." + minorVersionNumber.toString();
    }

    public boolean isReleased() {
        return VersionState.RELEASED.equals(versionState);
    }

    public boolean isDraft() {
        return VersionState.DRAFT.equals(versionState);
    }

    public boolean isWorkingCopy() {
        return VersionState.DRAFT.equals(versionState);
    }

    public boolean isArchived() {
        return VersionState.ARCHIVED.equals(versionState);
    }

    public boolean isDeaccessioned() {
        return VersionState.DEACCESSIONED.equals(versionState);
    }

    public boolean isMinorUpdate() {
        if (dataset.getLatestVersion().isWorkingCopy()
                && dataset.getVersions().size() > 1
                && dataset.getVersions().get(1) != null
                && dataset.getVersions().get(1).isDeaccessioned()) {
            return false;
        }
        if (dataset.getReleasedVersion() != null) {
            if (fileMetadatas.size() != dataset.getReleasedVersion().getFileMetadatas().size()) {
                return false;
            } else {
                List<DataFile> current = fileMetadatas.stream()
                        .map(FileMetadata::getDataFile)
                        .collect(Collectors.toList());
                List<DataFile> previous = dataset.getReleasedVersion().getFileMetadatas().stream()
                        .map(FileMetadata::getDataFile)
                        .collect(Collectors.toList());
                previous.removeAll(current);
                return previous.isEmpty();
            }
        }
        return true;
    }

    public boolean isHasPackageFile() {
        if (fileMetadatas.size() != 1) {
            return false;
        }
        return PackageMimeType.DATAVERSE_PACKAGE.getMimeValue().equals(fileMetadatas.get(0).getDataFile().getContentType());
    }

    // XHTML
    public boolean isHasNonPackageFile() {
        // The presence of any non-package file means that HTTP Upload was used (no mixing allowed) so we just check the first file.
        return !fileMetadatas.isEmpty()
                && !PackageMimeType.DATAVERSE_PACKAGE.getMimeValue().equals(fileMetadatas.get(0).getDataFile().getContentType());
    }

    public DatasetVersion cloneDatasetVersion() {
        DatasetVersion cloned = new DatasetVersion();
        cloned.setVersionState(getPriorVersionState());
        cloned.setFileMetadatas(new ArrayList<>());
        cloned.setUNF(UNF);
        cloned.setDatasetFields(DatasetFieldUtil.copyDatasetFields(datasetFields));

        for (FileMetadata fm : fileMetadatas) {
            FileMetadata newFm = new FileMetadata();
            newFm.setCategories(fm.getCategories());
            newFm.setDescription(fm.getDescription());
            newFm.setLabel(fm.getLabel());
            newFm.setDirectoryLabel(fm.getDirectoryLabel());
            newFm.setDataFile(fm.getDataFile());
            newFm.setDatasetVersion(cloned);
            newFm.setProvFreeForm(fm.getProvFreeForm());
            FileTermsOfUse termsOfUse = fm.getTermsOfUse();
            FileTermsOfUse clonedTermsOfUse = termsOfUse.createCopy();
            newFm.setTermsOfUse(clonedTermsOfUse);
            cloned.getFileMetadatas().add(newFm);
        }
        cloned.setDataset(dataset);
        return cloned;
    }

    public String getTitle() {
        String result = StringUtils.EMPTY;
        for (DatasetField dsfv : datasetFields) {
            if (DatasetFieldConstant.title.equals(dsfv.getDatasetFieldType().getName())) {
                result = dsfv.getDisplayValue();
            }
        }
        return result;
    }

    public String getParsedTitle() {
        return Jsoup.parse(getTitle()).text();
    }

    public String getProductionDate() {
        String retVal = null;
        for (DatasetField dsfv : datasetFields) {
            if (DatasetFieldConstant.productionDate.equals(dsfv.getDatasetFieldType().getName())) {
                retVal = dsfv.getDisplayValue();
            }
        }
        return retVal;
    }

    /**
     * @return Strip out all A string with the description of the dataset that
     * has been passed through the stripAllTags method to remove all HTML tags.
     */
    public String getDescriptionPlainText() {
        for (DatasetField dsf : datasetFields) {
            if (!DatasetFieldConstant.description.equals(dsf.getDatasetFieldType().getName())) {
                continue;
            }
            String descriptionString = StringUtils.EMPTY;
            for (DatasetField subField : dsf.getDatasetFieldsChildren()) {
                if (DatasetFieldConstant.descriptionText.equals(subField.getDatasetFieldType().getName())
                        && !subField.isEmptyForDisplay()) {
                    descriptionString = subField.getValue();
                }
            }
            logger.log(Level.FINE, "pristine description: {0}", descriptionString);
            return MarkupChecker.stripAllTags(descriptionString);
        }
        return StringUtils.EMPTY;
    }

    public List<Map<String, DatasetField>> extractSubfields(String fieldName, List<String> subfields) {
        Set<String> namesLookup = new HashSet<>(subfields);
        namesLookup.add(fieldName); // sometimes the main field will be also needed
        return datasetFields.stream()
                .filter(f -> fieldName.equals(f.getDatasetFieldType().getName()))
                .map(f -> Stream.concat(f.getDatasetFieldsChildren().stream(), Stream.of(f))
                        .filter(s -> namesLookup.contains(s.getDatasetFieldType().getName()))
                        .collect(Collectors.toMap(s -> s.getDatasetFieldType().getName(), s -> s, (prev, next) -> next)))
                .filter(e -> e.size() > 1) // if there's only one element then we have only parent field with no subfields
                .collect(Collectors.toList());
    }

    public List<DatasetAuthor> getDatasetAuthors() {
        return extractSubfields(DatasetFieldConstant.author,
                Arrays.asList(DatasetFieldConstant.authorName, DatasetFieldConstant.authorAffiliation, DatasetFieldConstant.authorAffiliationIdentifier,
                        DatasetFieldConstant.authorIdType, DatasetFieldConstant.authorIdValue))
                .stream()
                .filter(e -> {
                    DatasetField name = e.get(DatasetFieldConstant.authorName);
                    return name != null && !name.isEmptyForDisplay();
                })
                .map(e -> {
                    DatasetAuthor author = new DatasetAuthor(e.get(DatasetFieldConstant.author).getDisplayOrder());
                    author.setName(e.get(DatasetFieldConstant.authorName));
                    author.setAffiliation(e.get(DatasetFieldConstant.authorAffiliation));
                    author.setAffiliationIdentifier(e.get(DatasetFieldConstant.authorAffiliationIdentifier));
                    DatasetField idType = e.get(DatasetFieldConstant.authorIdType);
                    author.setIdType(idType != null && !idType.getControlledVocabularyValues().isEmpty()
                            ? idType.getControlledVocabularyValues().get(0).getStrValue() : null);
                    author.setIdValue(mapIfNotNull(e.get(DatasetFieldConstant.authorIdValue), DatasetField::getDisplayValue));
                    return author;
                })
                .collect(Collectors.toList());
    }

    public List<String> extractFieldValues(String fieldName) {
        List<String> values = new ArrayList<>();
        for (DatasetField field : datasetFields) {
            if (fieldName.equals(field.getDatasetFieldType().getName())) {
                values.addAll(field.getValues());
            }
        }
        return values;
    }

    public List<String> getDatasetSubjects() {
        return extractFieldValues(DatasetFieldConstant.subject);
    }

    public List<String> getKeywords() {
        return getCompoundChildFieldValues(DatasetFieldConstant.keyword,
                Collections.singletonList(DatasetFieldConstant.keywordValue));
    }

    public List<DatasetRelPublication> getRelatedPublications() {
        return extractSubfields(DatasetFieldConstant.publication, Arrays.asList(
                    DatasetFieldConstant.publicationCitation, DatasetFieldConstant.publicationURL,
                    DatasetFieldConstant.publicationIDNumber, DatasetFieldConstant.publicationIDType))
                .stream()
                .map(m -> {
                    DatasetRelPublication publication = new DatasetRelPublication();
                    publication.setText(mapIfNotNull(m.get(DatasetFieldConstant.publicationCitation), DatasetField::getDisplayValue));
                    publication.setUrl(mapIfNotNull(m.get(DatasetFieldConstant.publicationURL), DatasetField::getValue));
                    publication.setIdNumber(mapIfNotNull(m.get(DatasetFieldConstant.publicationIDNumber), DatasetField::getValue));
                    publication.setIdType(mapIfNotNull(m.get(DatasetFieldConstant.publicationIDType), DatasetField::getValue));
                    return publication;
                })
                .collect(Collectors.toList());
    }

    public List<String> getCompoundChildFieldValues(String parentName, List<String> childNames) {
        return extractSubfields(parentName, childNames).stream()
                .flatMap(f -> childNames.stream().map(f::get))
                .filter(Objects::nonNull)
                .map(DatasetField::getValue)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    public Optional<DatasetField> getDatasetFieldByTypeName(String datasetFieldTypeName) {
        return streamDatasetFieldsByTypeName(datasetFieldTypeName)
                .findFirst();
    }

    public Stream<DatasetField> streamDatasetFieldsByTypeName(String datasetFieldTypeName) {
        return getFlatDatasetFields().stream()
                .filter(f -> datasetFieldTypeName.equals(f.getDatasetFieldType().getName()));
    }

    public String getDistributionDate() {
        for (DatasetField dsf : datasetFields) {
            if (DatasetFieldConstant.distributionDate.equals(dsf.getDatasetFieldType().getName())) {
                return dsf.getValue();
            }
        }
        return null;
    }

    // TODO: Consider renaming this method since it's also used for getting the "provider" for Schema.org JSON-LD.
    public String getRootDataverseNameForCitation() {
        Dataverse root = dataset.getOwner();
        while (root.getOwner() != null) {
            root = root.getOwner();
        }
        String rootDataverseName = root.getName();
        return StringUtils.isNotBlank(rootDataverseName)
                ? rootDataverseName : StringUtils.EMPTY;
    }

    public String getAuthorsStr() {
        return getAuthorsStr(true);
    }

    public String getAuthorsStr(boolean withAffiliation) {
        StringBuilder str = new StringBuilder();
        for (DatasetAuthor sa : getDatasetAuthors()) {
            if (sa.getName() == null) {
                break;
            }
            if (str.toString().trim().length() > 1) {
                str.append("; ");
            }
            str.append(sa.getName().getValue());
            if (withAffiliation
                    && sa.getAffiliation() != null
                    && StringUtils.isNotBlank(sa.getAffiliation().getValue())) {
                str.append(" (").append(sa.getAffiliation().getValue()).append(")");
            }
        }
        return str.toString();
    }

    // TODO: clean up init methods and get them to work, cascading all the way down.
    // right now, only work for one level of compound objects
    public List<DatasetField> initDatasetFields() {
        List<DatasetField> result = new ArrayList<>();
        Set<Long> usedTypeIds = new HashSet<>();
        if (datasetFields != null) {
            for (DatasetField dsf : datasetFields) {
                result.add(initDatasetField(dsf));
                usedTypeIds.add(dsf.getDatasetFieldType().getId());
            }
        }

        // Test to see that there are values for
        // all fields in this dataset via metadata blocks
        // only add if not added above
        for (MetadataBlock mdb : dataset.getOwner().getRootMetadataBlocks()) {
            for (DatasetFieldType dsfType : mdb.getDatasetFieldTypes()) {
                if (dsfType.isSubField()) {
                    continue;
                }
                if (!usedTypeIds.contains(dsfType.getId())) {
                    result.add(DatasetField.createNewEmptyDatasetField(dsfType, this));
                    usedTypeIds.add(dsfType.getId());
                }
            }
        }
        result.sort(DatasetField.DisplayOrder);
        return result;
    }

    public List<DatasetField> getFlatDatasetFields() {
        return DatasetFieldUtil.getFlatDatasetFields(getDatasetFields());
    }

    /**
    * Not prepending a "v" like "v1.1" or "v2.0" because while SemVerTag
    * was in http://semver.org/spec/v1.0.0.html but later removed in
    * http://semver.org/spec/v2.0.0.html
    *
    * See also to v or not to v · Issue #1 · mojombo/semver -
    * https://github.com/mojombo/semver/issues/1#issuecomment-2605236
    */
    public String getSemanticVersion() {
        if (isReleased()) {
            return versionNumber + "." + minorVersionNumber;
        } else if (isDraft()) {
            return VersionState.DRAFT.toString();
        } else if (isDeaccessioned()) {
            return versionNumber + "." + minorVersionNumber;
        } else {
            return versionNumber + "." + minorVersionNumber;
        }
    }

    public Set<ConstraintViolation<FileMetadata>> validateFileMetadata() {
        Set<ConstraintViolation<FileMetadata>> returnSet = new HashSet<>();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        List<FileMetadata> dsvfileMetadatas = fileMetadatas;
        if (dsvfileMetadatas != null) {
            for (FileMetadata fileMetadata : dsvfileMetadatas) {
                Set<ConstraintViolation<FileMetadata>> constraintViolations = validator.validate(fileMetadata);
                if (constraintViolations.size() > 0) {
                    // currently only support one message
                    ConstraintViolation<FileMetadata> violation = constraintViolations.iterator().next();

                    // @todo How can we expose this more detailed message containing the invalid value to the user?
                    String message = "Constraint violation found in FileMetadata. "
                            + violation.getMessage() + " "
                            + "The invalid value is \"" + violation.getInvalidValue().toString() + "\".";
                    logger.info(message);
                    returnSet.add(violation);
                    break; // currently only support one message, so we can break out of the loop after the first constraint violation
                }
            }
        }
        return returnSet;
    }

    /**
     * dataset publication date unpublished datasets will return an empty
     * string.
     *
     * @return String dataset publication date in ISO 8601 format (yyyy-MM-dd).
     */
    public String getPublicationDateAsString() {
        if (DatasetVersion.VersionState.DRAFT == versionState) {
            return StringUtils.EMPTY;
        }
        Date relDate = releaseTime;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        return fmt.format(relDate.getTime());
    }

    public void addFileMetadata(FileMetadata fileMetadata) {
        fileMetadata.setDisplayOrder(fileMetadataNextOrder());
        getFileMetadatas().add(fileMetadata);
    }

    // -------------------- PRIVATE --------------------

    private <T> T mapIfNotNull(DatasetField field, Function<DatasetField, T> mapper) {
        return field != null ? mapper.apply(field) : null;
    }

    private int fileMetadataNextOrder() {
        int maxDisplayOrder = -1;
        for (FileMetadata metadata : getFileMetadatas()) {
            if (metadata.getDisplayOrder() > maxDisplayOrder) {
                maxDisplayOrder = metadata.getDisplayOrder();
            }
        }
        return ++maxDisplayOrder;
    }

    // TODO: clean up init methods and get them to work, cascading all the way down.
    // right now, only work for one level of compound objects
    private DatasetField initDatasetField(DatasetField dsf) {
        if (dsf.getDatasetFieldType().isCompound()) {
            for (DatasetFieldType dsfType : dsf.getDatasetFieldType().getChildDatasetFieldTypes()) {
                boolean add = true;
                for (DatasetField subfield : dsf.getDatasetFieldsChildren()) {
                    if (dsfType.equals(subfield.getDatasetFieldType())) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    dsf.getDatasetFieldsChildren().add(DatasetField.createNewEmptyChildDatasetField(dsfType, dsf));
                }
            }
        }
        return dsf;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetVersion)) {
            return false;
        }
        DatasetVersion other = (DatasetVersion) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "[DatasetVersion id:" + id + "]";
    }
}
