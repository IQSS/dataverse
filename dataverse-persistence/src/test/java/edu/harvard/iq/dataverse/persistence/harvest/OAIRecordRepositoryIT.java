package edu.harvard.iq.dataverse.persistence.harvest;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class OAIRecordRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private OAIRecordRepository repository;


    @Before
    public void setUp() {
        repository.save(new OAIRecord("", "global_id_1", Date.from(Instant.parse("2007-12-03T10:15:29.00Z"))));
        repository.save(new OAIRecord("", "global_id_2", Date.from(Instant.parse("2007-12-03T10:15:30.00Z"))));
        repository.save(new OAIRecord("", "global_id_3", Date.from(Instant.parse("2007-12-03T10:15:31.00Z"))));
        repository.save(new OAIRecord("", "global_id_4", Date.from(Instant.parse("2007-12-03T10:15:32.00Z"))));
        repository.save(new OAIRecord("", "global_id_5", Date.from(Instant.parse("2007-12-03T10:15:33.00Z"))));

        repository.save(new OAIRecord("set_name_1", "global_id_1", Date.from(Instant.parse("2007-12-03T10:15:30.00Z"))));
        repository.save(new OAIRecord("set_name_1", "global_id_2", Date.from(Instant.parse("2007-12-03T10:15:31.00Z"))));
        repository.save(new OAIRecord("set_name_1", "global_id_3", Date.from(Instant.parse("2007-12-03T10:15:32.00Z"))));
        repository.save(new OAIRecord("set_name_1", "global_id_4", Date.from(Instant.parse("2007-12-03T10:15:33.00Z"))));

        repository.save(new OAIRecord("set_name_2", "global_id_1", Date.from(Instant.parse("2007-12-03T10:15:30.00Z"))));
        OAIRecord removedRecord = new OAIRecord("set_name_2", "global_id_5", Date.from(Instant.parse("2007-12-03T10:15:31.00Z")));
        removedRecord.setRemoved(true);
        repository.save(removedRecord);
    }

    // -------------------- TESTS --------------------

    @Test
    public void findBySetName() {
        // when
        List<OAIRecord> oaiRecords = repository.findBySetName("set_name_1");
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactlyInAnyOrder(
                tuple("global_id_1", "set_name_1"),
                tuple("global_id_2", "set_name_1"),
                tuple("global_id_3", "set_name_1"),
                tuple("global_id_4", "set_name_1")
            );
    }

    @Test
    public void findBySetNameAndRemoved_non_removed() {
        // when
        List<OAIRecord> oaiRecords = repository.findBySetNameAndRemoved("set_name_2", false);
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactlyInAnyOrder(
                tuple("global_id_1", "set_name_2")
            );
    }

    @Test
    public void findBySetNameAndRemoved_removed() {
        // when
        List<OAIRecord> oaiRecords = repository.findBySetNameAndRemoved("set_name_2", true);
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactlyInAnyOrder(
                tuple("global_id_5", "set_name_2")
            );
    }

    @Test
    public void findByGlobalId() {
        // when
        List<OAIRecord> oaiRecords = repository.findByGlobalId("global_id_1");
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactlyInAnyOrder(
                tuple("global_id_1", ""),
                tuple("global_id_1", "set_name_1"),
                tuple("global_id_1", "set_name_2")
            );
    }

    @Test
    public void findByGlobalIds() {
        // when
        List<OAIRecord> oaiRecords = repository.findByGlobalIds(
                Lists.newArrayList("global_id_1", "global_id_5", "global_id_10"));
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactlyInAnyOrder(
                tuple("global_id_1", ""),
                tuple("global_id_1", "set_name_1"),
                tuple("global_id_1", "set_name_2"),
                tuple("global_id_5", ""),
                tuple("global_id_5", "set_name_2")
            );
    }

    @Test
    public void findBySetNameAndLastUpdateBetween() {
        // when
        List<OAIRecord> oaiRecords = repository.findBySetNameAndLastUpdateBetween(
                "set_name_1",
                Date.from(Instant.parse("2007-12-03T10:15:31.00Z")),
                Date.from(Instant.parse("2007-12-03T10:15:32.00Z")));
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactly(
                tuple("global_id_2", "set_name_1"),
                tuple("global_id_3", "set_name_1")
            );
    }

    @Test
    public void findBySetNameAndLastUpdateBetween_without_lower_limit() {
        // when
        List<OAIRecord> oaiRecords = repository.findBySetNameAndLastUpdateBetween(
                "set_name_1",
                null,
                Date.from(Instant.parse("2007-12-03T10:15:32.00Z")));
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactly(
                tuple("global_id_1", "set_name_1"),
                tuple("global_id_2", "set_name_1"),
                tuple("global_id_3", "set_name_1")
            );
    }

    @Test
    public void findBySetNameAndLastUpdateBetween_without_upper_limit() {
        // when
        List<OAIRecord> oaiRecords = repository.findBySetNameAndLastUpdateBetween(
                "set_name_1",
                Date.from(Instant.parse("2007-12-03T10:15:31.00Z")),
                null);
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactly(
                tuple("global_id_2", "set_name_1"),
                tuple("global_id_3", "set_name_1"),
                tuple("global_id_4", "set_name_1")
            );
    }

    @Test
    public void findBySetNameAndLastUpdateBetween_without_any_limit() {
        // when
        List<OAIRecord> oaiRecords = repository.findBySetNameAndLastUpdateBetween(
                "set_name_1",
                null, null);
        // then
        assertThat(oaiRecords)
            .extracting(OAIRecord::getGlobalId, OAIRecord::getSetName)
            .containsExactly(
                tuple("global_id_1", "set_name_1"),
                tuple("global_id_2", "set_name_1"),
                tuple("global_id_3", "set_name_1"),
                tuple("global_id_4", "set_name_1")
            );
    }

    @Test
    public void deleteBySetName() {
        // when
        repository.deleteBySetName("oai_set_2");
        // then
        assertThat(repository.findAll())
            .extracting(OAIRecord::getSetName)
            .noneMatch(setName -> StringUtils.equals(setName, "oai_set_2"));
    }

}
