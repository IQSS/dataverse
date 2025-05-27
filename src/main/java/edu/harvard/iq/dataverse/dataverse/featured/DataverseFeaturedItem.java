package edu.harvard.iq.dataverse.dataverse.featured;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

@NamedQueries({
        @NamedQuery(name = "DataverseFeaturedItem.deleteById",
                query = "DELETE FROM DataverseFeaturedItem item WHERE item.id=:id"),
        @NamedQuery(name = "DataverseFeaturedItem.findByDataverseOrderedByDisplayOrder",
                query = "SELECT item FROM DataverseFeaturedItem item WHERE item.dataverse = :dataverse ORDER BY item.displayOrder ASC")
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

    @NotBlank
    @Size(max = MAX_FEATURED_ITEM_CONTENT_SIZE)
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
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
        this.type = DataverseFeaturedItem.TYPES.CUSTOM.name().toLowerCase();
        this.dvobject = null;
        String dvType = (type != null) ? type.toLowerCase() : DataverseFeaturedItem.TYPES.CUSTOM.name().toLowerCase();
        if (DataverseFeaturedItem.TYPES.CUSTOM.name().equalsIgnoreCase(dvType)) {
            if (dvObject != null) {
                throw new IllegalArgumentException(BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.TypeAndDvObjectMismatch"));
            }
        } else if (DataverseFeaturedItem.VALID_TYPES.contains(dvType)) {
            if ((DataverseFeaturedItem.TYPES.DATAVERSE.name().equalsIgnoreCase(dvType) && dvObject instanceof Dataverse) ||
                    (DataverseFeaturedItem.TYPES.DATASET.name().equalsIgnoreCase(dvType) && dvObject instanceof Dataset) ||
                    (DataverseFeaturedItem.TYPES.DATAFILE.name().equalsIgnoreCase(dvType) && dvObject instanceof DataFile)) {
                this.type = dvType;
                this.dvobject = dvObject;
            } else {
                throw new IllegalArgumentException(BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.TypeAndDvObjectMismatch"));
            }
        } else {
            throw new IllegalArgumentException(BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.invalidType", List.of(String.join(",", DataverseFeaturedItem.VALID_TYPES))));
        }
    }
}
