package edu.harvard.iq.dataverse.bannersandmessages.messages;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.bannersandmessages.UnsupportedLanguageCleaner;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseTextMessageDto;
import edu.harvard.iq.dataverse.bannersandmessages.validation.DataverseTextMessageValidator;
import edu.harvard.iq.dataverse.bannersandmessages.validation.EndDateMustBeAFutureDate;
import edu.harvard.iq.dataverse.bannersandmessages.validation.EndDateMustNotBeEarlierThanStartingDate;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.JsfHelper;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Date;

@ViewScoped
@Named("EditTextMessagePage")
public class EditTextMessagePage implements Serializable {

    @Inject
    private DataverseTextMessageServiceBean textMessageService;

    @Inject
    PermissionsWrapper permissionsWrapper;

    @EJB
    private DataverseDao dataverseDao;

    @Inject
    private UnsupportedLanguageCleaner languageCleaner;

    private Long dataverseId;
    private Dataverse dataverse;
    private Long textMessageId;

    private DataverseTextMessageDto dto;

    private UIInput fromTimeInput;

    public String init() {
        if (!permissionsWrapper.canEditDataverseTextMessagesAndBanners(dataverseId)) {
            return permissionsWrapper.notAuthorized();
        }

        if (dataverseId == null) {
            return permissionsWrapper.notFound();
        }

        dataverse = dataverseDao.find(dataverseId);

        if (textMessageId != null) {
            dto = textMessageService.getTextMessage(textMessageId);
            languageCleaner.removeMessageLanguagesNotPresentInDataverse(dto);
        } else {
            dto = textMessageService.newTextMessage(dataverseId);
        }
        if (!dto.getDataverseId().equals(dataverseId)) {
            return permissionsWrapper.notAuthorized();
        }
        return null;
    }

    public String save() {
        textMessageService.save(dto);
        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataversemessages.textmessages.new.success"));
        return redirectToTextMessages();
    }

    public String cancel() {
        return redirectToTextMessages();
    }

    public void validateEndDateTime(FacesContext context, UIComponent toValidate, Object rawValue) throws ValidatorException {
        Date toDate = (Date) rawValue;
        Date fromDate = (Date)fromTimeInput.getValue();

        try {
            DataverseTextMessageValidator.validateEndDate(fromDate, toDate);
        } catch (EndDateMustNotBeEarlierThanStartingDate e) {
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("textmessages.endDateTime.valid")));
        } catch (EndDateMustBeAFutureDate e) {
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("textmessages.endDateTime.future")));
        }
    }
    
    private String redirectToTextMessages() {
        return "/dataverse-textMessages.xhtml?dataverseId=" + dataverseId + "&faces-redirect=true";
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public Long getTextMessageId() {
        return textMessageId;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setTextMessageId(Long textMessageId) {
        this.textMessageId = textMessageId;
    }

    public DataverseTextMessageDto getDto() {
        return dto;
    }

    public void setDto(DataverseTextMessageDto dto) {
        this.dto = dto;
    }

    public UIInput getFromTimeInput() {
        return fromTimeInput;
    }

    public void setFromTimeInput(UIInput fromTimeInput) {
        this.fromTimeInput = fromTimeInput;
    }
}
