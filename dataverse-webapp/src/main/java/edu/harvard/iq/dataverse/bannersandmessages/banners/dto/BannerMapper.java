package edu.harvard.iq.dataverse.bannersandmessages.banners.dto;

import edu.harvard.iq.dataverse.bannersandmessages.banners.BannerLimits;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseBanner;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseLocalizedBanner;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import org.apache.commons.lang.StringUtils;
import org.imgscalr.Scalr;
import org.primefaces.model.DefaultStreamedContent;
import org.springframework.util.StreamUtils;

import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class BannerMapper {

    private static final Logger logger = Logger.getLogger(BannerMapper.class.getCanonicalName());

    private BannerLimits bannerLimits;

    private SettingsWrapper settingsWrapper;

    public BannerMapper() {
    }

    @Inject
    public BannerMapper(BannerLimits bannerLimits, SettingsWrapper settingsWrapper) {
        this.bannerLimits = bannerLimits;
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

            ByteArrayOutputStream resizedImage = convertImageToMiniSize(dlb.getImage());

            localBannerDto.setDisplayedImage(DefaultStreamedContent.builder()
                                                     .contentType(dlb.getContentType())
                                                     .name(dlb.getImageName())
                                                     .stream(() -> new ByteArrayInputStream(dlb.getImage()))
                                                     .build());
            localBannerDto.setMiniDisplayImage(
                    DefaultStreamedContent.builder()
                            .contentType("image/jpeg")
                            .stream(() -> new ByteArrayInputStream(resizedImage.toByteArray()))
                            .build());

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

        if (fuDto.getDisplayedImage() == null) {
            dataverseLocalizedBanner.setImage(new byte[0]);
        } else {

            try {
                dataverseLocalizedBanner.setImage(StreamUtils.copyToByteArray(fuDto.getDisplayedImage().getStream()));
            } catch (IOException e) {
                throw new IllegalStateException("There was a problem converting display image to byte array", e);
            }

            dataverseLocalizedBanner.setContentType(fuDto.getDisplayedImage().getContentType());
            dataverseLocalizedBanner.setImageName(fuDto.getDisplayedImage().getName());
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

    private ByteArrayOutputStream convertImageToMiniSize(byte[] image) {

        try {
            BufferedImage loadedImage = ImageIO.read(new ByteArrayInputStream(image));

            BufferedImage resizedImage = Scalr.resize(loadedImage,
                                                      bannerLimits.getMaxWidth() / 3,
                                                      bannerLimits.getMaxHeight() / 3);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpeg", os);
            return os;

        } catch (IOException e) {
            throw new RuntimeException("There was a problem loading the image", e);
        }
    }
}
