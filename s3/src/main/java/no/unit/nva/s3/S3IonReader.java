package no.unit.nva.s3;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import nva.commons.core.attempt.Try;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonTextWriterBuilder;

public final class S3IonReader {

    private S3IonReader() {

    }

    public static Stream<JsonNode> extractJsonNodesFromIonContent(InputStream content) {
        return
            contentToLines(content)
                .map(attempt(S3IonReader::toJsonObjectsString))
                .map(attempt -> attempt.map(S3IonReader::toJsonNode))
                .map(Try::orElseThrow);
    }

    private static JsonNode toJsonNode(String jsonString) {
        return attempt(() -> dtoObjectMapper.readTree(jsonString)).orElseThrow();
    }

    private static Stream<String> contentToLines(InputStream content) {
        return new BufferedReader(new InputStreamReader(content)).lines();
    }

    private static String toJsonObjectsString(String ion) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (IonWriter writer = createIonToJsonTransformer(stringBuilder)) {
            rewrite(ion, writer);
        }
        return stringBuilder.toString();
    }

    private static IonWriter createIonToJsonTransformer(StringBuilder stringBuilder) {
        return IonTextWriterBuilder.json().withCharset(StandardCharsets.UTF_8).build(stringBuilder);
    }

    private static void rewrite(String textIon, IonWriter writer) throws IOException {
        try (IonReader reader = IonReaderBuilder.standard().build(textIon)) {
            writer.writeValues(reader);
        }
    }
}
