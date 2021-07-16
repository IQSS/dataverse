package edu.harvard.iq.dataverse.search.ror;

import com.google.common.collect.ImmutableList;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.search.RorSolrClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class RorSolrDataFinderIT extends WebappArquillianDeployment {

    @Inject
    private RorSolrDataFinder rorSolrDataFinder;

    @Inject
    @RorSolrClient
    private SolrClient solrClient;

    @Before
    public void setUp() throws IOException, SolrServerException {
        solrClient.deleteByQuery("*:*");
        solrClient.commit();
    }
    
    @Test
    public void findRorData_should_find_by_query() throws SolrServerException, IOException {
        // given
        final RorDto rorSolr = new RorDto()
                .setRorId("03wbkx358")
                .setRorUrl("https://ror.org/03wbkx358")
                .setName("Federal Office of Meteorology and Climatology")
                .setCountryName("Switzerland")
                .setCountryCode("CH")
                .setCity("Zurich")
                .setWebsite("http://www.meteoswiss.admin.ch/home.html?tab=overview")
                .setNameAliases(ImmutableList.of(
                        "Bundesamt für Meteorologie und Klimatologie",
                        "Uffizi Federal per Meteorologia e Climatologia"))
                .setAcronyms(ImmutableList.of("MZA", "SMA"))
                .setLabels(ImmutableList.of(
                        "MétéoSuisse",
                        "MeteoSvizzera",
                        "MeteoSchweiz"));

        solrClient.addBean(rorSolr);
        solrClient.commit();

        // when & then
        assertThat(rorSolrDataFinder.findRorData("03wbkx358", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("03wb", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("https://ror.org/03wbkx358", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("https://ror.org/03wb", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Switzerlan", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("ch", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Zuric", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Office of Meteorology", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Fed Off Meteor", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Uffizi", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("sma", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Météosuisse", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Météosuiss", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Météosuis", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Météosui", 1)).hasSize(1);
        assertThat(rorSolrDataFinder.findRorData("Météosu", 1)).hasSize(1);
    }
    
    @Test
    public void findRorData_should_return_correctly_filled_response() throws SolrServerException, IOException {
        //given
        final RorDto rorSolr = new RorDto()
                .setRorId("testRor")
                .setRorUrl("https://ror.org/testRor")
                .setName("testName")
                .setCountryName("Poland")
                .setCountryCode("PL")
                .setNameAliases(ImmutableList.of("alias"))
                .setAcronyms(ImmutableList.of("acronym"))
                .setLabels(ImmutableList.of("label"));

        solrClient.addBean(rorSolr);
        solrClient.commit();

        //when
        final List<RorDto> queryResponse = rorSolrDataFinder.findRorData("", 5);

        //then
        assertThat(queryResponse.size()).isEqualTo(1);
        assertThat(queryResponse.get(0).getRorUrl()).isEqualTo("https://ror.org/testRor");
        assertThat(queryResponse.get(0).getName()).isEqualTo("testName");
        assertThat(queryResponse.get(0).getCountryName()).isEqualTo("Poland");
        assertThat(queryResponse.get(0).getCountryCode()).isEqualTo("PL");
        assertThat(queryResponse.get(0).getNameAliases()).containsOnly("alias");
        assertThat(queryResponse.get(0).getAcronyms()).containsOnly("acronym");
        assertThat(queryResponse.get(0).getLabels()).containsOnly("label");

    }
}
