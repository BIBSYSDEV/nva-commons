package no.unit.nva.auth;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import nva.commons.core.JacocoGenerated;

public class UserInfo {

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

    public static UserInfo fromString(String json) {
        return JsonConfig.beanFrom(UserInfo.class, json);
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
        if (!(o instanceof UserInfo)) {
            return false;
        }
        UserInfo userInfo = (UserInfo) o;
        return Objects.equals(getFeideId(), userInfo.getFeideId()) && Objects.equals(
            getCurrentCustomer(), userInfo.getCurrentCustomer()) && Objects.equals(getAccessRights(),
                                                                                   userInfo.getAccessRights());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getFeideId(), getCurrentCustomer(), getAccessRights());
    }

    public static final class Builder {

        private final UserInfo userInfo;

        private Builder() {
            userInfo = new UserInfo();
        }

        public Builder withFeideId(String feideId) {
            userInfo.setFeideId(feideId);
            return this;
        }

        public Builder withCurrentCustomer(URI currentCustomer) {
            userInfo.setCurrentCustomer(currentCustomer);
            return this;
        }

        public UserInfo build() {
            return userInfo;
        }

        public Builder withAccessRights(Set<String> accessRights) {
            userInfo.setAccessRights(String.join(ELEMENTS_DELIMITER, accessRights));
            return this;
        }
    }
}
