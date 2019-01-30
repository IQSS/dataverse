package edu.harvard.iq.dataverse.dataverse.banners;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataverse.banners.dto.BannerMapper;
import edu.harvard.iq.dataverse.dataverse.banners.dto.DataverseBannerDto;
import edu.harvard.iq.dataverse.dataverse.validation.BannerErrorHandler;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;

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
    private DataverseServiceBean dataverseServiceBean;

    private Long dataverseId;
    private Dataverse dataverse;
    private Long bannerId;
    private String link;

    private UIInput fromTimeInput;
    private UIInput toTimeInput;

    private DataverseBannerDto dto;

    public String init() {
        if (!permissionsWrapper.canEditDataverseTextMessagesAndBanners(dataverseId)) {
            return permissionsWrapper.notAuthorized();
        }

        if (dataverseId == null) {
            return permissionsWrapper.notFound();
        }

        dataverse = dataverseServiceBean.find(dataverseId);

        dto = bannerId != null ?
                mapper.mapToDto(dao.getBanner(bannerId)) :
                mapper.mapToNewBanner(dataverseId);

        return StringUtils.EMPTY;
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
        return redirectToTextMessages();
    }

    private boolean errorsOccurred() {
        return FacesContext.getCurrentInstance().getMessageList().size() > 0;
    }

    public String cancel() {
        return redirectToTextMessages();
    }

    private String redirectToTextMessages() {
        return "/dataverse-textMessages.xhtml?dataverseId=" + dataverseId + "&faces-redirect=true";
    }

    public PermissionsWrapper getPermissionsWrapper() {
        return permissionsWrapper;
    }

    public void setPermissionsWrapper(PermissionsWrapper permissionsWrapper) {
        this.permissionsWrapper = permissionsWrapper;
    }

    public DataverseServiceBean getDataverseServiceBean() {
        return dataverseServiceBean;
    }

    public void setDataverseServiceBean(DataverseServiceBean dataverseServiceBean) {
        this.dataverseServiceBean = dataverseServiceBean;
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

    public UIInput getToTimeInput() {
        return toTimeInput;
    }

    public void setToTimeInput(UIInput toTimeInput) {
        this.toTimeInput = toTimeInput;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
