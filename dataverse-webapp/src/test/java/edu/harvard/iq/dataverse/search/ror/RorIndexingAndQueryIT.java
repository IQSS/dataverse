package edu.harvard.iq.dataverse.search.ror;

import com.google.common.collect.ImmutableList;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.List;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class RorIndexingAndQueryIT extends WebappArquillianDeployment {

    @Inject
    private RorSolrDataFinder rorSolrDataFinder;

    @Inject
    private RorIndexingService rorIndexingService;

    @Test
    public void indexAndSearchForCollection() {
        //given
        String rorId = "testRor";
        String name = "testName";
        String countryName = "Poland";
        String countryCode = "PL";
        final ImmutableList<String> aliases = ImmutableList.of("alias");
        final ImmutableList<String> acronyms = ImmutableList.of("acronym");
        final ImmutableList<String> labels = ImmutableList.of("label");

        final RorDto rorData = new RorDto(rorId, name, countryName, countryCode, "","", aliases, acronyms, labels);

        //when
        rorIndexingService.indexRorRecord(rorData);
        final List<RorDto> queryResponse = rorSolrDataFinder.findRorData("test");

        //then
        Assertions.assertThat(queryResponse.size()).isEqualTo(1);
        Assertions.assertThat(queryResponse.get(0).getName()).isEqualTo(name);
        Assertions.assertThat(queryResponse.get(0).getCountryName()).isEqualTo(countryName);
        Assertions.assertThat(queryResponse.get(0).getCountryCode()).isEqualTo(countryCode);
        Assertions.assertThat(queryResponse.get(0).getNameAliases().iterator().next()).isEqualTo("alias");
        Assertions.assertThat(queryResponse.get(0).getAcronyms().iterator().next()).isEqualTo("acronym");
        Assertions.assertThat(queryResponse.get(0).getLabels().iterator().next()).isEqualTo("label");

    }
}
