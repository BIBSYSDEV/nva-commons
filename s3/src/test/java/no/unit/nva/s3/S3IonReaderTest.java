package no.unit.nva.s3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

class S3IonReaderTest {

    public static final String ION_DATA_FILE = "ion_data.ion.gz";

    @Test
    void ionReaderReadsFileWhereEachLineIsAnIonObject() throws IOException {
        long numberOfLines = dataFileNumberOfLines();
        GZIPInputStream inputStream = new GZIPInputStream(IoUtils.inputStreamFromResources(ION_DATA_FILE));
        long numberOfJsonObjects = S3IonReader.extractJsonNodesFromIonContent(inputStream).count();
        assertThat(numberOfJsonObjects, is(equalTo(numberOfLines)));
    }

    private long dataFileNumberOfLines() throws IOException {
        GZIPInputStream inputStream = new GZIPInputStream(IoUtils.inputStreamFromResources(ION_DATA_FILE));
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines().count();
    }
}