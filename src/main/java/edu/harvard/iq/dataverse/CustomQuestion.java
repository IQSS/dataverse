package edu.harvard.iq.dataverse;
import java.io.Serializable;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {
        @Index(columnList = "guestbook_id")
})
public class CustomQuestion implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(nullable=false)
    private Guestbook guestbook;
    
    @OneToMany(mappedBy="customQuestion",cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST},orphanRemoval=true)
    private List<CustomQuestionResponse> customQuestionResponses;

    @OneToMany(mappedBy="customQuestion",cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST},orphanRemoval=true)
    @OrderBy("displayOrder")    
    private List<CustomQuestionValue> customQuestionValues;
    
    @Column( nullable = false )
    private String questionType;
    
    @NotBlank(message = "{custom.questiontext}")
    @Column( nullable = false )
    private String questionString;
    private boolean required;
    
    private boolean hidden;  //when a question is marked for removal, but it has data it is set to hidden

    private int displayOrder;

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Guestbook getGuestbook() {
        return guestbook;
    }

    public void setGuestbook(Guestbook guestbook) {
        this.guestbook = guestbook;
    }

    public String getQuestionString() {
        return questionString;
    }

    public void setQuestionString(String questionString) {
        this.questionString = questionString;
    }

    public List<CustomQuestionValue> getCustomQuestionValues() {
        return customQuestionValues;
    }
    
    public String getCustomQuestionValueString(){
        String retString = "";
        
        if (customQuestionValues != null && !this.customQuestionValues.isEmpty()){
            for (CustomQuestionValue customQuestionValue : this.customQuestionValues){
                if (!retString.isEmpty()){
                    retString += ", ";
                } else {
                    retString += "Answers:  ";
                }
                retString += customQuestionValue.getValueString();
            }
        }
        
        return retString;
    }

    public void setCustomQuestionValues(List<CustomQuestionValue> customQuestionValues) {
        this.customQuestionValues = customQuestionValues;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
    
    public List<CustomQuestionResponse> getCustomQuestionResponses() {
        return customQuestionResponses;
    }

    public void setCustomQuestionResponses(List<CustomQuestionResponse> customQuestionResponses) {
        this.customQuestionResponses = customQuestionResponses;
    }
    
    public void removeCustomQuestionValue(int index){
        customQuestionValues.remove(index);
    }
    
    public void addCustomQuestionValue(int index, CustomQuestionValue cq){
        customQuestionValues.add(index, cq);
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
        if (!(object instanceof CustomQuestion)) {
            return false;
        }
        CustomQuestion other = (CustomQuestion) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.vdc.CustomQuestion[ id=" + id + " ]";
    }
    
}

