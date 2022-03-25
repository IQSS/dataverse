package edu.harvard.iq.dataverse.authorization.providers.saml;

import edu.harvard.iq.dataverse.api.dto.SamlIdentityProviderDTO;
import edu.harvard.iq.dataverse.interceptors.SuperuserRequired;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProvider;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProviderRepository;
import io.vavr.control.Either;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Stateless
public class SamlIdpManagementService {

    private SamlIdentityProviderRepository idpRepository;

    private SamlIdentityProviderDTO.Converter converter = new SamlIdentityProviderDTO.Converter();

    // -------------------- CONSTRUCTORS --------------------

    public SamlIdpManagementService() { }

    @Inject
    public SamlIdpManagementService(SamlIdentityProviderRepository idpRepository) {
        this.idpRepository = idpRepository;
    }

    // -------------------- LOGIC --------------------

    @SuperuserRequired
    public List<SamlIdentityProviderDTO> listAll() {
        return idpRepository.findAll().stream()
                .map(converter::convert)
                .collect(Collectors.toList());
    }

    @SuperuserRequired
    public Optional<SamlIdentityProviderDTO> listSingle(Long id) {
        Optional<SamlIdentityProvider> idp = idpRepository.findById(id);
        return idp.map(converter::convert);
    }

    @SuperuserRequired
    public Either<String, List<SamlIdentityProviderDTO>> create(List<SamlIdentityProviderDTO> providers) {
        if (providers.isEmpty()
                || !providers.stream().allMatch(p -> notNull(p::getEntityId, p::getMetadataUrl, p::getDisplayName))) {
            return Either.left("No providers data or incomplete data for at least one provider");
        }
        if (providers.stream().anyMatch(p -> p.getId() != null)) {
            return Either.left("One or more providers in input json contains non-null id property");
        }
        SamlIdentityProviderDTO.Converter converter = new SamlIdentityProviderDTO.Converter();
        List<SamlIdentityProviderDTO> saved = providers.stream()
                .map(converter::toEntity)
                .map(this::removeId) // assure that user won't accidentally update existing provider
                .map(idpRepository::save)
                .map(converter::convert)
                .collect(Collectors.toList());
        return Either.right(saved);
    }

    @SuperuserRequired
    public Either<String, SamlIdentityProviderDTO> update(SamlIdentityProviderDTO updateDto) {
        if (!notNull(updateDto::getId, updateDto::getEntityId, updateDto::getMetadataUrl, updateDto::getDisplayName)) {
            return Either.left("All properties (id, entityId, metadataUrl, displayName) must be filled!");
        }
        if (!idpRepository.findById(updateDto.getId()).isPresent()) {
            return Either.left("Cannot found provider with id " + updateDto.getId());
        }
        SamlIdentityProvider updated = idpRepository.saveAndFlush(converter.toEntity(updateDto));
        return Either.right(converter.convert(updated));
    }

    @SuperuserRequired
    public Either<String, SamlIdentityProviderDTO> delete(Long id) {
        Optional<SamlIdentityProvider> existing = idpRepository.findById(id);
        if (!existing.isPresent()) {
            return Either.left("Provider with id " + id + " was not found");
        }
        idpRepository.deleteById(id);
        return Either.right(converter.convert(existing.get()));
    }

    // -------------------- PRIVATE --------------------

    private boolean notNull(Supplier<?>... suppliers) {
        return Arrays.stream(suppliers).allMatch(s -> s.get() != null);
    }

    private SamlIdentityProvider removeId(SamlIdentityProvider provider) {
        provider.setId(null);
        return provider;
    }
}
