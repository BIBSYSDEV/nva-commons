package nva.commons.apigateway;

import com.google.common.net.MediaType;

public final class MediaTypes {

    public static final MediaType APPLICATION_JSON_LD = MediaType.create("application", "ld+json");
    public static final MediaType APPLICATION_PROBLEM_JSON = MediaType.create("application", "problem+json");


    private MediaTypes() {
    }

}