package nva.commons.dynamodb;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ItemMapperTest {

    public static final String ATTRIBUTE_VALUE_MAP_JSON = "src/test/resources/attribute_value_map.json";

    @Test
    public void toJsonNodeReturnsJsonOnAttributeValueMap() throws IOException {
        var file = new File(ATTRIBUTE_VALUE_MAP_JSON);
        var map = objectMapper.readValue(file, HashMap.class);
        var item = Item.fromMap(map);
        var attributeValueMap = ItemUtils.toAttributeValues(item);
        var json = ItemMapper.toJson(attributeValueMap);

        assertThat(json, is(notNullValue()));
    }
}
