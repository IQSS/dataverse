package io.gdcc.dvopa;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain
public class DvOpa implements QuarkusApplication {

    public static final String NAME = "dvopa";
    public static final String API_GROUP = NAME + ".gdcc.io";
    public static final String API_VERSION = "v0";
    
    private final Operator operator;
    
    @Inject
    public DvOpa(Operator operator) {
        this.operator = operator;
    }
    
    @Override
    public int run(String... args) throws Exception {
        operator.start();
        Quarkus.waitForExit();
        return 0;
    }
    
    public static void main(String... args) {
        Quarkus.run(DvOpa.class, args);
    }

}
