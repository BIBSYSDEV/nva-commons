package nva.commons.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ItemMapperTest {

    public static final String EXPECTED_JSON = "src/test/resources/expected.json";


    @Test
    public void toJsonNodeReturnsJsonOnAttributeValueMap() throws IOException {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put("list", new AttributeValue().withL());
        attributeValueMap.put("string", new AttributeValue().withS("value"));
        attributeValueMap.put("emptyString", new AttributeValue().withS(""));
        attributeValueMap.put("nullString", new AttributeValue().withS(null));

        JsonNode actual = ItemMapper.toJsonNode(attributeValueMap);

        var expected = objectMapper.readTree(new File(EXPECTED_JSON));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void toJsonNodeFromEventReturnsJsonOnEventAttributeValueMap() throws IOException {
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


        JsonNode actual = ItemMapper.toJsonNodeFromEvent(attributeValueMap);

        var expected = objectMapper.readTree(new File(EXPECTED_JSON));
        assertThat(actual, is(equalTo(expected)));
    }
}
