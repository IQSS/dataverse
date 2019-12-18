package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DataverseDao;
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
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJBException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author skraffmiller
 */
@ViewScoped
@Named("GuestbookPage")
public class GuestbookPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(GuestbookPage.class.getCanonicalName());

    private GuestbookServiceBean guestbookHelperService;
    private DataverseDao dataverseDao;
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
    private Boolean customQuestions = Boolean.FALSE;

    private UIInput guestbookName;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public GuestbookPage() {
    }

    @Inject
    public GuestbookPage(GuestbookServiceBean guestbookHelperService, DataverseDao dataverseDao,
                         PermissionsWrapper permissionsWrapper, GuestbookService guestbookService) {
        this.guestbookHelperService = guestbookHelperService;
        this.dataverseDao = dataverseDao;
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

    public Boolean getCustomQuestions() {
        return customQuestions;
    }

    public UIInput getGuestbookName() {
        return guestbookName;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        dataverse = dataverseDao.find(ownerId);
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
            guestbook = new Guestbook();
            guestbook.setDataverse(dataverse);
            // create mode for a new template
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
        toAdd.setGuestbook(guestbook);
        addInitialEmptyCustomQuestionValues(toAdd);
        guestbook.addCustomQuestion(indexIn, toAdd);
    }

    public String removeCustomQuestion(Long index) {
        guestbook.removeCustomQuestion(index.intValue());
        return StringUtils.EMPTY;
    }

    public void addCustomQuestionValue(CustomQuestion question, int index) {
        CustomQuestionValue toAdd = new CustomQuestionValue();
        toAdd.setValueString(StringUtils.EMPTY);
        toAdd.setCustomQuestion(question);
        question.addCustomQuestionValue(index, toAdd);
    }

    public void removeCustomQuestionValue(CustomQuestion question, Long index) {
        question.removeCustomQuestionValue(index.intValue());
    }

    public String save() {
        if (!isFormValid()) {
            return StringUtils.EMPTY;
        }

        if (guestbook.getCustomQuestions() != null) {
            removeUnusedQuestionValues();
            renumberQuestionsDisplayOrder();
        }

        if (editMode == EditMode.CREATE || editMode == EditMode.CLONE) {
            Try<Dataverse> guestbookTry = Try.of(() -> guestbookService.saveGuestbook(guestbook))
                    .onSuccess(dv -> JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("guestbook.create")))
                    .onFailure(this::handleErrorMessages);

            if(guestbookTry.isFailure() && guestbookTry.getCause() instanceof EJBException) {
                return StringUtils.EMPTY;
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
            guestbook.setCustomQuestions(new ArrayList<>());
            addCustomQuestion(0);
        } else {
            this.customQuestions = Boolean.TRUE;
            for (CustomQuestion question : guestbook.getCustomQuestions()) {
                addInitialEmptyCustomQuestionValues(question);
            }
        }
    }

    private void addInitialEmptyCustomQuestionValues(CustomQuestion question) {
        List<CustomQuestionValue> questionValues = question.getCustomQuestionValues();
        if (questionValues == null || questionValues.isEmpty()) {
            question.setCustomQuestionValues(new ArrayList<>());
            addCustomQuestionValue(question, 0);
            addCustomQuestionValue(question, 1);
        }
    }

    private void removeUnusedQuestionValues() {
        for (CustomQuestion question : guestbook.getCustomQuestions()) {
            if ("text".equals(question.getQuestionType())) {
                question.setCustomQuestionValues(null);
            }
        }
    }

    private void renumberQuestionsDisplayOrder() {
        int i = 0;
        for (CustomQuestion question : guestbook.getCustomQuestions()) {
            question.setDisplayOrder(i);
            if (question.getCustomQuestionValues() != null && !question.getCustomQuestionValues().isEmpty()) {
                int j = 0;
                for (CustomQuestionValue questionValue : question.getCustomQuestionValues()) {
                    questionValue.setDisplayOrder(j);
                    j++;
                }
            }
            i++;
        }
    }

    private boolean isFormValid() {
        validateName();
        validateCustomQuestions();
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return !facesContext.isValidationFailed();
    }

    private void validateName() {
        if (StringUtils.isNotBlank(guestbook.getName())) {
            return;
        }
        JsfHelper.addErrorMessage(guestbookName.getClientId(), StringUtils.EMPTY,
                BundleUtil.getStringFromBundle("guestbook.name.empty"));
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.validationFailed();
    }

    private void validateCustomQuestions() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        VisitContext visitContext = VisitContext.createVisitContext(
                facesContext,
                null,
                Collections.singleton(VisitHint.SKIP_UNRENDERED) // this causes validation of visible components only
        );
        UIViewRoot viewRoot = facesContext.getViewRoot();
        viewRoot.visitTree(visitContext, this::processComponent);
    }

    private VisitResult processComponent(VisitContext context, UIComponent uiComponent) {
        if (isInputForCustomQuestion(uiComponent)) {
            UIInput input = (UIInput) uiComponent;
            if (StringUtils.isBlank((String) input.getValue())) {
                JsfHelper.addErrorMessage(input.getClientId(), StringUtils.EMPTY,
                        BundleUtil.getStringFromBundle("guestbook.field.empty"));
                FacesContext facesContext = context.getFacesContext();
                facesContext.validationFailed();
            }
        }
        return VisitResult.ACCEPT;
    }

    private boolean isInputForCustomQuestion(UIComponent uiComponent) {
        if (uiComponent == null) {
            return false;
        }
        String clientId = uiComponent.getClientId();
        return uiComponent instanceof UIInput
                && (clientId.contains("questionText") || clientId.contains("responseText"));
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

    public void setCustomQuestions(Boolean customQuestions) {
        this.customQuestions = customQuestions;
    }

    public void setGuestbookName(UIInput guestbookName) {
        this.guestbookName = guestbookName;
    }
}

