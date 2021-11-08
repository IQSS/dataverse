package edu.harvard.iq.dataverse.harvest.server.xoai;

import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.persistence.harvest.OAISet;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class XsetRepositoryTest {

    @Mock
    private OAISetServiceBean setService;

    @InjectMocks
    private XsetRepository repository;

    // -------------------- TESTS --------------------

    @ParameterizedTest
    @CsvSource({
            "set, true",
            "set2, false",
            "set3, true"
    })
    void exists(String name, boolean exists) {
        // given
        List<OAISet> allSets = Stream.of("set", "set3")
                .map(this::createSetWithName)
                .collect(Collectors.toList());
        Mockito.when(setService.findAllNamedSets()).thenReturn(allSets);

        // when
        boolean result = repository.exists(name);

        // then
        assertThat(result).isEqualTo(exists);
    }

    // -------------------- PRIVATE --------------------

    private OAISet createSetWithName(String name) {
        OAISet set = new OAISet();
        set.setName(name);
        return set;
    }
}