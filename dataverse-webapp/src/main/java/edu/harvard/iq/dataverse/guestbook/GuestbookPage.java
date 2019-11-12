/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.CustomQuestion;
import edu.harvard.iq.dataverse.persistence.guestbook.CustomQuestionValue;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.faces.application.FacesMessage.SEVERITY_ERROR;

/**
 * @author skraffmiller
 */
@ViewScoped
@Named("GuestbookPage")
public class GuestbookPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(GuestbookPage.class.getCanonicalName());

    private GuestbookServiceBean guestbookHelperService;
    private DataverseServiceBean dataverseService;
    private PermissionsWrapper permissionsWrapper;
    private GuestbookService guestbookService;

    public enum EditMode {

        CREATE, METADATA, CLONE
    }

    private Guestbook guestbook;
    private Dataverse dataverse;
    private EditMode editMode;
    private Long ownerId;
    private Long guestbookId;
    private Long sourceId;

    private UIInput guestbookName;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public GuestbookPage() {
    }

    @Inject
    public GuestbookPage(GuestbookServiceBean guestbookHelperService, DataverseServiceBean dataverseService,
                         PermissionsWrapper permissionsWrapper, GuestbookService guestbookService) {
        this.guestbookHelperService = guestbookHelperService;
        this.dataverseService = dataverseService;
        this.permissionsWrapper = permissionsWrapper;
        this.guestbookService = guestbookService;
    }

    // -------------------- GETTERS --------------------

    public Long getSourceId() {
        return sourceId;
    }

    public Long getGuestbookId() {
        return guestbookId;
    }

    public Guestbook getGuestbook() {
        return guestbook;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public UIInput getGuestbookName() {
        return guestbookName;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        dataverse = dataverseService.find(ownerId);
        if (dataverse == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }

        if (guestbookId != null) { // edit or view existing for a template  
            for (Guestbook dvGb : dataverse.getGuestbooks()) {
                if (dvGb.getId().longValue() == guestbookId) {
                    guestbook = dvGb;
                }
            }
            editMode = EditMode.METADATA;
        } else if (ownerId != null && sourceId == null) {
            // create mode for a new template
            guestbook = new Guestbook();
            guestbook.setDataverse(dataverse);
            editMode = EditMode.CREATE;
        } else if (ownerId != null && sourceId != null) {
            // Clone mode for a new template from source
            editMode = EditMode.CLONE;
            Guestbook sourceGB = guestbookHelperService.find(sourceId);
            guestbook = sourceGB.copyGuestbook(sourceGB, dataverse);
            String name = BundleUtil.getStringFromBundle("page.copy") + " " + sourceGB.getName();
            guestbook.setName(name);
            guestbook.setUsageCount(0L);
            guestbook.setCreateTime(new Timestamp(new Date().getTime()));

        } else {
            throw new RuntimeException("On Guestook page without id or ownerid."); // improve error handling
        }

        initCustomQuestionsForView();

        return null;

    }

    public void addCustomQuestion(Integer indexIn) {
        CustomQuestion toAdd = new CustomQuestion();
        toAdd.setQuestionType("text");
        toAdd.setCustomQuestionValues(new ArrayList<CustomQuestionValue>());
        toAdd.setGuestbook(guestbook);

        addCustomQuestionValue(toAdd, 0);

        guestbook.addCustomQuestion(indexIn, toAdd);
    }

    public String removeCustomQuestion(Long index) {
        guestbook.removeCustomQuestion(index.intValue());
        return "";
    }


    public void addCustomQuestionValue(CustomQuestion cq, int index) {
        CustomQuestionValue toAdd = new CustomQuestionValue();
        toAdd.setValueString("");
        toAdd.setCustomQuestion(cq);
        cq.addCustomQuestionValue(index, toAdd);
    }

    public void removeCustomQuestionValue(CustomQuestion cq, Long index) {
        cq.removeCustomQuestionValue(index.intValue());
    }


    public String save() {

        if (StringUtils.isEmpty(guestbook.getName())) {
            FacesContext.getCurrentInstance().validationFailed();
            FacesContext.getCurrentInstance().addMessage(guestbookName.getClientId(),
                                                         new FacesMessage(SEVERITY_ERROR, StringUtils.EMPTY, BundleUtil.getStringFromBundle("guestbook.name.empty")));
            return StringUtils.EMPTY;
        }

        if (!(guestbook.getCustomQuestions() == null)) {
            for (CustomQuestion cq : guestbook.getCustomQuestions()) {
                if (cq.getQuestionType().equals("text")) {
                    cq.setCustomQuestionValues(null);
                }
            }

            Iterator<CustomQuestion> cqIt = guestbook.getCustomQuestions().iterator();
            while (cqIt.hasNext()) {
                CustomQuestion cq = cqIt.next();
                if (StringUtils.isBlank(cq.getQuestionString())) {
                    cqIt.remove();
                }
            }

            for (CustomQuestion cq : guestbook.getCustomQuestions()) {
                if (cq != null && cq.getQuestionType().equals("options")) {
                    Iterator<CustomQuestionValue> cqvIt = cq.getCustomQuestionValues().iterator();
                    while (cqvIt.hasNext()) {
                        CustomQuestionValue cqv = cqvIt.next();
                        if (StringUtils.isBlank(cqv.getValueString())) {
                            cqvIt.remove();
                        }
                    }
                }
            }

            for (CustomQuestion cq : guestbook.getCustomQuestions()) {
                if (cq != null && cq.getQuestionType().equals("options")) {
                    if (cq.getCustomQuestionValues() == null || cq.getCustomQuestionValues().isEmpty()) {
                        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("guestbook.save.fail"), BundleUtil.getStringFromBundle("guestbook.option.msg")));
                        return null;
                    }
                    if (cq.getCustomQuestionValues().size() == 1) {
                        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("guestbook.save.fail"), BundleUtil.getStringFromBundle("guestbook.option.msg")));
                        return null;
                    }
                }
            }
            int i = 0;
            for (CustomQuestion cq : guestbook.getCustomQuestions()) {
                int j = 0;
                cq.setDisplayOrder(i);
                if (cq.getCustomQuestionValues() != null && !cq.getCustomQuestionValues().isEmpty()) {
                    for (CustomQuestionValue cqv : cq.getCustomQuestionValues()) {
                        cqv.setDisplayOrder(j);
                        j++;
                    }
                }
                i++;
            }
        }

        if (editMode == EditMode.CREATE || editMode == EditMode.CLONE) {
            Try<Dataverse> guestbookTry = Try.of(() -> guestbookService.saveGuestbook(guestbook))
                    .onSuccess(dv -> JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("guestbook.create")))
                    .onFailure(this::handleErrorMessages);

            if(guestbookTry.isFailure() && guestbookTry.getCause() instanceof EJBException) {
                return "";
            }
        } else {
            Try.of(() -> guestbookService.editGuestbook(guestbook))
                    .onSuccess(dv -> JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("guestbook.save")))
                    .onFailure(this::handleErrorMessages);
        }

        editMode = null;

        return "/manage-guestbooks.xhtml?dataverseId=" + dataverse.getId() + "&faces-redirect=true";
    }

    public void cancel() {
        editMode = null;
    }

    // -------------------- PRIVATE --------------------
    private void handleErrorMessages(Throwable throwable) {
            logger.log(Level.SEVERE,"Guestbook Page Exception. Dataverse: " + dataverse.getName());
            logger.log(Level.SEVERE, "There was an error when saving guestbook: ", throwable);

            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("guestbook.save.fail"));
    }

    private void initCustomQuestionsForView() {
        if (guestbook.getCustomQuestions() == null || guestbook.getCustomQuestions().isEmpty()) {
            guestbook.setCustomQuestions(new ArrayList<CustomQuestion>());
            addCustomQuestion(0);
        } else {
            for (CustomQuestion customQuestion : guestbook.getCustomQuestions()) {
                if (customQuestion.getCustomQuestionValues() == null) {
                    customQuestion.setCustomQuestionValues(new ArrayList<>());
                }
                if (customQuestion.getCustomQuestionValues().isEmpty()) {
                    addCustomQuestionValue(customQuestion, 0);
                }
            }
        }
    }

    // -------------------- SETTERS --------------------

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public void setGuestbookId(Long guestbookId) {
        this.guestbookId = guestbookId;
    }

    public void setGuestbookName(UIInput guestbookName) {
        this.guestbookName = guestbookName;
    }

}

