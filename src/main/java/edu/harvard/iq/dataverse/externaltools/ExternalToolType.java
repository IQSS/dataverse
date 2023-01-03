package edu.harvard.iq.dataverse.externaltools;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(indexes = {
    @Index(columnList = "externaltool_id")})
public class ExternalToolType implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExternalTool.Type type;

    /**
     * TODO: Should this be something other than CascadeType.PERSIST? Right now
     * if you delete the last or only ExternalToolType for an ExternalTool, the
     * ExternalTool is not deleted from the database.
     */
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(nullable = false)
    private ExternalTool externalTool;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExternalTool.Type getType() {
        return type;
    }

    public void setType(ExternalTool.Type type) {
        this.type = type;
    }

    public ExternalTool getExternalTool() {
        return externalTool;
    }

    public void setExternalTool(ExternalTool externalTool) {
        this.externalTool = externalTool;
    }

}
