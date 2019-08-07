package edu.harvard.iq.dataverse.license.dto;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseIcon;
import edu.harvard.iq.dataverse.persistence.datafile.license.LocaleText;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

public class LicenseMapperTest {

    private LicenseMapper licenseMapper = new LicenseMapper();

    // -------------------- TEST --------------------

    @Test
    public void shouldCorrectlyMapToDto() {
        //given
        License license = createTestLicense();

        //when
        LicenseDto licenseDto = licenseMapper.mapToDto(license);

        //then
        Assert.assertEquals(new Long(1L), licenseDto.getId());
        Assert.assertEquals(new Long(1L), licenseDto.getPosition());
        Assert.assertEquals("testLicense", licenseDto.getName());
        Assert.assertEquals("www.test.com", licenseDto.getUrl());

        Assert.assertEquals(Locale.ENGLISH, licenseDto.getLocalizedNames().get(0).getLocale());
        Assert.assertEquals("English license", licenseDto.getLocalizedNames().get(0).getText());
        Assert.assertEquals(new Locale("pl"), licenseDto.getLocalizedNames().get(1).getLocale());
        Assert.assertEquals("Polish license", licenseDto.getLocalizedNames().get(1).getText());

        Assert.assertNotNull(licenseDto.getIcon().getContent());

    }

    @Test
    public void shouldCorrectlyMapToSimpleDto() {
        //given
        License license = createTestLicense();

        //when
        LicenseSimpleDto simpleDto = licenseMapper.mapToSimpleDto(license, Locale.forLanguageTag("en"));

        //then
        Assert.assertEquals(license.getId(), simpleDto.getLicenseId());
        Assert.assertEquals(license.getLocalizedName(Locale.forLanguageTag("en")), simpleDto.getLocalizedText());
    }

    @Test
    public void shouldCorrectlyMapToLicense() {
        //given
        LicenseDto licenseDto = createTestLicenseDto();

        //when
        License license = licenseMapper.mapToLicense(licenseDto);

        //then
        Assert.assertEquals("testLicense", license.getName());
        Assert.assertEquals("http://www.google.pl", license.getUrl());
        Assert.assertEquals(Long.valueOf(99), license.getPosition());
        Assert.assertEquals(Locale.ENGLISH, license.getLocalizedNames().get(0).getLocale());
        Assert.assertEquals("English license", license.getLocalizedNames().get(0).getText());
        Assert.assertEquals(new Locale("pl"), license.getLocalizedNames().get(1).getLocale());
        Assert.assertEquals("Polish license", license.getLocalizedNames().get(1).getText());
    }

    // -------------------- PRIVATE --------------------

    private License createTestLicense() {
        License license = new License();
        license.setActive(true);
        license.setId(1L);
        license.setName("testLicense");
        license.setPosition(1L);
        license.setUrl("www.test.com");
        createLocaleTexts().forEach(license::addLocalizedName);
        license.setIcon(createLicenseIcon(license));

        return license;
    }

    private LicenseDto createTestLicenseDto() {
        LicenseDto licenseDto = new LicenseDto();
        licenseDto.setUrl("http://www.google.pl");
        licenseDto.setName("testLicense");
        licenseDto.setPosition(99L);
        licenseDto.setActive(false);
        licenseDto.setLocalizedNames(createLocaleTextsDto());
        licenseDto.setIcon(new LicenseIconDto());

        return licenseDto;
    }

    private LicenseIcon createLicenseIcon(License license) {

        LicenseIcon licenseIcon = new LicenseIcon();
        licenseIcon.setContent(new byte[0]);
        licenseIcon.setContentType("png");
        licenseIcon.setLicense(license);

        return licenseIcon;
    }

    private List<LocaleTextDto> createLocaleTextsDto() {
        LocaleTextDto englishText = new LocaleTextDto(Locale.ENGLISH, "English license");
        LocaleTextDto polishText = new LocaleTextDto(new Locale("pl"), "Polish license");

        return Lists.newArrayList(englishText, polishText);
    }

    private List<LocaleText> createLocaleTexts() {
        LocaleText englishText = new LocaleText(Locale.ENGLISH, "English license");
        LocaleText polishText = new LocaleText(new Locale("pl"), "Polish license");

        return Lists.newArrayList(englishText, polishText);
    }

}