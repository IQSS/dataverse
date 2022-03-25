package edu.harvard.iq.dataverse.authorization.providers.saml;

import edu.harvard.iq.dataverse.api.dto.SamlIdentityProviderDTO;
import edu.harvard.iq.dataverse.persistence.StubJpaPersistence;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProvider;
import edu.harvard.iq.dataverse.persistence.user.SamlIdentityProviderRepository;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;


class SamlIdpManagementServiceTest {

    private SamlIdentityProviderRepository repository;

    private SamlIdpManagementService service;

    @BeforeEach
    void setUp() {
        repository = new StubJpaPersistence().stub(SamlIdentityProviderRepository.class);
        List<SamlIdentityProvider> testData = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> new SamlIdentityProvider(null, "EntityId" + i, "MetadataUrl" + i, "DisplayName" + i))
                .collect(Collectors.toList());
        testData.forEach(repository::save);
        service = new SamlIdpManagementService(repository);
    }

    // -------------------- TESTS --------------------

    @Test
    void listAll() {
        // when
        List<SamlIdentityProviderDTO> dtos = service.listAll();

        // then
        assertThat(dtos)
                .extracting(SamlIdentityProviderDTO::getId, SamlIdentityProviderDTO::getEntityId,
                        SamlIdentityProviderDTO::getMetadataUrl, SamlIdentityProviderDTO::getDisplayName)
                .containsExactlyInAnyOrder(
                        tuple(1L, "EntityId1", "MetadataUrl1", "DisplayName1"),
                        tuple(2L, "EntityId2", "MetadataUrl2", "DisplayName2"),
                        tuple(3L, "EntityId3", "MetadataUrl3", "DisplayName3"));
    }

    @Test
    void listSingle() {
        // when
        Optional<SamlIdentityProviderDTO> result = service.listSingle(3L);

        // then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get())
                .extracting(SamlIdentityProviderDTO::getId, SamlIdentityProviderDTO::getDisplayName)
                .containsExactly(3L, "DisplayName3");
    }

    @Test
    void listSingle__nonExistingId() {
        // when
        Optional<SamlIdentityProviderDTO> result = service.listSingle(5L);

        // then
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void update() {
        // given
        SamlIdentityProviderDTO updateData = new SamlIdentityProviderDTO(1L, "Updated", "Updated", "Updated");

        // when
        Either<String, SamlIdentityProviderDTO> result = service.update(updateData);

        // then
        assertThat(result.isRight()).isTrue();
        assertThat(result.get())
                .extracting(SamlIdentityProviderDTO::getId, SamlIdentityProviderDTO::getEntityId)
                .containsExactly(1L, "Updated");
    }

    @Test
    void update__incompleteData() {
        // given
        SamlIdentityProviderDTO updateData = new SamlIdentityProviderDTO();

        // when
        Either<String, SamlIdentityProviderDTO> result = service.update(updateData);

        // then
        assertThat(result.isRight()).isFalse();
    }

    @Test
    void update__nonexistingEntity() {
        // given
        SamlIdentityProviderDTO updateData = new SamlIdentityProviderDTO(7L, "Updated", "Updated", "Updated");

        // when
        Either<String, SamlIdentityProviderDTO> result = service.update(updateData);

        // then
        assertThat(result.isRight()).isFalse();
    }

    @Test
    void create() {
        // given
        List<SamlIdentityProviderDTO> toCreate = IntStream.rangeClosed(4, 5)
                .mapToObj(i -> new SamlIdentityProviderDTO(
                        null, "EntityId" + i, "MetadataUrl" + i, "DisplayName" + i))
                .collect(Collectors.toList());

        // when
        Either<String, List<SamlIdentityProviderDTO>> result = service.create(toCreate);
        List<SamlIdentityProviderDTO> afterCreate = service.listAll();

        // then
        assertThat(result.isRight()).isTrue();
        assertThat(result.get()).hasSize(2);
        assertThat(afterCreate).extracting(SamlIdentityProviderDTO::getId, SamlIdentityProviderDTO::getMetadataUrl)
                .containsExactlyInAnyOrder(
                        tuple(1L, "MetadataUrl1"),
                        tuple(2L, "MetadataUrl2"),
                        tuple(3L, "MetadataUrl3"),
                        tuple(4L, "MetadataUrl4"),
                        tuple(5L, "MetadataUrl5"));
    }

    @Test
    void create__incompleteData() {
        // given
        List<SamlIdentityProviderDTO> toCreate = IntStream.rangeClosed(4, 5)
                .mapToObj(i -> new SamlIdentityProviderDTO((long) i, null, "MetadataUrl" + i, "DisplayName" + i))
                .collect(Collectors.toList());

        // when
        Either<String, List<SamlIdentityProviderDTO>> result = service.create(toCreate);
        List<SamlIdentityProviderDTO> afterCreate = service.listAll();

        // then
        assertThat(result.isRight()).isFalse();
        assertThat(afterCreate).hasSize(3);
    }

    @Test
    void delete() {
        // when
        Either<String, SamlIdentityProviderDTO> result = service.delete(1L);
        List<SamlIdentityProviderDTO> afterDelete = service.listAll();

        // then
        assertThat(result.isRight()).isTrue();
        assertThat(result.get())
                .extracting(SamlIdentityProviderDTO::getId, SamlIdentityProviderDTO::getDisplayName)
                .containsExactly(1L, "DisplayName1");
        assertThat(afterDelete)
                .extracting(SamlIdentityProviderDTO::getId)
                .containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void delete__nonexistingEntity() {
        // when
        Either<String, SamlIdentityProviderDTO> result = service.delete(101L);
        List<SamlIdentityProviderDTO> afterDelete = service.listAll();

        // then
        assertThat(result.isRight()).isFalse();
        assertThat(afterDelete)
                .extracting(SamlIdentityProviderDTO::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }
}