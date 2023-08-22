package nva.commons.apigateway;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;
import nva.commons.apigateway.exceptions.InvalidAccessRightException;

public enum AccessRight {

    USER,// pseudo access-right to indicate the customer in cognito groups
    PUBLISH_FILES,
    PUBLISH_METADATA,
    PUBLISH_DEGREE,
    PUBLISH_DEGREE_EMBARGO_READ,
    PROCESS_IMPORT_CANDIDATE,
    APPROVE_DOI_REQUEST,
    REJECT_DOI_REQUEST,
    READ_DOI_REQUEST,
    APPROVE_PUBLISH_REQUEST,
    EDIT_OWN_INSTITUTION_RESOURCES,
    EDIT_ALL_NON_DEGREE_RESOURCES,
    EDIT_OWN_INSTITUTION_USERS,
    EDIT_OWN_INSTITUTION_PROJECTS,
    EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW,
    ADMINISTRATE_APPLICATION,
    MANAGE_OWN_PROJECTS,
    MANAGE_NVI_PERIODS;

    /**
     * Creates an AccessRight instance from a string (case insensitive).
     *
     * @param accessRight string representation of access right
     * @return an AccessRight instance.
     */
    @JsonCreator
    public static AccessRight fromString(String accessRight) {
        var upperCaseAccessRight = toUpper(accessRight);
        return
            attempt(() -> AccessRight.valueOf(upperCaseAccessRight))
                .orElseThrow(new InvalidAccessRightException(accessRight));
    }

    private static String toUpper(String accessRightString) {
        return accessRightString.toUpperCase(Locale.getDefault());
    }
}
