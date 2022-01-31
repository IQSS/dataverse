package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeCitationMetadataBlock;
import static org.assertj.core.api.Assertions.assertThat;

class MetadataBlockWithFieldsDTOCreatorTest {

    private MetadataBlockWithFieldsDTO.Creator creator = new MetadataBlockWithFieldsDTO.Creator();

    // -------------------- TESTS --------------------

    @Test
    public void create() throws JsonProcessingException {
        // given
        MetadataBlock metadataBlock = makeCitationMetadataBlock();

        // when
        MetadataBlockWithFieldsDTO created = creator.create(metadataBlock, Collections.emptyList());

        // then
        assertThat(new ObjectMapper().writeValueAsString(created))
                .isEqualTo(("{'displayName':'Citation Metadata'}")
                        .replaceAll("'", "\""));
    }
}