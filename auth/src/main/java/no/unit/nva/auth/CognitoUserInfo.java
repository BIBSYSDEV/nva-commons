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
    public static final String FEIDE_ID_CLAIM = "custom:feideId";
    public static final String SELECTED_CUSTOMER_CLAIM = "custom:customerId";
    public static final String ACCESS_RIGHTS_CLAIM = "custom:accessRights";
    public static final String NVA_USERNAME_CLAIM = "custom:nvaUsername";
    public static final String TOP_LEVEL_ORG_CRISTIN_ID_CLAIM = "custom:topOrgCristinId";
    public static final String PERSON_CRISTIN_ID_CLAIM = "custom:cristinId";
    public static final String PERSON_NIN_ID_CLAIM = "custom:nin";
    public static final String PERSON_FEIDE_NIN_ID_CLAIM = "custom:feideIdNin";

    @JsonProperty(FEIDE_ID_CLAIM)
    private String feideId;
    @JsonProperty(SELECTED_CUSTOMER_CLAIM)
    private URI currentCustomer;
    @JsonProperty(ACCESS_RIGHTS_CLAIM)
    private String accessRights;
    @JsonProperty(NVA_USERNAME_CLAIM)
    private String nvaUsername;
    @JsonProperty(TOP_LEVEL_ORG_CRISTIN_ID_CLAIM)
    private URI topOrgCristinId;
    @JsonProperty(PERSON_CRISTIN_ID_CLAIM)
    private URI personCristinId;
    @JsonProperty(PERSON_NIN_ID_CLAIM)
    private String personNinId;
    @JsonProperty(PERSON_FEIDE_NIN_ID_CLAIM)
    private String personFeideNinId;

    public static Builder builder() {
        return new Builder();
    }

    public static CognitoUserInfo fromString(String json) {
        return JsonConfig.beanFrom(CognitoUserInfo.class, json);
    }

    public URI getTopOrgCristinId() {
        return topOrgCristinId;
    }

    public String getNvaUsername() {
        return nvaUsername;
    }

    public void setNvaUsername(String nvaUsername) {
        this.nvaUsername = nvaUsername;
    }

    public URI getCurrentCustomer() {
        return currentCustomer;
    }

    public void setCurrentCustomer(URI currentCustomer) {
        this.currentCustomer = currentCustomer;
    }

    public String getFeideId() {
        return feideId;
    }

    public void setFeideId(String feideId) {
        this.feideId = feideId;
    }

    public String getAccessRights() {
        return nonNull(accessRights) ? accessRights : EMPTY_STRING;
    }

    public void setAccessRights(String accessRights) {
        this.accessRights = accessRights;
    }

    private void setTopOrgCristinId(URI topOrgCristinId) {
        this.topOrgCristinId = topOrgCristinId;
    }

    public URI getPersonCristinId() {
        return personCristinId;
    }

    public void setPersonCristinId(URI personCristinId) {
        this.personCristinId = personCristinId;
    }

    public String getPersonNinId() {
        return personNinId;
    }

    public void setPersonNinId(String personNinId) {
        this.personNinId = personNinId;
    }

    public String getPersonFeideNinId() {
        return personFeideNinId;
    }

    public void setPersonFeideNinId(String personFeideNinId) {
        this.personFeideNinId = personFeideNinId;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getFeideId(), getCurrentCustomer(), getAccessRights(), getNvaUsername(),
                            getTopOrgCristinId(),
                            getPersonCristinId(), getPersonNinId(), getPersonFeideNinId());
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
               && Objects.equals(getNvaUsername(), that.getNvaUsername())
               && Objects.equals(getTopOrgCristinId(), that.getTopOrgCristinId())
               && Objects.equals(getPersonCristinId(), that.getPersonCristinId())
               && Objects.equals(getPersonNinId(), that.getPersonNinId())
               && Objects.equals(getPersonFeideNinId(), that.getPersonFeideNinId());
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

        public Builder withTopOrgCristinId(URI topOrgCristinId) {
            cognitoUserInfo.setTopOrgCristinId(topOrgCristinId);
            return this;
        }

        public Builder withPersonCristinId(URI personCristinId) {
            cognitoUserInfo.setPersonCristinId(personCristinId);
            return this;
        }

        public Builder withPersonNinId(String personNinId) {
            cognitoUserInfo.setPersonNinId(personNinId);
            return this;
        }

        public Builder withPersonFeideNinId(String personFeideNinId) {
            cognitoUserInfo.setPersonFeideNinId(personFeideNinId);
            return this;
        }
    }
}
