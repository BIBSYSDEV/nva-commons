package nva.commons.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class IoUtilsTest {

    private static Path RESOURCE = Path.of("sampleFolder", "sampleResource.txt");

    @Test
    public void inputStreamFromResourcesReturnANonEmptyStreamForExistingResource() throws IOException {
        InputStream stream = IoUtils.inputStreamFromResources(RESOURCE);
        String line = new BufferedReader(new InputStreamReader(stream)).readLine();
        assertThat(line, is(not(emptyOrNullString())));
    }

    @Test
    public void stringFromResourcesReturnsTheWhoeContentOfAResourceFileInAString() throws IOException {
        String actual = IoUtils.stringFromResources(RESOURCE);
        String expected = new BufferedReader(new InputStreamReader(IoUtils.inputStreamFromResources(RESOURCE)))
            .lines().collect(Collectors.joining(System.lineSeparator()));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void fileAsStringReturnsFileContentsAsString() throws URISyntaxException, IOException {
        URL file = Thread.currentThread().getContextClassLoader().getResource(RESOURCE.toString());
        Path filePath = Path.of(file.toURI());
        String actual = IoUtils.stringFromFile(filePath);
        String expected = IoUtils.stringFromResources(RESOURCE);
        assertThat(actual, is(equalTo(expected)));
    }
}
