package edu.harvard.iq.dataverse.api;

import com.amazonaws.services.pi.model.InvalidArgumentException;
import edu.harvard.iq.dataverse.api.dto.FileTermsOfUseDTO;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.security.InvalidParameterException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Stateless
public class DatasetsValidators {
    private LicenseRepository licenseRepository;
    private SettingsServiceBean settingsService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public DatasetsValidators() {
    }

    @Inject
    public DatasetsValidators(LicenseRepository licenseRepository, SettingsServiceBean settingsService) {
        this.licenseRepository = licenseRepository;
        this.settingsService = settingsService;
    }

    // -------------------- LOGIC --------------------
    public void validateFileTermsOfUseDTO(FileTermsOfUseDTO fileTermsOfUseDTO) throws InvalidParameterException, MissingArgumentException {
        if(fileTermsOfUseDTO == null) {
            throw new MissingArgumentException("datasets.api.add.termsOfUseAndAccess.missingFileTermsOfUseDto");
        }
        if(StringUtils.isEmpty(fileTermsOfUseDTO.getTermsType())) {
            throw new InvalidParameterException(BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.missingTermsOfUse"));
        }

        if(fileTermsOfUseDTO.getTermsType().equals(FileTermsOfUse.TermsOfUseType.LICENSE_BASED.toString())) {
            validateLicenseBasedTerms(fileTermsOfUseDTO);
        } else if(fileTermsOfUseDTO.getTermsType().equals(FileTermsOfUse.TermsOfUseType.ALL_RIGHTS_RESERVED.toString())) {
            validateAllRightsReservedTerms(fileTermsOfUseDTO);
        } else if(fileTermsOfUseDTO.getTermsType().equals(FileTermsOfUse.TermsOfUseType.RESTRICTED.toString())) {
            validateRestrictedTerms(fileTermsOfUseDTO);
        } else {
            throw new InvalidParameterException(BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.invalidTermsOfUse"));
        }
    }

    public void validateEmbargoDate(Date embargoDate) throws InvalidArgumentException {
        if (embargoDate.toInstant().isBefore(getTomorrowsDateInstant())) {
            throw new InvalidArgumentException(BundleUtil.getStringFromBundle("datasets.api.setEmbargo.failure.badDate.notFuture"));
        }
        if (isMaximumEmbargoLengthSet() && embargoDate.toInstant().isAfter(getMaximumEmbargoDate())) {
            throw new InvalidArgumentException(BundleUtil.getStringFromBundle("datasets.api.setEmbargo.failure.badDate.tooLong",
                    settingsService.getValueForKey(SettingsServiceBean.Key.MaximumEmbargoLength)));
        }
    }

    // -------------------- PRIVATE ---------------------
    private void validateRestrictedTerms(FileTermsOfUseDTO fileTermsOfUseDTO) {
        if(fileTermsOfUseDTO.getAccessConditions() == null) {
            throw new InvalidParameterException(BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.restrictedTerms.missingAccessConditions",
                    FileTermsOfUse.TermsOfUseType.RESTRICTED.toString()));
        }
        if(fileTermsOfUseDTO.getLicense() != null) {
            throw new InvalidParameterException(BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.restrictedTerms.invalidLicenseParameter",
                    FileTermsOfUse.TermsOfUseType.RESTRICTED.toString()));
        }

        if(!containsRestrictType(fileTermsOfUseDTO.getAccessConditions())) {
            throw new InvalidParameterException(BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.restrictedTerms.invalidAccessConditions",
                    fileTermsOfUseDTO.getAccessConditions()));
        }

        if(fileTermsOfUseDTO.getAccessConditions().equals(FileTermsOfUse.RestrictType.CUSTOM.toString()) && StringUtils.isEmpty(fileTermsOfUseDTO.getAccessConditionsCustomText())) {
            throw new InvalidParameterException(BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.restrictedTerms.missingAccessConditionsCustomText",
                    FileTermsOfUse.RestrictType.CUSTOM.toString()));
        }

        if(!fileTermsOfUseDTO.getAccessConditions().equals(FileTermsOfUse.RestrictType.CUSTOM.toString()) && fileTermsOfUseDTO.getAccessConditionsCustomText() != null) {
            throw new InvalidParameterException(BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.restrictedTerms.invalidRemainingParameters",
                    fileTermsOfUseDTO.getAccessConditions()));
        }
    }

    private void validateAllRightsReservedTerms(FileTermsOfUseDTO fileTermsOfUseDTO) {
        if(fileTermsOfUseDTO.getAccessConditionsCustomText() != null || fileTermsOfUseDTO.getAccessConditions() != null || fileTermsOfUseDTO.getLicense() != null) {
            throw new InvalidParameterException(BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.allRightsReservedTerms.invalidRemainingParameters",
                    FileTermsOfUse.TermsOfUseType.ALL_RIGHTS_RESERVED.toString()));
        }
    }

    private void validateLicenseBasedTerms(FileTermsOfUseDTO fileTermsOfUseDTO) {
        if(fileTermsOfUseDTO.getAccessConditionsCustomText() != null || fileTermsOfUseDTO.getAccessConditions() != null || fileTermsOfUseDTO.getLicense() == null) {
            throw new InvalidParameterException(BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.licenseBasedTerms.invalidRemainingParameters",
                    FileTermsOfUse.TermsOfUseType.LICENSE_BASED.toString()));
        }

        licenseRepository.findActiveOrderedByPosition()
                .stream()
                .filter(license -> license.getName().equals(fileTermsOfUseDTO.getLicense()))
                .findAny()
                .orElseThrow(() -> new InvalidParameterException(
                        BundleUtil.getStringFromBundle("datasets.api.add.termsOfUseAndAccess.licenseBasedTerms.invalidParameter", fileTermsOfUseDTO.getLicense())));
    }

    private boolean containsRestrictType(String apiValue) {
        for (FileTermsOfUse.RestrictType restrictType : FileTermsOfUse.RestrictType.values()) {
            if (restrictType.toString().equals(apiValue)) {
                return true;
            }
        }
        return false;
    }

    private Instant getTomorrowsDateInstant() {
        return Date.from(Instant.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)).toInstant();
    }

    private boolean isMaximumEmbargoLengthSet() {
        return settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.MaximumEmbargoLength) > 0;
    }

    private Instant getMaximumEmbargoDate() {
        return Date.from(Instant
                .now().atOffset(ZoneOffset.UTC)
                .plus(settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaximumEmbargoLength), ChronoUnit.MONTHS)
                .toInstant()).toInstant();
    }

}
