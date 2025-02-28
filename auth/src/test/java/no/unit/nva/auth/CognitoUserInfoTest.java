package no.unit.nva.auth;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CognitoUserInfoTest {

    @Test
    void builderContainsAllFields() {
        var userInfo = CognitoUserInfo.builder()
                           .withAccessRights(Set.of(randomString(), randomString()))
                           .withFeideId(randomString())
                           .withCurrentCustomer(randomUri())
                           .withUserName(randomString())
                           .withTopOrgCristinId(randomUri())
                           .withPersonCristinId(randomUri())
                           .withPersonNin(randomString())
                           .withCognitoUsername(randomString())
                           .withAllowedCustomers(randomString())
                           .withRoles(randomString())
                           .withPersonAffiliation(randomUri())
                           .withSub(randomString())
                           .withViewingScopeIncluded(randomString())
                           .withViewingScopeExcluded(randomString())
                           .build();
        assertThat(userInfo, doesNotHaveEmptyValues());
    }
}