package nva.commons.apigateway;

import static java.util.Optional.ofNullable;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import nva.commons.apigateway.exceptions.InvalidAccessRightException;

public enum AccessRight {

    USER("USER"),// pseudo access-right to indicate the customer in cognito groups
    // Old Access Rights:

    /**
     * @deprecated Use MANAGE_PUBLISHING_REQUEST instead.
     */
    @Deprecated(forRemoval = true)
    PUBLISH_FILES("PUBLISH_FILES"),

    /**
     * @deprecated Use MANAGE_PUBLISHING_REQUEST instead.
     */
    @Deprecated(forRemoval = true)
    PUBLISH_METADATA("PUBLISH_METADATA"),

    /**
     * @deprecated Use MANAGE_DEGREE instead.
     */
    @Deprecated(forRemoval = true)
    PUBLISH_DEGREE("PUBLISH_DEGREE"),

    /**
     * @deprecated Use MANAGE_DEGREE_EMBARGO instead.
     */
    @Deprecated(forRemoval = true)
    PUBLISH_DEGREE_EMBARGO_READ("PUBLISH_DEGREE_EMBARGO_READ"),

    /**
     * @deprecated Use MANAGE_IMPORT instead.
     */
    @Deprecated(forRemoval = true)
    PROCESS_IMPORT_CANDIDATE("PROCESS_IMPORT_CANDIDATE"),

    /**
     * @deprecated Use MANAGE_DOI instead.
     */
    @Deprecated(forRemoval = true)
    APPROVE_DOI_REQUEST("APPROVE_DOI_REQUEST"),

    /**
     * @deprecated Use MANAGE_DOI instead.
     */
    @Deprecated(forRemoval = true)
    REJECT_DOI_REQUEST("REJECT_DOI_REQUEST"),

    /**
     * @deprecated Use MANAGE_DOI instead.
     */
    @Deprecated(forRemoval = true)
    READ_DOI_REQUEST("READ_DOI_REQUEST"),

    /**
     * @deprecated Use MANAGE_PUBLISHING_REQUEST instead.
     */
    @Deprecated(forRemoval = true)
    APPROVE_PUBLISH_REQUEST("APPROVE_PUBLISH_REQUEST"),

    /**
     * @deprecated Use MANAGE_RESOURCES_STANDARD instead.
     */
    @Deprecated(forRemoval = true)
    EDIT_OWN_INSTITUTION_RESOURCES("EDIT_OWN_INSTITUTION_RESOURCES"),

    /**
     * @deprecated Use MANAGE_RESOURCES_ALL instead.
     */
    @Deprecated(forRemoval = true)
    EDIT_ALL_NON_DEGREE_RESOURCES("EDIT_ALL_NON_DEGREE_RESOURCES"),

    /**
     * @deprecated Use MANAGE_OWN_AFFILIATION instead.
     */
    @Deprecated(forRemoval = true)
    EDIT_OWN_INSTITUTION_USERS("EDIT_OWN_INSTITUTION_USERS"),

    /**
     * @deprecated Never assigned a Role. Will be deleted
     */
    @Deprecated(forRemoval = true)
    EDIT_OWN_INSTITUTION_PROJECTS("EDIT_OWN_INSTITUTION_PROJECTS"),

    /**
     * @deprecated Use MANAGE_OWN_AFFILIATION instead.
     */
    @Deprecated(forRemoval = true)
    EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW("EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW"),

    /**
     * @deprecated Use MANAGE_CUSTOMERS instead.
     */
    @Deprecated(forRemoval = true)
    ADMINISTRATE_APPLICATION("ADMINISTRATE_APPLICATION"),

    /**
     * @deprecated Use MANAGE_OWN_RESOURCES instead.
     */
    @Deprecated(forRemoval = true)
    MANAGE_OWN_PROJECTS("MANAGE_OWN_PROJECTS"),

    /**
     * @deprecated Use MANAGE_NVI instead.
     */
    @Deprecated(forRemoval = true)
    MANAGE_NVI_PERIODS("MANAGE_NVI_PERIODS"),

    /**
     * @deprecated Use MANAGE_NVI_CANDIDATES instead.
     */
    @Deprecated(forRemoval = true)
    MANAGE_NVI_CANDIDATE("MANAGE_NVI_CANDIDATE"),

    // New Access Rights:

    MANAGE_PUBLISHING_REQUESTS("publishing"),
    MANAGE_DEGREE("degree"),
    MANAGE_DEGREE_EMBARGO("degree-embargo"),
    MANAGE_IMPORT("import"),
    MANAGE_DOI("doi"),
    MANAGE_RESOURCES_STANDARD("resource"),
    MANAGE_RESOURCES_ALL("resource-all"),
    MANAGE_OWN_AFFILIATION("own-affiliation"),
    MANAGE_CUSTOMERS("customers"),
    MANAGE_OWN_RESOURCES("resources"),
    MANAGE_NVI("nvi-admin"),
    MANAGE_NVI_CANDIDATES("nvi-candidates"),
    MANAGE_ALL_PROJECTS("manage-projects"),
    MANAGE_DOI_AGENT("doi-agent"),
    SUPPORT("support"),
    MANAGE_KBS("kbs"),
    ACT_AS("act-as"),
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
