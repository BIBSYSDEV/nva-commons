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
    private String accessRights;

    public static Builder builder() {
        return new Builder();
    }

    public static CognitoUserInfo fromString(String json) {
        return JsonConfig.beanFrom(CognitoUserInfo.class, json);
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
    public void setAccessRights(String accessRights) {
        this.accessRights = accessRights;
    }

    @JacocoGenerated
    public String getAccessRights() {
        return nonNull(accessRights) ? accessRights : EMPTY_STRING;
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
        CognitoUserInfo cognitoUserInfo = (CognitoUserInfo) o;
        return Objects.equals(getFeideId(), cognitoUserInfo.getFeideId()) && Objects.equals(
            getCurrentCustomer(), cognitoUserInfo.getCurrentCustomer()) && Objects.equals(getAccessRights(),
                                                                                          cognitoUserInfo.getAccessRights());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getFeideId(), getCurrentCustomer(), getAccessRights());
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
    }
}
