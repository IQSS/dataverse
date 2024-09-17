package edu.harvard.iq.dataverse.harvest.client;

import com.google.gson.Gson;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DataciteHarvesterParamsTest {

    @Test
    public void parseJson__empty() {
        // given
        String json = "{}";

        // when
        DataciteHarvesterParams params = new Gson().fromJson(json, DataciteHarvesterParams.class);

        // then
        assertThat(params.getDoiImport()).isEmpty();
    }

    @Test
    public void parseJson__emptyArray() {
        // given
        String json = "{ \"doiImport\": [] }";

        // when
        DataciteHarvesterParams params = new Gson().fromJson(json, DataciteHarvesterParams.class);

        // then
        assertThat(params.getDoiImport()).isEmpty();
    }

    @Test
    public void parseJson__array_of_doi_parts() {
        // given
        String json = "{ \"doiImport\": [ {\"authority\": \"10.5072\", \"id\": \"FK2/BYM3IW\"}, {\"authority\": \"1902.1\", \"id\": \"111012\"} ] }";

        // when
        DataciteHarvesterParams params = new Gson().fromJson(json, DataciteHarvesterParams.class);

        // then
        assertThat(params.getDoiImport()).hasSize(2);
    }

    @Test
    public void parseJson__class() {
        // given
        String json = "{ \"doiImport\": [ {\"authority\": \"10.5072\", \"id\": \"FK2/BYM3IW\"}, {\"authority\": \"1902.1\", \"id\": \"111012\"} ] }";

        // when
        HarvesterParams params = new Gson().fromJson(json, getParams());

        // then
        assertThat(params).isInstanceOf(DataciteHarvesterParams.class);
    }

    private Class<HarvesterParams> getParams() {
        return getSpecific();
    }

    private <T extends HarvesterParams> Class<T> getSpecific() {
        return (Class<T>) DataciteHarvesterParams.class;
    }
}
