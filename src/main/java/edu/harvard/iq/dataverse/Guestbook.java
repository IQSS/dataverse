
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.List;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author skraffmiller
 */
@Entity
public class Guestbook implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
     /**
     * Holds value of the Dataverse
     */
    @OneToOne
    @JoinColumn(nullable=true)
    private Dataverse dataverse;
    
    @OneToMany(mappedBy="guestbook",cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST},orphanRemoval=true)
    private List<CustomQuestion> customQuestions;
    
    @NotBlank(message="Enter a name for the guestbook")
    private String name;
    
    private boolean enabled;
    private boolean nameRequired;
    private boolean emailRequired;
    private boolean institutionRequired;   
    private boolean positionRequired; 
    private Long usageCount;
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date createTime;

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

    public List<CustomQuestion> getCustomQuestions() {
        return customQuestions;
    }

    public void setCustomQuestions(List<CustomQuestion> customQuestions) {
        this.customQuestions = customQuestions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isNameRequired() {
        return nameRequired;
    }

    public void setNameRequired(boolean nameRequired) {
        this.nameRequired = nameRequired;
    }

    public boolean isEmailRequired() {
        return emailRequired;
    }

    public void setEmailRequired(boolean emailRequired) {
        this.emailRequired = emailRequired;
    }

    public boolean isInstitutionRequired() {
        return institutionRequired;
    }

    public void setInstitutionRequired(boolean institutionRequired) {
        this.institutionRequired = institutionRequired;
    }

    public boolean isPositionRequired() {
        return positionRequired;
    }

    public void setPositionRequired(boolean positionRequired) {
        this.positionRequired = positionRequired;
    }
    
    public Long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }    
    
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
        
    public Guestbook copyGuestbook(Guestbook source) {
        Guestbook newGuestbook = new Guestbook();
        newGuestbook.setEnabled(true);
        newGuestbook.setEmailRequired(source.isEmailRequired());
        newGuestbook.setNameRequired(source.isNameRequired());
        newGuestbook.setPositionRequired(source.isPositionRequired());
        newGuestbook.setInstitutionRequired(source.isInstitutionRequired());
        newGuestbook.setCustomQuestions(new ArrayList());
        if (!source.getCustomQuestions().isEmpty()) {
            for (CustomQuestion sq: source.getCustomQuestions()){
                CustomQuestion target = new CustomQuestion();
                target.setQuestionType(sq.getQuestionType());
                target.setGuestbook(newGuestbook);
                target.setHidden(sq.isHidden());
                target.setRequired(sq.isRequired());
                if(!sq.getCustomQuestionValues().isEmpty()){
                    for (CustomQuestionValue scqv: sq.getCustomQuestionValues()){
                        CustomQuestionValue newVal = new CustomQuestionValue();
                        newVal.setCustomQuestion(target);
                        newVal.setValueString(scqv.getValueString());
                    }
                }
            }          
        }

        return newGuestbook;
    }
    
    @Transient
    private boolean deletable;

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }
    
    public String getRequiredCustomQuestionsString(){
        String retVal = "";
        for (CustomQuestion cq : this.getCustomQuestions()){
            if(cq.isRequired()){
            if(retVal.isEmpty()){
               retVal = "Required Custom Qustion(s): " + cq.getQuestionString(); 
            } else { 
               retVal += "; " + cq.getQuestionString();
            }
        }
        }
        return retVal;        
    }
    
    public String getOptionalCustomQuestionsString(){
        String retVal = "";
        for (CustomQuestion cq : this.getCustomQuestions()){
            if(!cq.isRequired()){
            if(retVal.isEmpty()){
               retVal = "Optional Custom Qustion(s): " + cq.getQuestionString(); 
            } else { 
               retVal += "; " + cq.getQuestionString();
            }
        }
        }
        return retVal;        
    }
    
    public String getRequiredAccountInformationString(){
        String retVal = "";
        if(nameRequired){
            retVal = "Required Account Information: Name";
        }
        if(emailRequired){
            if(retVal.isEmpty()){
               retVal = "Required Account Information: EMail"; 
            } else { 
               retVal += ", EMail";  
            }
        }
        if(institutionRequired){
            if(retVal.isEmpty()){
               retVal = "Required Account Information: Institution"; 
            } else { 
               retVal += ", Institution";  
            }
        }
        if(positionRequired){
            if(retVal.isEmpty()){
               retVal = "Required Account Information: Position"; 
            } else { 
               retVal += ", Position";  
            }
        }
        return retVal;
    }
    
    public String getOptionalAccountInformationString(){
        String retVal = "";
        if(!nameRequired){
            retVal = "Optional Account Information: Name";
        }
        if(!emailRequired){
            if(retVal.isEmpty()){
               retVal = "Optional Account Information: EMail"; 
            } else { 
               retVal += ", EMail";  
            }
        }
        if(!institutionRequired){
            if(retVal.isEmpty()){
               retVal = "Optional Account Information: Institution"; 
            } else { 
               retVal += ", Institution";  
            }
        }
        if(!positionRequired){
            if(retVal.isEmpty()){
               retVal = "Optional Account Information: Position"; 
            } else { 
               retVal += ", Position";  
            }
        }
        return retVal;
    }
    
    public void removeCustomQuestion(int index){
        customQuestions.remove(index);
    }
    
    public void addCustomQuestion(int index, CustomQuestion cq){
        customQuestions.add(index, cq);
    }
    
}
