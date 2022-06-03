package no.unit.nva.auth;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonAlias;
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
    public static final String PERSON_NIN_CLAIM = "custom:nin";
    public static final String PERSON_FEIDE_NIN_CLAIM = "custom:feideIdNin";
    public static final String ROLES = "custom:roles";
    public static final String SUB = "sub";
    public static final String PERSON_AFFILIATION = "custom:personAffiliation";
    public static final String ALLOWED_CUSTOMERS = "custom:allowedCustomers";
    public static final String COGNITO_USERNAME = "username";
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
    @JsonAlias(PERSON_FEIDE_NIN_CLAIM)
    @JsonProperty(PERSON_NIN_CLAIM)
    private String personNin;
    @JsonProperty(ROLES)
    private String roles;
    @JsonProperty(SUB)
    private String sub;
    @JsonProperty(PERSON_AFFILIATION)
    private String personAffiliation;
    @JsonProperty(ALLOWED_CUSTOMERS)
    private String allowedCustomers;
    @JsonProperty(COGNITO_USERNAME)
    private String cognitoUsername;

    public static Builder builder() {
        return new Builder();
    }

    public static CognitoUserInfo fromString(String json) {
        return JsonConfig.beanFrom(CognitoUserInfo.class, json);
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getPersonAffiliation() {
        return personAffiliation;
    }

    public void setPersonAffiliation(String personAffiliation) {
        this.personAffiliation = personAffiliation;
    }

    public String getAllowedCustomers() {
        return allowedCustomers;
    }

    public void setAllowedCustomers(String allowedCustomers) {
        this.allowedCustomers = allowedCustomers;
    }

    public String getCognitoUsername() {
        return cognitoUsername;
    }

    public void setCognitoUsername(String cognitoUsername) {
        this.cognitoUsername = cognitoUsername;
    }

    public URI getTopOrgCristinId() {
        return topOrgCristinId;
    }

    public void setTopOrgCristinId(URI topOrgCristinId) {
        this.topOrgCristinId = topOrgCristinId;
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

    public URI getPersonCristinId() {
        return personCristinId;
    }

    public void setPersonCristinId(URI personCristinId) {
        this.personCristinId = personCristinId;
    }

    public String getPersonNin() {
        return personNin;
    }

    public void setPersonNin(String personNin) {
        this.personNin = personNin;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getFeideId(), getCurrentCustomer(), getAccessRights(), getNvaUsername(),
                            getTopOrgCristinId(),
                            getPersonCristinId(), getPersonNin());
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
               && Objects.equals(getPersonNin(), that.getPersonNin());
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

        public Builder withPersonNin(String personNin) {
            cognitoUserInfo.setPersonNin(personNin);
            return this;
        }

        public Builder withRoles(String roles) {
            cognitoUserInfo.setRoles(roles);
            return this;
        }

        public Builder withSub(String sub) {
            cognitoUserInfo.setSub(sub);
            return this;
        }

        public Builder withPersonAffiliation(String personAffiliation) {
            cognitoUserInfo.setPersonAffiliation(personAffiliation);
            return this;
        }

        public Builder withAllowedCustomers(String allowedCustomers) {
            cognitoUserInfo.setAllowedCustomers(allowedCustomers);
            return this;
        }

        public Builder withCognitoUsername(String cognitoUsername) {
            cognitoUserInfo.setCognitoUsername(cognitoUsername);
            return this;
        }

        public CognitoUserInfo build() {
            return cognitoUserInfo;
        }
    }
}
