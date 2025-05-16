package edu.harvard.iq.dataverse.dataverse.featured;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
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

    /*
    Make sure the object and type match.
     */
    public static DvObjectFeaturedItem sanitizeDvObject(String type, DvObject dvObject) {
        String dvType = (type != null) ? type.toLowerCase() : "custom";
        if (DataverseFeaturedItem.VALID_TYPES.contains(dvType)) {
            if (("dataverse".equals(dvType) && dvObject instanceof Dataverse) ||
                    ("dataset".equals(dvType) && dvObject instanceof Dataset) ||
                    ("datafile".equals(dvType) && dvObject instanceof DataFile)) {
                return new DvObjectFeaturedItem(dvType, dvObject);
            }
        }
        return new DvObjectFeaturedItem("custom", null);
    }
    public static class DvObjectFeaturedItem {
        public String type;
        public DvObject dvObject;
        DvObjectFeaturedItem (String type, DvObject dvObject) {
            this.type = type;
            this.dvObject = dvObject;
        }
    }
}
