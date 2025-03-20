package nva.commons.apigateway;

import static no.unit.nva.testutils.RandomDataGenerator.randomAccessRight;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.junit.jupiter.api.Test;

class AccessRightEntryTest {

    @Test
    void toStringReturnsTheSameStringForEquivalentObjects() {
        var accessRight = randomAccessRight();
        var customerId = randomUri();
        var group1 = new AccessRightEntry(accessRight, customerId);
        var group2 = new AccessRightEntry(accessRight, customerId);
        assertThat(group1.toString(), is(equalTo(group2.toString())));
    }

    @Test
    void fromCsv() {
        var accessRight1 = randomAccessRight();
        var customerId1 = randomUri();
        var accessRight2 = randomAccessRight();
        var customerId2 = randomUri();
        var csv = accessRight1.toPersistedString() + "@" + customerId1 + ","
                  + accessRight2.toPersistedString() + "@" + customerId2;
        var group = AccessRightEntry.fromCsv(csv).toList();

        assertThat(group, containsInAnyOrder(
            new AccessRightEntry(accessRight1, customerId1),
            new AccessRightEntry(accessRight2, customerId2)
        ));
    }
}