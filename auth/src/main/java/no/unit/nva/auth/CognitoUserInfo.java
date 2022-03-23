package no.unit.nva.auth;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import nva.commons.core.JacocoGenerated;

public class CognitoUserInfo {

    public static final String ELEMENTS_DELIMITER = ",";
    public static final String EMPTY_STRING = "";
    @JsonProperty("custom:feideId")
    private String feideId;
    @JsonProperty("custom:currentCustomer")
    private URI currentCustomer;
    @JsonProperty("custom:accessRights")
    private String accessRights;
    @JsonProperty("custom:nvaUsername")
    private String nvaUsername;

    public static Builder builder() {
        return new Builder();
    }

    public static CognitoUserInfo fromString(String json) {
        return JsonConfig.beanFrom(CognitoUserInfo.class, json);
    }

    public String getNvaUsername() {
        return nvaUsername;
    }

    public void setNvaUsername(String nvaUsername) {
        this.nvaUsername = nvaUsername;
    }

    @JacocoGenerated
    public URI getCurrentCustomer() {
        return currentCustomer;
    }

    @JacocoGenerated
    public void setCurrentCustomer(URI currentCustomer) {
        this.currentCustomer = currentCustomer;
    }

    @JacocoGenerated
    public String getFeideId() {
        return feideId;
    }

    @JacocoGenerated
    public void setFeideId(String feideId) {
        this.feideId = feideId;
    }

    @JacocoGenerated
    public String getAccessRights() {
        return nonNull(accessRights) ? accessRights : EMPTY_STRING;
    }

    @JacocoGenerated
    public void setAccessRights(String accessRights) {
        this.accessRights = accessRights;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getFeideId(), getCurrentCustomer(), getAccessRights(), getNvaUsername());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CognitoUserInfo)) {
            return false;
        }
        CognitoUserInfo that = (CognitoUserInfo) o;
        return Objects.equals(getFeideId(), that.getFeideId())
               && Objects.equals(getCurrentCustomer(), that.getCurrentCustomer())
               && Objects.equals(getAccessRights(), that.getAccessRights())
               && Objects.equals(getNvaUsername(), that.getNvaUsername());
    }

    public static final class Builder {

        private final CognitoUserInfo cognitoUserInfo;

        private Builder() {
            cognitoUserInfo = new CognitoUserInfo();
        }

        public Builder withFeideId(String feideId) {
            cognitoUserInfo.setFeideId(feideId);
            return this;
        }

        public Builder withCurrentCustomer(URI currentCustomer) {
            cognitoUserInfo.setCurrentCustomer(currentCustomer);
            return this;
        }

        public CognitoUserInfo build() {
            return cognitoUserInfo;
        }

        public Builder withAccessRights(Set<String> accessRights) {
            if (nonNull(accessRights)) {
                cognitoUserInfo.setAccessRights(String.join(ELEMENTS_DELIMITER, accessRights));
            }

            return this;
        }

        public Builder withNvaUsername(String nvaUsername) {
            cognitoUserInfo.setNvaUsername(nvaUsername);
            return this;
        }
    }
}
