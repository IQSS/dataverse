package edu.harvard.iq.dataverse.dataverse.featured;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@NamedQueries({
        @NamedQuery(name = "DataverseFeaturedItem.deleteById",
                query = "DELETE FROM DataverseFeaturedItem item WHERE item.id=:id"),
        @NamedQuery(name = "DataverseFeaturedItem.findByDataverseOrderedByDisplayOrder",
                query = "SELECT item FROM DataverseFeaturedItem item WHERE item.dataverse = :dataverse ORDER BY item.displayOrder ASC")
})
@Entity
@Table(indexes = @Index(columnList = "displayOrder"))
public class DataverseFeaturedItem {

    public static final int MAX_FEATURED_ITEM_CONTENT_SIZE = 15000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Dataverse dataverse;

    @NotBlank
    @Size(max = MAX_FEATURED_ITEM_CONTENT_SIZE)
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Min(0)
    @Column(nullable = false)
    private int displayOrder;

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
}
