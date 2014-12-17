/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseGuestbookCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDateverseTemplateCommand;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 *
 * @author skraffmiller
 */
@ViewScoped
@Named("GuestbookPage")
public class GuestbookPage implements java.io.Serializable {

    @EJB
    GuestbookServiceBean guestbookService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    EjbDataverseEngine commandEngine;
    
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    
    @Inject
    DataverseSession session;

    public enum EditMode {

        CREATE, METADATA
    };

    private Guestbook guestbook;
    private Dataverse dataverse;
    private EditMode editMode;
    private Long ownerId;
    private Long guestbookId;

    public Long getGuestbookId() {
        return guestbookId;
    }

    public void setGuestbookId(Long guestbookId) {
        this.guestbookId = guestbookId;
    }

    public Guestbook getGuestbook() {
        return guestbook;
    }

    public void setGuestbook(Guestbook guestbook) {
        this.guestbook = guestbook;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void init() {
        if (guestbookId != null) { // edit or view existing for a template  
            dataverse = dataverseService.find(ownerId);
            for (Guestbook dvGb : dataverse.getGuestbooks()) {
                if (dvGb.getId().longValue() == guestbookId) {
                    guestbook = dvGb;
                }
            }
            guestbook.setDataverse(dataverse);
        } else if (ownerId != null) {
            // create mode for a new template
            dataverse = dataverseService.find(ownerId);
            editMode = GuestbookPage.EditMode.CREATE;
            guestbook = new Guestbook();
            guestbook.setDataverse(dataverse);
        } else {
            throw new RuntimeException("On Guestbook page without id or ownerid."); // improve error handling
        }
    }

    public String removeCustomQuestion(Long index){
        guestbook.removeCustomQuestion(index.intValue());
        return "";
    }
    
    public List<GuestbookResponse> getGuestbookResponses(){
        return null;
    }
    
    
    public String addCustomQuestion(){
        CustomQuestion toAdd = new CustomQuestion();
        toAdd.setQuestionType("text");
        toAdd.setCustomQuestionValues(new ArrayList());
        toAdd.setGuestbook(guestbook);
        int index = guestbook.getCustomQuestions().size();
        guestbook.addCustomQuestion(index, toAdd);
        return "";
    }
    
    public String addCustomQuestionValue(CustomQuestion cq, int index){
        CustomQuestionValue toAdd = new CustomQuestionValue();
        toAdd.setValueString("");
        toAdd.setCustomQuestion(cq);
        cq.addCustomQuestionValue(index, toAdd);
        return "";       
    }
    
    public String removeCustomQuestionValue(CustomQuestion cq, Long index){
        cq.removeCustomQuestionValue(index.intValue());
        return "";
    }
    
    public void toggleQuestionType(CustomQuestion questionIn) {
        if (questionIn.getCustomQuestionValues() != null && questionIn.getCustomQuestionValues().isEmpty() 
                && questionIn.getQuestionType().equals("options")){
            questionIn.setCustomQuestionValues(new ArrayList());
            CustomQuestionValue addCQV = new CustomQuestionValue();
            addCQV.setCustomQuestion(questionIn);
            questionIn.getCustomQuestionValues().add(addCQV);
        } 
    }
    


    public void edit(GuestbookPage.EditMode editMode) {
        this.editMode = editMode;
    }

    public String save() {
        
        if (!(guestbook.getCustomQuestions() == null)) {
            for (CustomQuestion cq : guestbook.getCustomQuestions()) {
                if (cq.getQuestionType().equals("text")) {
                    cq.setCustomQuestionValues(null);
                }
            }
        }
      
        Command<Dataverse> cmd;
        try {
            if (editMode == EditMode.CREATE) {
                guestbook.setCreateTime(new Timestamp(new Date().getTime()));
                guestbook.setUsageCount(new Long(0));
                dataverse.getGuestbooks().add(guestbook);
                cmd = new UpdateDataverseCommand(dataverse, null, null, session.getUser(), null);
                commandEngine.submit(cmd);
            } else {
                cmd = new UpdateDataverseGuestbookCommand(dataverse, guestbook, session.getUser());
                commandEngine.submit(cmd);
            }

        } catch (EJBException ex) {
            StringBuilder error = new StringBuilder();
            error.append(ex + " ");
            error.append(ex.getMessage() + " ");
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                error.append(cause + " ");
                error.append(cause.getMessage() + " ");
            }
            //
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Guestbook Save Failed", " - " + error.toString()));
            System.out.print("dataverse " + dataverse.getName());
            System.out.print("Ejb exception");
            System.out.print(error.toString());
            return null;
        } catch (CommandException ex) {
            System.out.print("command exception");
            System.out.print(ex.toString());
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Guestbook Save Failed", " - " + ex.toString()));
            //logger.severe(ex.getMessage());
        }
        editMode = null;
        return "/manage-guestbooks.xhtml?dataverseId=" + dataverse.getId() + "&faces-redirect=true";
    }

    public void cancel() {
        editMode = null;
    }

}

