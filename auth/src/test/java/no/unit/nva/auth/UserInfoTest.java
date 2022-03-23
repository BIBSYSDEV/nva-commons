package no.unit.nva.auth;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserInfoTest {

    @Test
    void builderContainsAllFields() {
        var userInfo = CognitoUserInfo.builder()
            .withAccessRights(Set.of(randomString(), randomString()))
            .withFeideId(randomString())
            .withCurrentCustomer(randomUri())
            .withNvaUsername(randomString())
            .build();
        assertThat(userInfo, doesNotHaveEmptyValues());
    }
}