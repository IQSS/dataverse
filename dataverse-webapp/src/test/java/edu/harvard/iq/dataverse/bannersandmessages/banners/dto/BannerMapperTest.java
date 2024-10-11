package edu.harvard.iq.dataverse.bannersandmessages.banners.dto;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseBanner;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseLocalizedBanner;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

import static edu.harvard.iq.dataverse.common.DateUtil.convertToDate;

@ExtendWith(MockitoExtension.class)
public class BannerMapperTest {

    private static final Date FROM_TIME = convertToDate(LocalDateTime.of(2018, 10, 1, 9, 15, 45));
    private static final Date TO_TIME = convertToDate(LocalDateTime.of(2018, 11, 2, 10, 25, 55));

    @Mock
    private SettingsWrapper settingsWrapper;

    private BannerMapper bannerMapper;
    private byte[] bannerFile;

    @BeforeEach
    public void setup() throws IOException {
        bannerMapper = new BannerMapper(settingsWrapper);
        bannerFile = UnitTestUtils.readFileToByteArray("images/banner.png");
    }

    @Test
    public void shouldMapToDto() throws IOException {
        //given
        DataverseBanner dataverseBanner = createBannerEntity();

        //when
        DataverseBannerDto bannerDto = bannerMapper.mapToDto(dataverseBanner);
        DataverseLocalizedBannerDto localizedBannerDto = bannerDto.getDataverseLocalizedBanner().get(0);
        DataverseLocalizedBanner localizedBanner = dataverseBanner.getDataverseLocalizedBanner().get(0);

        //then
        Assertions.assertEquals(bannerDto.getDataverseId(), dataverseBanner.getDataverse().getId());
        Assertions.assertEquals(bannerDto.getFromTime(), dataverseBanner.getFromTime());
        Assertions.assertEquals(bannerDto.getToTime(), dataverseBanner.getToTime());
        Assertions.assertEquals(localizedBanner.getLocale(), localizedBannerDto.getLocale());
        Assertions.assertEquals(localizedBanner.getImageLink().get(), localizedBannerDto.getImageLink());
    }

    @Test
    public void shouldMapToNewEntity() {
        //given
        DataverseBannerDto bannerDto = createBannerDto();
        Dataverse dataverse = new Dataverse();
        dataverse.setId(1L);
        DataverseLocalizedBannerDto localizedBannerDto = bannerDto.getDataverseLocalizedBanner().get(0);
        localizedBannerDto.setContent(bannerFile);
        localizedBannerDto.setContentType("image/jpeg");

        //when
        DataverseBanner banner = bannerMapper.mapToEntity(bannerDto, dataverse);

        //then
        DataverseLocalizedBanner localizedBanner = banner.getDataverseLocalizedBanner().get(0);
        Assertions.assertEquals(banner.getDataverse().getId(), bannerDto.getDataverseId());
        Assertions.assertEquals(banner.getFromTime(), bannerDto.getFromTime());
        Assertions.assertEquals(banner.getToTime(), bannerDto.getToTime());
        Assertions.assertTrue(localizedBanner.getImage().length > 0);
        Assertions.assertEquals(localizedBanner.getLocale(), localizedBannerDto.getLocale());
        Assertions.assertEquals(localizedBanner.getContentType(), localizedBannerDto.getContentType());
        Assertions.assertEquals(localizedBanner.getImageName(), localizedBannerDto.getFilename());
        Assertions.assertEquals(localizedBanner.getImageLink().get(), localizedBannerDto.getImageLink());
    }

    @Test
    public void shouldMapToNewBanner() {

        //when
        DataverseBannerDto dataverseBannerDto = bannerMapper.mapToNewBanner(1L);

        //then
        Assertions.assertSame(1L, dataverseBannerDto.getDataverseId());

        dataverseBannerDto.getDataverseLocalizedBanner()
                .forEach(localeDto -> Assertions.assertTrue(localeDto.getLocale().equals("pl")
                                                                || localeDto.getLocale().equals("en")));
    }

    private DataverseBanner createBannerEntity() throws IOException {
        DataverseBanner dataverseBanner = new DataverseBanner();
        dataverseBanner.setActive(false);
        dataverseBanner.setFromTime(FROM_TIME);
        dataverseBanner.setToTime(TO_TIME);
        dataverseBanner.setId(1L);
        Dataverse dataverse = new Dataverse();
        dataverse.setId(1L);
        dataverseBanner.setDataverse(dataverse);

        DataverseLocalizedBanner dataverseLocalizedBanner = new DataverseLocalizedBanner();
        dataverseLocalizedBanner.setContentType("image/jpeg");
        dataverseLocalizedBanner.setLocale("en");
        dataverseLocalizedBanner.setImageName("Best Image");
        dataverseLocalizedBanner.setImage(bannerFile);
        dataverseLocalizedBanner.setImageLink("www.google.pl");
        dataverseBanner.setDataverseLocalizedBanner(Lists.newArrayList(dataverseLocalizedBanner));
        return dataverseBanner;
    }

    private DataverseBannerDto createBannerDto() {
        DataverseBannerDto dataverseBannerDto = new DataverseBannerDto();
        dataverseBannerDto.setActive(false);
        dataverseBannerDto.setFromTime(FROM_TIME);
        dataverseBannerDto.setToTime(TO_TIME);
        dataverseBannerDto.setId(1L);
        dataverseBannerDto.setDataverseId(1L);

        DataverseLocalizedBannerDto dataverseLocalizedBannerDto = new DataverseLocalizedBannerDto();
        dataverseLocalizedBannerDto.setLocale("en");
        dataverseLocalizedBannerDto.setImageLink("www.google.pl");
        dataverseBannerDto.setDataverseLocalizedBanner(Lists.newArrayList(dataverseLocalizedBannerDto));
        return dataverseBannerDto;
    }
}
