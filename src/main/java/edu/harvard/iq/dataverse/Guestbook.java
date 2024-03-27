
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;

import edu.harvard.iq.dataverse.util.DateUtil;
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
    
    @NotBlank(message="{guestbook.name}")
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
        return DateUtil.formatDate(createTime);
    }
        
    public Guestbook copyGuestbook(Guestbook source, Dataverse dataverse) {
        Guestbook newGuestbook = new Guestbook();
        newGuestbook.setDataverse(dataverse);
        newGuestbook.setEmailRequired(source.isEmailRequired());
        newGuestbook.setNameRequired(source.isNameRequired());
        newGuestbook.setPositionRequired(source.isPositionRequired());
        newGuestbook.setInstitutionRequired(source.isInstitutionRequired());
        newGuestbook.setCustomQuestions(new ArrayList<>());
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
                    target.setCustomQuestionValues(new ArrayList<>());
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
    
    public List<String> getRequiredAccountInformation() {
        List<String> retList = new ArrayList<>();
        if (nameRequired) {
            retList.add(BundleUtil.getStringFromBundle("name"));
        }
        if (emailRequired) {
            retList.add(BundleUtil.getStringFromBundle("email"));
        }
        if (institutionRequired) {
            retList.add(BundleUtil.getStringFromBundle("institution"));
        }
        if (positionRequired) {
            retList.add(BundleUtil.getStringFromBundle("position"));
        }
        return retList;
    }
    
    public List<String> getOptionalAccountInformation(){
                List <String> retList = new ArrayList<>();
        if(!nameRequired){
           retList.add(BundleUtil.getStringFromBundle("name"));
        }
        if(!emailRequired){
            retList.add(BundleUtil.getStringFromBundle("email"));
        }
        if(!institutionRequired){
            retList.add(BundleUtil.getStringFromBundle("institution"));
        }
        if(!positionRequired){
            retList.add(BundleUtil.getStringFromBundle("position"));
        }
        return retList;
        
    }
    
    public List<String> getRequiredQuestionsList(){
        List <String> retList = new ArrayList<>();
                for (CustomQuestion cq : this.getCustomQuestions()){
                    if(cq.isRequired()){
                        retList.add(cq.getQuestionString());
                    }
                }
        return retList;
    }
    
    public List<String> getOptionalQuestionsList(){
        List <String> retList = new ArrayList<>();
                for (CustomQuestion cq : this.getCustomQuestions()){
                    if(!cq.isRequired()){
                        retList.add(cq.getQuestionString());
                    }
                }
        return retList;
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
    private Long usageCountDataverse;

    public Long getUsageCountDataverse() {
        return usageCountDataverse;
    }
    
    public void setUsageCountDataverse(Long usageCountDataverse) {
        this.usageCountDataverse = usageCountDataverse;
    }
    
    @Transient 
    private Long responseCount;

    public Long getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(Long responseCount) {
        this.responseCount = responseCount;
    }
    
    @Transient 
    private Long responseCountDataverse;

    public Long getResponseCountDataverse() {
        return responseCountDataverse;
    }

    public void setResponseCountDataverse(Long responseCountDataverse) {
        this.responseCountDataverse = responseCountDataverse;
    }
    
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Guestbook)) {
            return false;
        }
        Guestbook other = (Guestbook) object;
        return Objects.equals(getId(), other.getId());
    }
    
    
}
