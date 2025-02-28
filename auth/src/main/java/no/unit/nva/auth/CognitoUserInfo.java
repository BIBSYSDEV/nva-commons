package no.unit.nva.auth;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD")
public class CognitoUserInfo {

    public static final String ELEMENTS_DELIMITER = ",";
    public static final String EMPTY_STRING = "";
    public static final String PERSON_FEIDE_ID_CLAIM = "custom:feideId";
    public static final String SELECTED_CUSTOMER_CLAIM = "custom:customerId";
    public static final String ACCESS_RIGHTS_CLAIM = "custom:accessRights";
    public static final String USER_NAME_CLAIM = "custom:nvaUsername";
    public static final String CUSTOMER_ID_CLAIM = "custom:customerId";
    public static final String TOP_LEVEL_ORG_CRISTIN_ID_CLAIM = "custom:topOrgCristinId";
    public static final String PERSON_CRISTIN_ID_CLAIM = "custom:cristinId";
    public static final String PERSON_NIN_CLAIM = "custom:nin";
    public static final String PERSON_FEIDE_NIN_CLAIM = "custom:feideIdNin";
    public static final String ROLES = "custom:roles";
    public static final String SUB = "sub";
    public static final String PERSON_AFFILIATION_CLAIM = "custom:personAffiliation";
    public static final String ALLOWED_CUSTOMERS = "custom:allowedCustomers";
    public static final String COGNITO_USER_NAME = "username";
    public static final String VIEWING_SCOPE_INCLUDED_CLAIM = "custom:viewingScopeIncluded";
    public static final String VIEWING_SCOPE_EXCLUDED_CLAIM = "custom:viewingScopeExcluded";

    @JsonProperty(PERSON_FEIDE_ID_CLAIM)
    private String feideId;
    @JsonProperty(SELECTED_CUSTOMER_CLAIM)
    private URI currentCustomer;
    @JsonProperty(ACCESS_RIGHTS_CLAIM)
    private String accessRights;
    @JsonProperty(USER_NAME_CLAIM)
    private String userName;
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
    @JsonProperty(PERSON_AFFILIATION_CLAIM)
    private URI personAffiliation;
    @JsonProperty(ALLOWED_CUSTOMERS)
    private String allowedCustomers;
    @JsonProperty(COGNITO_USER_NAME)
    private String cognitoUsername;
    @JsonProperty(VIEWING_SCOPE_INCLUDED_CLAIM)
    private String viewingScopeIncluded;
    @JsonProperty(VIEWING_SCOPE_EXCLUDED_CLAIM)
    private String viewingScopeExcluded;

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

    public URI getPersonAffiliation() {
        return personAffiliation;
    }

    public void setPersonAffiliation(URI personAffiliation) {
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public String getViewingScopeIncluded() {
        return viewingScopeIncluded;
    }

    public void setViewingScopeIncluded(String viewingScopeIncluded) {
        this.viewingScopeIncluded = viewingScopeIncluded;
    }

    public String getViewingScopeExcluded() {
        return viewingScopeExcluded;
    }

    public void setViewingScopeExcluded(String viewingScopeExcluded) {
        this.viewingScopeExcluded = viewingScopeExcluded;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getFeideId(),
                            getCurrentCustomer(),
                            getAccessRights(),
                            getUserName(),
                            getTopOrgCristinId(),
                            getPersonCristinId(),
                            getPersonAffiliation(),
                            getPersonNin(),
                            getViewingScopeIncluded(),
                            getViewingScopeExcluded());
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
               && Objects.equals(getUserName(), that.getUserName())
               && Objects.equals(getTopOrgCristinId(), that.getTopOrgCristinId())
               && Objects.equals(getPersonCristinId(), that.getPersonCristinId())
               && Objects.equals(getPersonAffiliation(), that.getPersonAffiliation())
               && Objects.equals(getPersonNin(), that.getPersonNin())
               && Objects.equals(getFeideId(), that.getFeideId())
               && Objects.equals(getViewingScopeIncluded(), that.getViewingScopeIncluded())
               && Objects.equals(getViewingScopeExcluded(), that.getViewingScopeExcluded());
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

        public Builder withUserName(String userName) {
            cognitoUserInfo.setUserName(userName);
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

        public Builder withPersonAffiliation(URI personAffiliation) {
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

        public Builder withViewingScopeIncluded(String viewingScopeIncluded) {
            cognitoUserInfo.setViewingScopeIncluded(viewingScopeIncluded);
            return this;
        }

        public Builder withViewingScopeExcluded(String viewingScopeExcluded) {
            cognitoUserInfo.setViewingScopeExcluded(viewingScopeExcluded);
            return this;
        }

        public CognitoUserInfo build() {
            return cognitoUserInfo;
        }
    }
}
