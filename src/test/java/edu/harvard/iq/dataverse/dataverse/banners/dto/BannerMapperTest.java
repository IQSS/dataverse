package edu.harvard.iq.dataverse.dataverse.banners.dto;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataverse.banners.BannerLimits;
import edu.harvard.iq.dataverse.dataverse.banners.DataverseBanner;
import edu.harvard.iq.dataverse.dataverse.banners.DataverseLocalizedBanner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.primefaces.model.UploadedFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;

import static edu.harvard.iq.dataverse.util.DateUtil.convertToDate;

@RunWith(MockitoJUnitRunner.class)
public class BannerMapperTest {

    private static final Date FROM_TIME = convertToDate(
            LocalDateTime.of(2018, 10, 1, 9, 15, 45));
    private static final Date TO_TIME = convertToDate(
            LocalDateTime.of(2018, 11, 2, 10, 25, 55));
    private static final Path BANNER_PATH = Paths.get("src/test/resources/images/banner.png");

    @Mock
    private UploadedFile uploadedFile;

    private BannerMapper bannerMapper;

    @Before
    public void setup() throws IOException {
        bannerMapper = new BannerMapper(new BannerLimits());

        Mockito.when(uploadedFile.getContents()).thenReturn(Files.readAllBytes(BANNER_PATH));
        Mockito.when(uploadedFile.getContentType()).thenReturn("image/jpeg");
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
        Assert.assertArrayEquals(localizedBanner.getImage(), localizedBannerDto.getImage());
        Assert.assertEquals(localizedBanner.getLocale(), localizedBannerDto.getLocale());
        Assert.assertEquals(localizedBanner.getContentType(), localizedBannerDto.getContentType());
        Assert.assertEquals(localizedBanner.getImageLink().get(), localizedBannerDto.getImageLink());
    }

    @Test
    public void shouldMapToEntity() throws IOException {
        //given
        DataverseBannerDto bannerDto = createBannerDto();
        Dataverse dataverse = new Dataverse();
        dataverse.setId(1L);

        //when

        DataverseLocalizedBannerDto localizedBannerDto = bannerDto.getDataverseLocalizedBanner().get(0);
        localizedBannerDto.setFile(uploadedFile);

        DataverseBanner banner = bannerMapper.mapToEntity(bannerDto, dataverse);
        DataverseLocalizedBanner localizedBanner = banner.getDataverseLocalizedBanner().get(0);

        //then
        Assert.assertEquals(banner.getDataverse().getId(), bannerDto.getDataverseId());
        Assert.assertEquals(banner.getFromTime(), bannerDto.getFromTime());
        Assert.assertEquals(banner.getToTime(), bannerDto.getToTime());
        Assert.assertArrayEquals(localizedBanner.getImage(), localizedBannerDto.getImage());
        Assert.assertEquals(localizedBanner.getLocale(), localizedBannerDto.getLocale());
        Assert.assertEquals(localizedBanner.getContentType(), localizedBannerDto.getContentType());
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
        dataverseLocalizedBanner.setImage(Files.readAllBytes(BANNER_PATH));
        dataverseLocalizedBanner.setImageLink("www.google.pl");
        dataverseBanner.setDataverseLocalizedBanner(Lists.newArrayList(dataverseLocalizedBanner));
        return dataverseBanner;
    }

    private DataverseBannerDto createBannerDto() throws IOException {
        DataverseBannerDto dataverseBannerDto = new DataverseBannerDto();
        dataverseBannerDto.setActive(false);
        dataverseBannerDto.setFromTime(FROM_TIME);
        dataverseBannerDto.setToTime(TO_TIME);
        dataverseBannerDto.setId(1L);
        dataverseBannerDto.setDataverseId(1L);

        DataverseLocalizedBannerDto dataverseLocalizedBannerDto = new DataverseLocalizedBannerDto();
        dataverseLocalizedBannerDto.setContentType("image/jpeg");
        dataverseLocalizedBannerDto.setLocale("en");
        dataverseLocalizedBannerDto.setImage(Files.readAllBytes(BANNER_PATH));
        dataverseLocalizedBannerDto.setImageLink("www.google.pl");
        dataverseBannerDto.setDataverseLocalizedBanner(Lists.newArrayList(dataverseLocalizedBannerDto));
        return dataverseBannerDto;
    }
}
