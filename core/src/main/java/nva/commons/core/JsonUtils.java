package nva.commons.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import org.zalando.problem.ProblemModule;

public final class JsonUtils {

    public static final ObjectMapper objectMapperNoEmpty = createJsonParser(Include.NON_EMPTY);
    public static final ObjectMapper objectMapperWithEmpty = createJsonParser(Include.ALWAYS);

    public static final ObjectMapper objectMapper = objectMapperNoEmpty;

    private JsonUtils() {
    }

    private static ObjectMapper createJsonParser(JsonInclude.Include includeEmptyValuesOption) {
        JsonFactory jsonFactory =
            new JsonFactory().configure(Feature.ALLOW_SINGLE_QUOTES, true);

        return new ObjectMapper(jsonFactory)
            .registerModule(new ProblemModule())
            //TODO: JavaTimeModule belongs to an obsolete library, find alternative
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .registerModule(emptyStringAsNullModule())
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // We want date-time format, not unix timestamps
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Ignore null and empty fields
            .setSerializationInclusion(includeEmptyValuesOption);
    }

    private static SimpleModule emptyStringAsNullModule() {
        SimpleModule module = new SimpleModule();
        //Do not optimize the line under. It can fail due to a Oracle JDK bug.
        module.addDeserializer(String.class, new StdDeserializer<String>(String.class) {

            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String result = StringDeserializer.instance.deserialize(p, ctxt);
                if (result == null || result.isEmpty()) {
                    return null;
                }
                return result;
            }
        });

        return module;
    }
}
