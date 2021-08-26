package edu.harvard.iq.dataverse.bannersandmessages.banners;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.bannersandmessages.UnsupportedLanguageCleaner;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.BannerMapper;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.DataverseBannerDto;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.DataverseLocalizedBannerDto;
import edu.harvard.iq.dataverse.bannersandmessages.validation.BannerErrorHandler;
import edu.harvard.iq.dataverse.bannersandmessages.validation.DataverseTextMessageValidator;
import edu.harvard.iq.dataverse.bannersandmessages.validation.EndDateMustBeAFutureDate;
import edu.harvard.iq.dataverse.bannersandmessages.validation.EndDateMustNotBeEarlierThanStartingDate;
import edu.harvard.iq.dataverse.bannersandmessages.validation.ImageValidator;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.config.URLValidator;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseBanner;
import edu.harvard.iq.dataverse.util.JsfHelper;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Date;

@ViewScoped
@Named("EditBannerPage")
public class NewBannerPage implements Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    private BannerDAO dao;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    @Inject
    private BannerErrorHandler errorHandler;

    @Inject
    private BannerMapper mapper;

    @EJB
    private DataverseDao dataverseDao;

    @Inject
    private UnsupportedLanguageCleaner languageCleaner;

    @Inject
    private BannerLimits bannerLimits;
    
    
    private Long dataverseId;
    private Dataverse dataverse;
    private Long bannerId;
    private String link;

    private UIInput fromTimeInput;

    private DataverseBannerDto dto;

    public String init() {
        if (!permissionsWrapper.canEditDataverseTextMessagesAndBanners(dataverseId)) {
            return permissionsWrapper.notAuthorized();
        }

        if (dataverseId == null) {
            return permissionsWrapper.notFound();
        }

        dataverse = dataverseDao.find(dataverseId);

        dto = bannerId != null ?
                mapper.mapToDto(dao.getBanner(bannerId)) :
                mapper.mapToNewBanner(dataverseId);

        if (dto.getId() != null) {
            languageCleaner.removeBannersLanguagesNotPresentInDataverse(dto);
        }

        return StringUtils.EMPTY;
    }

    public boolean hasDisplayLocalizedBanner(DataverseLocalizedBannerDto localizedBanner) {
        return localizedBanner.getContent() != null;
    }
    
    public StreamedContent getDisplayLocalizedBanner(DataverseLocalizedBannerDto localizedBanner) {
        if (localizedBanner.getContent() == null) {
            return null;
        }
        return DefaultStreamedContent.builder()
                            .contentType(localizedBanner.getContentType())
                            .name(localizedBanner.getFilename())
                            .stream(() -> new ByteArrayInputStream(localizedBanner.getContent()))
                            .build();
    }
    
    public void uploadFileEvent(FileUploadEvent event) {
        
        if (ImageValidator.isImageResolutionTooBig(event.getFile().getContent(),
                bannerLimits.getMaxWidth(), bannerLimits.getMaxHeight())) {
            
            FacesContext context = FacesContext.getCurrentInstance();
            context.validationFailed();
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("dataversemessages.banners.resolutionError"));
            FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(context), message);
            return;
        }
        
        String locale = (String) event.getComponent().getAttributes()
                .get("imageLocale");

        dto.getDataverseLocalizedBanner().stream()
                .filter(dlb -> dlb.getLocale().equals(locale))
                .forEach(dlb -> {
                    dlb.setContentType(event.getFile().getContentType());
                    dlb.setFilename(event.getFile().getFileName());
                    dlb.setContent(event.getFile().getContent());
                });
    }

    public String save() {
        DataverseBanner banner =
                mapper.mapToEntity(dto, em.find(Dataverse.class, dto.getDataverseId()));

        banner.getDataverseLocalizedBanner().forEach(dlb ->
                                                             errorHandler.handleBannerAddingErrors(banner, dlb, FacesContext.getCurrentInstance()));

        if (errorsOccurred()) {
            return StringUtils.EMPTY;
        }

        dao.save(banner);
        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataversemessages.banners.new.success"));
        return redirectToTextMessages();
    }

    public void validateLink(FacesContext context, UIComponent toValidate, Object rawValue) throws ValidatorException {
        String valueStr = (String)rawValue;
        
        if (!URLValidator.isURLValid(valueStr)) {
            String message = "'" + valueStr + "'  " + BundleUtil.getStringFromBundle("url.invalid");
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "", message));
        }
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

    public int getBannerFileSizeLimit() {
        return bannerLimits.getMaxSizeInBytes();
    }
    
    private boolean errorsOccurred() {
        return FacesContext.getCurrentInstance().getMessageList().size() > 0;
    }

    public String cancel() {
        return redirectToTextMessages();
    }

    private String redirectToTextMessages() {
        return "/dataverse-textMessages.xhtml?dataverseId=" + dataverseId + "&activeTab=banners&faces-redirect=true";
    }

    public PermissionsWrapper getPermissionsWrapper() {
        return permissionsWrapper;
    }

    public void setPermissionsWrapper(PermissionsWrapper permissionsWrapper) {
        this.permissionsWrapper = permissionsWrapper;
    }

    public DataverseDao getDataverseDao() {
        return dataverseDao;
    }

    public void setDataverseDao(DataverseDao dataverseDao) {
        this.dataverseDao = dataverseDao;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public Long getBannerId() {
        return bannerId;
    }

    public void setBannerId(Long bannerId) {
        this.bannerId = bannerId;
    }

    public DataverseBannerDto getDto() {
        return dto;
    }

    public void setDto(DataverseBannerDto dto) {
        this.dto = dto;
    }

    public UIInput getFromTimeInput() {
        return fromTimeInput;
    }

    public void setFromTimeInput(UIInput fromTimeInput) {
        this.fromTimeInput = fromTimeInput;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
