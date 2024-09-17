package edu.harvard.iq.dataverse.harvest.client;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HarvesterParamsTest {

    private final Map<String, Class<?>> paramClasses = ImmutableMap.<String, Class<?>>builder()
            .put("Test1", HarvesterParamsTest1.class)
            .put("Test2", HarvesterParamsTest2.class)
            .put("Empty", HarvesterParams.EmptyHarvesterParams.class)
            .build();

    @Test
    public void parseJson() {
        // given
        String json = "{ \"param1Test1\": \"val1\", \"param2Test1\": \"val2\"}";

        // when
        HarvesterParams params = new Gson().fromJson(json, getParams("Test1"));

        // then
        assertThat(params).isInstanceOf(HarvesterParamsTest1.class);
        assertThat(params).asInstanceOf(InstanceOfAssertFactories.type(HarvesterParamsTest1.class))
                .satisfies(paramsTest1 -> {
                    assertThat(paramsTest1.getParam1Test1()).isEqualTo("val1");
                    assertThat(paramsTest1.getParam2Test1()).isEqualTo("val2");
                });
    }

    @Test
    public void parseJson__wrong_class() {
        // given
        String json = "{ \"param1Test1\": \"val1\", \"param2Test1\": \"val2\"}";

        // when
        HarvesterParams params = new Gson().fromJson(json, getParams("Test2"));

        // then
        assertThat(params).isInstanceOf(HarvesterParamsTest2.class);
        assertThat(params).asInstanceOf(InstanceOfAssertFactories.type(HarvesterParamsTest2.class))
                .satisfies(paramsTest2 -> {
                    assertThat(paramsTest2.getParam1Test2()).isNull();
                    assertThat(paramsTest2.getParam2Test2()).isNull();
                });
    }

    @Test
    public void parseJson__emptyJson() {
        // given
        String json = "{}";

        // when
        HarvesterParams params = new Gson().fromJson(json, getParams("Empty"));

        // then
        assertThat(params).isInstanceOf(HarvesterParams.EmptyHarvesterParams.class);
    }

    @Test
    public void parseJson__emptyString() {
        // given
        String json = "";

        // when
        HarvesterParams params = new Gson().fromJson(json, getParams("Empty"));

        // then
        assertThat(params).isNull();
    }

    @Test
    public void parseJson__null() {
        // given
        String json = null;

        // when
        HarvesterParams params = new Gson().fromJson(json, getParams("Empty"));

        // then
        assertThat(params).isNull();
    }

    private Class<HarvesterParams> getParams(String type) {
        return getSpecific(type);
    }

    private <T extends HarvesterParams> Class<T> getSpecific(String type) {
        return (Class<T>) paramClasses.get(type);
    }

    public static class HarvesterParamsTest1 extends HarvesterParams {
        private String param1Test1;
        private String param2Test1;

        public String getParam1Test1() {
            return param1Test1;
        }

        public String getParam2Test1() {
            return param2Test1;
        }
    }

    public static class HarvesterParamsTest2 extends HarvesterParams {
        private String param1Test2;
        private String param2Test2;

        public String getParam1Test2() {
            return param1Test2;
        }

        public String getParam2Test2() {
            return param2Test2;
        }
    }
}
