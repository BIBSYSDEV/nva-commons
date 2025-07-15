package no.unit.nva.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

public record UserDto(@JsonProperty("username") String username,
                      @JsonProperty("institution") URI institution,
                      @JsonProperty("givenName") String givenName,
                      @JsonProperty("familyName") String familyName,
                      @JsonProperty("viewingScope") ViewingScope viewingScope,
                      @JsonProperty("roles") List<Role> roles,
                      @JsonProperty("cristinId") URI cristinId,
                      @JsonProperty("feideIdentifier") String feideIdentifier,
                      @JsonProperty("institutionCristinId") URI institutionCristinId,
                      @JsonProperty("affiliation") URI affiliation,
                      @JsonProperty("type") String type,
                      @JsonProperty("accessRights") List<String> accessRights) implements JsonSerializable {

    public static Builder builder() {
        return new Builder();
    }

    public record ViewingScope(@JsonProperty("type") String type,
                               @JsonProperty("includedUnits") List<URI> includedUnits,
                               List<URI> excludedUnits) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private String type;
            private List<URI> includedUnits;
            private List<URI> excludedUnits;

            private Builder() {
            }

            public Builder withType(String type) {
                this.type = type;
                return this;
            }

            public Builder withIncludedUnits(List<URI> includedUnits) {
                this.includedUnits = includedUnits;
                return this;
            }

            public Builder withExcludedUnits(List<URI> excludedUnits) {
                this.excludedUnits = excludedUnits;
                return this;
            }

            public ViewingScope build() {
                return new ViewingScope(type, includedUnits, excludedUnits);
            }
        }
    }

    public record Role(@JsonProperty("rolename") String rolename,
                       @JsonProperty("accessRights") List<String> accessRights,
                       @JsonProperty("type") String type) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private String rolename;
            private List<String> accessRights;
            private String type;

            private Builder() {
            }

            public Builder withRolename(String rolename) {
                this.rolename = rolename;
                return this;
            }

            public Builder withAccessRights(List<String> accessRights) {
                this.accessRights = accessRights;
                return this;
            }

            public Builder withType(String type) {
                this.type = type;
                return this;
            }

            public Role build() {
                return new Role(rolename, accessRights, type);
            }
        }
    }

    public static final class Builder {

        private String username;
        private URI institution;
        private String givenName;
        private String familyName;
        private ViewingScope viewingScope;
        private List<Role> roles;
        private URI cristinId;
        private String feideIdentifier;
        private URI institutionCristinId;
        private URI affiliation;
        private String type;

        private List<String> accessRights;

        private Builder() {
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withInstitution(URI institution) {
            this.institution = institution;
            return this;
        }

        public Builder withGivenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public Builder withFamilyName(String familyName) {
            this.familyName = familyName;
            return this;
        }

        public Builder withViewingScope(ViewingScope viewingScope) {
            this.viewingScope = viewingScope;
            return this;
        }

        public Builder withRoles(List<Role> roles) {
            this.roles = roles;
            return this;
        }

        public Builder withCristinId(URI cristinId) {
            this.cristinId = cristinId;
            return this;
        }

        public Builder withFeideIdentifier(String feideIdentifier) {
            this.feideIdentifier = feideIdentifier;
            return this;
        }

        public Builder withInstitutionCristinId(URI institutionCristinId) {
            this.institutionCristinId = institutionCristinId;
            return this;
        }

        public Builder withAffiliation(URI affiliation) {
            this.affiliation = affiliation;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withAccessRights(List<String> accessRights) {
            this.accessRights = accessRights;
            return this;
        }

        public UserDto build() {
            return new UserDto(username, institution, givenName, familyName, viewingScope, roles, cristinId,
                               feideIdentifier, institutionCristinId, affiliation, type, accessRights);
        }
    }
}