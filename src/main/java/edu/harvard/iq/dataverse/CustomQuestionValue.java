package edu.harvard.iq.dataverse;

import java.io.Serializable;
import jakarta.persistence.*;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author skraffmiller
 */
@Entity
public class CustomQuestionValue implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column( nullable = false )
    @NotBlank(message = "{custom.response}")
    private String valueString;
    
    private int displayOrder;

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public CustomQuestion getCustomQuestion() {
        return customQuestion;
    }

    public void setCustomQuestion(CustomQuestion customQuestion) {
        this.customQuestion = customQuestion;
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
    }
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private CustomQuestion customQuestion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CustomQuestionValue)) {
            return false;
        }
        CustomQuestionValue other = (CustomQuestionValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.vdc.CustomQuestionValue[ id=" + id + " ]";
    }
    
}

