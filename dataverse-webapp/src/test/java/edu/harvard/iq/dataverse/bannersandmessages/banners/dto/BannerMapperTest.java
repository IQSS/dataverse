package edu.harvard.iq.dataverse.bannersandmessages.banners.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.bannersandmessages.banners.BannerLimits;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseBanner;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseLocalizedBanner;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

import static edu.harvard.iq.dataverse.common.DateUtil.convertToDate;

@RunWith(MockitoJUnitRunner.class)
public class BannerMapperTest {

    private static final Date FROM_TIME = convertToDate(LocalDateTime.of(2018, 10, 1, 9, 15, 45));
    private static final Date TO_TIME = convertToDate(LocalDateTime.of(2018, 11, 2, 10, 25, 55));

    @Mock
    private SettingsWrapper settingsWrapper;

    private BannerMapper bannerMapper;
    private byte[] bannerFile;

    @Before
    public void setup() throws IOException {
        bannerMapper = new BannerMapper(settingsWrapper);
        bannerFile = UnitTestUtils.readFileToByteArray("images/banner.png");

        Mockito.when(settingsWrapper.getConfiguredLocales()).thenReturn(ImmutableMap.of("pl", "pl", "en", "en"));
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
        Assert.assertEquals(bannerDto.getDataverseId(), dataverseBanner.getDataverse().getId());
        Assert.assertEquals(bannerDto.getFromTime(), dataverseBanner.getFromTime());
        Assert.assertEquals(bannerDto.getToTime(), dataverseBanner.getToTime());
        Assert.assertEquals(localizedBanner.getLocale(), localizedBannerDto.getLocale());
        Assert.assertEquals(localizedBanner.getImageLink().get(), localizedBannerDto.getImageLink());
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
        Assert.assertEquals(banner.getDataverse().getId(), bannerDto.getDataverseId());
        Assert.assertEquals(banner.getFromTime(), bannerDto.getFromTime());
        Assert.assertEquals(banner.getToTime(), bannerDto.getToTime());
        Assert.assertTrue(localizedBanner.getImage().length > 0);
        Assert.assertEquals(localizedBanner.getLocale(), localizedBannerDto.getLocale());
        Assert.assertEquals(localizedBanner.getContentType(), localizedBannerDto.getContentType());
        Assert.assertEquals(localizedBanner.getImageName(), localizedBannerDto.getFilename());
        Assert.assertEquals(localizedBanner.getImageLink().get(), localizedBannerDto.getImageLink());
    }

    @Test
    public void shouldMapToNewBanner() {

        //when
        DataverseBannerDto dataverseBannerDto = bannerMapper.mapToNewBanner(1L);

        //then
        Assert.assertSame(1L, dataverseBannerDto.getDataverseId());

        dataverseBannerDto.getDataverseLocalizedBanner()
                .forEach(localeDto -> Assert.assertTrue(localeDto.getLocale().equals("pl")
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
