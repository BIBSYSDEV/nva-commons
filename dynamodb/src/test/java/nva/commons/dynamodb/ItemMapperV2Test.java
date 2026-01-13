package nva.commons.dynamodb;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class ItemMapperV2Test {

    public static final String EXPECTED_JSON = "src/test/resources/expected.json";

    @Test
    void toJsonNodeReturnsJsonOnAttributeValueMap() throws IOException {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("list", AttributeValue.builder().l(List.of()).build());
        attributeValueMap.put("string", AttributeValue.builder().s("value").build());
        attributeValueMap.put("emptyString", AttributeValue.builder().s("").build());
        attributeValueMap.put("nullString", AttributeValue.builder().s(null).build());

        var actual = ItemMapperV2.toJsonNode(attributeValueMap);

        var expected = dtoObjectMapper.readTree(new File(EXPECTED_JSON));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void toJsonNodeFromEventReturnsJsonOnEventAttributeValueMap() throws IOException {
        Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> attributeValueMap
                = new HashMap<>();
        attributeValueMap.put("list",
                new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue().withL());

        attributeValueMap.put("string",
                new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue().withS("value"));
        attributeValueMap.put("emptyString",
                new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue().withS(""));
        attributeValueMap.put("nullString",
                new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue().withS(null));

        var actual = ItemMapperV2.toJsonNodeFromEvent(attributeValueMap);

        var expected = dtoObjectMapper.readTree(new File(EXPECTED_JSON));
        assertThat(actual, is(equalTo(expected)));
    }
}
