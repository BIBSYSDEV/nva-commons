package nva.commons.apigateway;

import com.google.common.net.MediaType;

public final class MediaTypes {

    public static final String APPLICATION = "application";
    public static final MediaType APPLICATION_JSON_LD = MediaType.create(APPLICATION, "ld+json");
    public static final MediaType APPLICATION_PROBLEM_JSON =
        MediaType.create(APPLICATION, "problem+json");
    public static final MediaType APPLICATION_DATACITE_XML =
        MediaType.create(APPLICATION, "vnd.datacite.datacite+xml");
    public static final MediaType SCHEMA_ORG =
        MediaType.create(APPLICATION, "vnd.schemaorg.ld+json");

    private MediaTypes() {
    }
}
