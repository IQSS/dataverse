package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Entity
@Table(indexes = {@Index(columnList = "dataverse_id"), @Index(columnList = "displayOrder")})
public class DataverseFeaturedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dataverse_id", nullable = false)
    private Dataverse dataverse;

    @NotBlank
    @Size(max = 15000)
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
        return SystemConfig.getDataverseSiteUrlStatic() + "/api/access/dataverseFeatureItemImage/" + id;
    }
}
