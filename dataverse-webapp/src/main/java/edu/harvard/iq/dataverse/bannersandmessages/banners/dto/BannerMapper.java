package edu.harvard.iq.dataverse.bannersandmessages.banners.dto;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseBanner;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseLocalizedBanner;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class BannerMapper {

    private static final Logger logger = Logger.getLogger(BannerMapper.class.getCanonicalName());

    private SettingsWrapper settingsWrapper;

    public BannerMapper() {
    }

    @Inject
    public BannerMapper(SettingsWrapper settingsWrapper) {
        this.settingsWrapper = settingsWrapper;
    }

    public DataverseBannerDto mapToDto(DataverseBanner dataverseBanner) {
        DataverseBannerDto dto = new DataverseBannerDto();

        dto.setId(dataverseBanner.getId());
        dto.setFromTime(dataverseBanner.getFromTime());
        dto.setToTime(dataverseBanner.getToTime());
        dto.setActive(dataverseBanner.isActive());
        dto.setDataverseId(dataverseBanner.getDataverse().getId());

        List<DataverseLocalizedBannerDto> dlbDto = new ArrayList<>();

        for (DataverseLocalizedBanner dlb : dataverseBanner.getDataverseLocalizedBanner()) {

            DataverseLocalizedBannerDto localBannerDto =
                    new DataverseLocalizedBannerDto(dlb.getId(), dlb.getLocale(),
                                                    dlb.getImageLink().isPresent() ?
                                                            dlb.getImageLink().get() : StringUtils.EMPTY);
            
            localBannerDto.setContent(dlb.getImage());
            localBannerDto.setContentType(dlb.getContentType());
            localBannerDto.setFilename(dlb.getImageName());

            dlbDto.add(localBannerDto);
        }

        dto.setDataverseLocalizedBanner(dlbDto);

        return dto;
    }

    public List<DataverseBannerDto> mapToDtos(List<DataverseBanner> dataverseBanners) {
        List<DataverseBannerDto> dtos = new ArrayList<>();

        dataverseBanners.forEach(dataverseBanner -> dtos.add(mapToDto(dataverseBanner)));

        return dtos;
    }

    public DataverseBanner mapToEntity(DataverseBannerDto dto, Dataverse dataverse) {
        DataverseBanner banner = new DataverseBanner();

        banner.setActive(dto.isActive());
        banner.setFromTime(dto.getFromTime());
        banner.setToTime(dto.getToTime());
        banner.setDataverse(dataverse);

        dto.getDataverseLocalizedBanner()
                .forEach(fuDto -> {
                    DataverseLocalizedBanner dataverseLocalizedBanner = mapToLocalizedBanner(banner, fuDto);

                    banner.getDataverseLocalizedBanner().add(dataverseLocalizedBanner);
                });

        return banner;
    }

    public DataverseBannerDto mapToNewBanner(Long dataverseId) {
        DataverseBannerDto dto = new DataverseBannerDto();

        dto.setDataverseId(dataverseId);
        dto.setDataverseLocalizedBanner(mapDefaultLocales());

        return dto;
    }

    private DataverseLocalizedBanner mapToLocalizedBanner(DataverseBanner banner, DataverseLocalizedBannerDto fuDto) {
        DataverseLocalizedBanner dataverseLocalizedBanner = new DataverseLocalizedBanner();

        if (fuDto.getContent() == null) {
            dataverseLocalizedBanner.setImage(new byte[0]);
        } else {

            dataverseLocalizedBanner.setImage(fuDto.getContent());
            dataverseLocalizedBanner.setContentType(fuDto.getContentType());
            dataverseLocalizedBanner.setImageName(fuDto.getFilename());
        }

        dataverseLocalizedBanner.setImageLink(fuDto.getImageLink());
        dataverseLocalizedBanner.setDataverseBanner(banner);
        dataverseLocalizedBanner.setLocale(fuDto.getLocale());
        return dataverseLocalizedBanner;
    }

    private List<DataverseLocalizedBannerDto> mapDefaultLocales() {
        Map<String, String> locales = settingsWrapper.getConfiguredLocales();

        return locales.entrySet().stream()
                .map(e -> new DataverseLocalizedBannerDto(e.getKey()))
                .collect(Collectors.toList());
    }
}
