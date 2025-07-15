package no.unit.nva.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RandomPortNumberTest {

    private RandomPortNumber port;

    @BeforeEach
    void setUp() {
        port = RandomPortNumber.newPort();
    }

    @AfterEach
    void tearDown() {
        port.close();
    }

    @Test
    void shouldReturnRandomPortNumber() {
        var portNumber = port.number();
        assertThat(portNumber, is(not(nullValue())));
    }
}
