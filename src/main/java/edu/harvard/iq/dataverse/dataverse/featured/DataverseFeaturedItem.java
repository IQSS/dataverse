package edu.harvard.iq.dataverse.dataverse.featured;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.apache.commons.text.CaseUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NamedQueries({
        @NamedQuery(name = "DataverseFeaturedItem.deleteById",
                query = "DELETE FROM DataverseFeaturedItem item WHERE item.id=:id"),
        @NamedQuery(name = "DataverseFeaturedItem.findByDataverseOrderedByDisplayOrder",
                query = "SELECT item FROM DataverseFeaturedItem item WHERE item.dataverse = :dataverse ORDER BY item.displayOrder ASC"),
        @NamedQuery(name = "DataverseFeaturedItem.deleteByDvObjectId",
                query = "DELETE FROM DataverseFeaturedItem item WHERE item.dvobject IS NOT NULL AND item.dvobject.id = :id")
})
@Entity
@Table(indexes = @Index(columnList = "displayOrder"))
public class DataverseFeaturedItem {

    public static final List<String> VALID_TYPES = List.of("custom","dataverse","dataset","datafile");
    public enum TYPES {
        CUSTOM, DATAVERSE, DATASET, DATAFILE
    }
    public static final int MAX_FEATURED_ITEM_CONTENT_SIZE = 15000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Dataverse dataverse;

    @ManyToOne
    @JoinColumn(nullable = true)
    private DvObject dvobject;

    @Size(max = MAX_FEATURED_ITEM_CONTENT_SIZE)
    @Lob
    @Column(columnDefinition = "TEXT", nullable = true)
    private String content;

    @Min(0)
    @Column(nullable = false)
    private int displayOrder;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String type;

    private String imageFileName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getImageFileUrl() {
        if (id != null && imageFileName != null) {
            return SystemConfig.getDataverseSiteUrlStatic() + "/api/access/dataverseFeaturedItemImage/" + id;
        }
        return null;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public DvObject getDvObject() {
        return dvobject;
    }

    public void setDvObject(DvObject dvObject) {
        this.dvobject = dvObject;
    }

    public void setDvObject(String type, DvObject dvObject) throws IllegalArgumentException {
        DataverseFeaturedItem.TYPES dvType = getDvType(type);
        validateTypeAndDvObject(dvObject != null ? dvObject.getIdentifier() : null, dvObject, dvType);
        this.type = dvType.name().toLowerCase();
        this.dvobject = dvObject;
    }
    public static DataverseFeaturedItem.TYPES getDvType(String type) throws IllegalArgumentException {
        try {
            return StringUtil.isEmpty(type) ? DataverseFeaturedItem.TYPES.CUSTOM : DataverseFeaturedItem.TYPES.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            String validTypes = Arrays.stream(DataverseFeaturedItem.TYPES.values()).map(t -> t.name()).collect(Collectors.joining(", "));
            throw new IllegalArgumentException(BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.invalidType", List.of(validTypes)));
        }
    }
    public static void validateTypeAndDvObject(String dvIdtf, DvObject dvObject, DataverseFeaturedItem.TYPES dvType) throws IllegalArgumentException {
        if (dvObject != null) {
            if ((dvObject instanceof Dataverse && dvType != DataverseFeaturedItem.TYPES.DATAVERSE) ||
                    (dvObject instanceof Dataset && dvType != DataverseFeaturedItem.TYPES.DATASET) ||
                    (dvObject instanceof DataFile && dvType != DataverseFeaturedItem.TYPES.DATAFILE)) {
                throw new IllegalArgumentException(BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.typeAndDvObjectMismatch"));
            }
            if (dvObject instanceof DataFile) {
                DataFile df = (DataFile)dvObject;
                if (df.isRestricted()) {
                    throw new IllegalArgumentException(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.restricted"));
                }
                if (!df.isReleased()) {
                    throw new IllegalArgumentException(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notPublished", List.of("Dataset")));
                }
            } else if (!dvObject.isReleased()) {
                throw new IllegalArgumentException(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notPublished", List.of(CaseUtils.toCamelCase(dvType.name(), true))));
            }
        } else {
            if (dvType != DataverseFeaturedItem.TYPES.CUSTOM) {
                throw new IllegalArgumentException(BundleUtil.getStringFromBundle("find.dvo.error.dvObjectNotFound", List.of(dvIdtf == null ? "unknown" : dvIdtf)));
            }
        }
    }
}
