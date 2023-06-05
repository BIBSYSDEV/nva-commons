package no.unit.nva.commons.dlq;

import nva.commons.core.Environment;

public final class Configuration {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String DELIVERY_STREAM_NAME = ENVIRONMENT.readEnv("DELIVERY_STREAM_NAME");
    //TODO: Make number of groups configurable.
    public static final int NUMBER_OF_GROUPS = 10;

    private Configuration() {

    }
}
