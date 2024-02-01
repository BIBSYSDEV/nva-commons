package nva.commons.apigateway;

import static java.util.Optional.ofNullable;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import nva.commons.apigateway.exceptions.InvalidAccessRightException;

public enum AccessRight {

    /**
     * Old PUBLISH_FILES and PUBLISH_METADATA and APPROVE_PUBLISH_REQUEST.
     */
    MANAGE_PUBLISHING_REQUESTS("publishing"),

    /**
     * Old PUBLISH_DEGREE
     */
    MANAGE_DEGREE("degree"),

    /**
     * Old PUBLISH_DEGREE_EMBARGO
     */
    MANAGE_DEGREE_EMBARGO("degree-embargo"),

    /**
     * Old PROCESS_IMPORT_CANDIDATE
     */
    MANAGE_IMPORT("import"),

    /**
     * Old APPROVE_DOI_REQUEST, REJECT_DOI_REQUEST, READ_DOI_REQUEST
     */
    MANAGE_DOI("doi"),

    /**
     * Old EDIT_OWN_INSTITUTION_RESOURCES
     */
    MANAGE_RESOURCES_STANDARD("resource"),

    /**
     * Old EDIT_ALL_NON_DEGREE_RESOURCES
     */
    MANAGE_RESOURCES_ALL("resource-all"),

    /**
     * Old EDIT_OWN_INSTITUTION_USERS
     */
    MANAGE_OWN_AFFILIATION("own-affiliation"),

    /**
     * Old ADMINISTRATE_APPLICATION
     */
    MANAGE_CUSTOMERS("customers"),

    MANAGE_OWN_RESOURCES("resources"),

    /**
     * Old MANAGE_NVI_PERIODS
     */
    MANAGE_NVI("nvi-admin"),

    MANAGE_NVI_CANDIDATES("nvi-candidates"),
    MANAGE_ALL_PROJECTS("manage-projects"),
    MANAGE_DOI_AGENT("doi-agent"),
    SUPPORT("support"),
    MANAGE_KBS("kbs"),

    /**
     * Old ADMINISTRATE_APPLICATION
     */
    ACT_AS("act-as"),

    /**
     * Old ADMINISTRATE_APPLICATION
     */
    MANAGE_EXTERNAL_CLIENTS("external-clients");

    private static final Map<String, AccessRight> LOOKUP = Maps.uniqueIndex(
        Arrays.asList(AccessRight.values()),
        AccessRight::getPersistedValue
    );

    private final String persistedValue;

    private String getPersistedValue() {
        return persistedValue;
    }

    AccessRight(String persistedValue) {
        this.persistedValue = persistedValue;
    }

    public String toPersistedString() {
        return persistedValue;
    }

    /**
     * Creates an AccessRight instance from a string which is in the shortened persisted format
     *
     * @param accessRight string representation of access right
     * @return an AccessRight instance.
     */

    public static AccessRight fromPersistedString(String accessRight) {
        return ofNullable(LOOKUP.get(accessRight)).orElseThrow(() -> new InvalidAccessRightException(accessRight));
    }

}
