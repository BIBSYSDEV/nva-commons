package nva.commons.apigateway;

import com.google.common.net.MediaType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MediaTypesTest {

    @Test
    public void test() {
        assertThat(MediaTypes.APPLICATION_JSON_LD, notNullValue());
        assertThat(MediaTypes.APPLICATION_JSON_LD, is(instanceOf(MediaType.class)));
    }
}
