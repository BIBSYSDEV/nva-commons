package nva.commons.dynamodb;


import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.amazonaws.util.BinaryUtils.copyAllBytesFrom;

public final class ItemMapper {

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    private ItemMapper() {
    }

    @JacocoGenerated
    public static String toJsonV2(
            Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> attributeValueMapV2)
            throws JsonProcessingException {
        return objectMapper.writeValueAsString(toJsonNodeV2(attributeValueMapV2));
    }

    public static String toJson(
            Map<String, AttributeValue> attributeValueMap)
            throws JsonProcessingException {
        return objectMapper.writeValueAsString(toJsonNode(attributeValueMap));
    }

    @JacocoGenerated
    public static JsonNode toJsonNodeV2(
            Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> attributeValueMapV2)
            throws JsonProcessingException {
        Map<String, AttributeValue> attributeValueMap = fromV2toV1(attributeValueMapV2);
        return toJsonNode(attributeValueMap);
    }

    public static JsonNode toJsonNode(Map<String, AttributeValue> attributeValueMap) {
        var object = toObject(attributeValueMap);
        Item item = Item.fromMap(object);
        var jsonNode = objectMapper.convertValue(item.toJSON(), JsonNode.class);

        return jsonNode;
    }

    private static Map<String, Object> toObject(Map<String, AttributeValue> attributeMap) {
        return toSimpleMapValue(attributeMap);
    }

    @JacocoGenerated
    private static Map<String, AttributeValue> fromV2toV1(
            Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> map)
            throws JsonProcessingException {
        var jsonString = objectMapper.writeValueAsString(map);
        var javaType = objectMapper.getTypeFactory().constructParametricType(Map.class, String.class,
                AttributeValue.class);
        return objectMapper.readValue(jsonString, javaType);
    }

    @JacocoGenerated
    private static <T> Map<String, T> toSimpleMapValue(
            Map<String,AttributeValue> values) {
        if (values == null) {
            return null;
        }
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, T> result = new LinkedHashMap<>(values.size());
        for (Map.Entry<String, AttributeValue> entry : values.entrySet()) {
            T t = toSimpleValue(entry.getValue());
            result.put(entry.getKey(), t);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private  static <T> T toSimpleValue(AttributeValue value) {
        if (value == null) {
            return null;
        }
        if (Boolean.TRUE.equals(value.getNULL())) {
            return null;
        } else if (Boolean.FALSE.equals(value.getNULL())) {
            throw new UnsupportedOperationException("False-NULL is not supported in DynamoDB");
        } else if (value.getBOOL() != null) {
            T t = (T) value.getBOOL();
            return t;
        } else if (value.getS() != null) {
            T t = (T) value.getS();
            return t;
        } else if (value.getN() != null) {
            T t = (T) new BigDecimal(value.getN());
            return t;
        } else if (value.getB() != null) {
            T t = (T) copyAllBytesFrom(value.getB());
            return t;
        } else if (value.getSS() != null) {
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            T t = (T) new LinkedHashSet<>(value.getSS());
            return t;
        } else if (value.getNS() != null) {
            Set<BigDecimal> set = new LinkedHashSet<>(value.getNS().size());
            value.getNS()
                    .stream()
                    .map(BigDecimal::new)
                    .forEach(set::add);
            T t = (T) set;
            return t;
        } else if (value.getBS() != null) {
            Set<byte[]> set = new LinkedHashSet<>(value.getBS().size());
            for (ByteBuffer bb : value.getBS()) {
                set.add(copyAllBytesFrom(bb));
            }
            T t = (T) set;
            return t;
        } else if (value.getL() != null) {
            T t = (T) toSimpleList(value.getL());
            return t;
        } else if (value.getM() != null) {
            T t = (T) toSimpleMapValue(value.getM());
            return t;
        } else {
            return null;
        }
    }

    private static List<Object> toSimpleList(List<AttributeValue> attrValues) {
        if (attrValues == null) {
            return null;
        }
        List<Object> result = new ArrayList<>(attrValues.size());
        for (AttributeValue attrValue : attrValues) {
            Object value = toSimpleValue(attrValue);
            result.add(value);
        }
        return result;
    }

}
