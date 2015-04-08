
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
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
    @ManyToOne
    @JoinColumn(nullable=true)
    private Dataverse dataverse;
    
    @OneToMany(mappedBy="guestbook",cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST},orphanRemoval=true)
    @OrderBy("displayOrder")
    private List<CustomQuestion> customQuestions;
    
    @NotBlank(message="Enter a name for the guestbook")
    private String name;
    
    private boolean enabled;
    private boolean nameRequired;
    private boolean emailRequired;
    private boolean institutionRequired;   
    private boolean positionRequired; 
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column( nullable = false )
    private Date createTime;
    
    /* WE PROBABLY NEED HELP INFO TEXT...
     * public String guestbook() {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Edit Guestbook", " – Edit your dataset guestbook and click Save Changes. Asterisks indicate required fields."));
        return null;
    } */

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
    
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateDate() {
        return new SimpleDateFormat("MMMM d, yyyy").format(createTime);
    }
        
    public Guestbook copyGuestbook(Guestbook source, Dataverse dataverse) {
        Guestbook newGuestbook = new Guestbook();
        newGuestbook.setDataverse(dataverse);
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
                target.setDisplayOrder(sq.getDisplayOrder());
                target.setQuestionString(sq.getQuestionString());
                if(!sq.getCustomQuestionValues().isEmpty()){
                    target.setCustomQuestionValues(new ArrayList());
                    for (CustomQuestionValue scqv: sq.getCustomQuestionValues()){
                        CustomQuestionValue newVal = new CustomQuestionValue();
                        newVal.setValueString(scqv.getValueString());
                        newVal.setCustomQuestion(target);
                        target.getCustomQuestionValues().add(newVal);
                    }
                }
                newGuestbook.getCustomQuestions().add(target);
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
               retVal = "Required Custom Qustions<br/>&#160; &#8226; " + cq.getQuestionString(); 
            } else { 
               retVal += "<br/>&#160; &#8226; " + cq.getQuestionString();
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
               retVal = "Optional Custom Qustions<br/>&#160; &#8226; " + cq.getQuestionString(); 
            } else { 
               retVal += "<br/>&#160; &#8226; " + cq.getQuestionString();
            }
        }
        }
        return retVal;        
    }
    
    public String getRequiredAccountInformationString(){
        String retVal = "";
        if(nameRequired){
            retVal = "Required Account Information<br/>&#160; &#8226; Name";
        }
        if(emailRequired){
            if(retVal.isEmpty()){
               retVal = "Required Account Information<br/>&#160; &#8226; Email"; 
            } else { 
               retVal += "<br/>&#160; &#8226; Email";  
            }
        }
        if(institutionRequired){
            if(retVal.isEmpty()){
               retVal = "Required Account Information<br/>&#160; &#8226; Institution"; 
            } else { 
               retVal += "<br/>&#160; &#8226; Institution";  
            }
        }
        if(positionRequired){
            if(retVal.isEmpty()){
               retVal = "Required Account Information<br/>&#160; &#8226; Position"; 
            } else { 
               retVal += "<br/>&#160; &#8226; Position";  
            }
        }
        return retVal;
    }
    
    public String getOptionalAccountInformationString(){
        String retVal = "";
        if(!nameRequired){
            retVal = "Optional Account Information<br/>&#160; &#8226; Name";
        }
        if(!emailRequired){
            if(retVal.isEmpty()){
               retVal = "Optional Account Information<br/>&#160; &#8226; Email"; 
            } else { 
               retVal += "<br/>&#160; &#8226; Email";  
            }
        }
        if(!institutionRequired){
            if(retVal.isEmpty()){
               retVal = "Optional Account Information<br/>&#160; &#8226; Institution"; 
            } else { 
               retVal += "<br/>&#160; &#8226; Institution";  
            }
        }
        if(!positionRequired){
            if(retVal.isEmpty()){
               retVal = "Optional Account Information<br/>&#160; &#8226; Position"; 
            } else { 
               retVal += "<br/>&#160; &#8226; Position";  
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
    
    @Transient
    private Long usageCount;

    public Long getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }
    
    @Transient 
    private Long responseCount;

    public Long getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(Long responseCount) {
        this.responseCount = responseCount;
    }
    
    
}
