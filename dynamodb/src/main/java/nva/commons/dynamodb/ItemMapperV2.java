package nva.commons.dynamodb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Utility class for mapping AWS SDK v2 DynamoDB AttributeValue maps to JSON.
 */
@SuppressWarnings("PMD.GodClass")
public final class ItemMapperV2 {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ItemMapperV2() {
    }

    @JacocoGenerated
    public static String toJsonFromEvent(
            Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue>
                    eventAttributeValueMap) {
        return toJsonNodeFromEvent(eventAttributeValueMap).toString();
    }

    @JacocoGenerated
    public static String toJson(Map<String, AttributeValue> attributeValueMap)
            throws JsonProcessingException {
        return objectMapper.writeValueAsString(toJsonNode(attributeValueMap));
    }

    public static JsonNode toJsonNodeFromEvent(
            Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue>
                    eventAttributeValueMap) {
        var attributeValueMap = fromEvent(eventAttributeValueMap);
        return toJsonNode(attributeValueMap);
    }

    public static JsonNode toJsonNode(Map<String, AttributeValue> attributeValueMap) {
        var object = toObject(attributeValueMap);
        return objectMapper.valueToTree(object);
    }

    private static Map<String, Object> toObject(Map<String, AttributeValue> attributeMap) {
        return toSimpleMapValue(attributeMap);
    }

    private static Map<String, AttributeValue> fromEvent(
            Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> map) {
        if (map == null) {
            return Map.of();
        }
        Map<String, AttributeValue> result = new LinkedHashMap<>();
        for (var entry : map.entrySet()) {
            result.put(entry.getKey(), convertEventAttributeValue(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
    private static AttributeValue convertEventAttributeValue(
            com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue eventValue) {
        if (eventValue == null) {
            return null;
        }
        var builder = AttributeValue.builder();
        if (eventValue.getNULL() != null && eventValue.getNULL()) {
            builder.nul(true);
        } else if (eventValue.getBOOL() != null) {
            builder.bool(eventValue.getBOOL());
        } else if (eventValue.getS() != null) {
            builder.s(eventValue.getS());
        } else if (eventValue.getN() != null) {
            builder.n(eventValue.getN());
        } else if (eventValue.getB() != null) {
            builder.b(SdkBytes.fromByteBuffer(eventValue.getB()));
        } else if (eventValue.getSS() != null && !eventValue.getSS().isEmpty()) {
            builder.ss(eventValue.getSS());
        } else if (eventValue.getNS() != null && !eventValue.getNS().isEmpty()) {
            builder.ns(eventValue.getNS());
        } else if (eventValue.getBS() != null && !eventValue.getBS().isEmpty()) {
            builder.bs(eventValue.getBS().stream().map(SdkBytes::fromByteBuffer).toList());
        } else if (eventValue.getL() != null) {
            builder.l(eventValue.getL().stream().map(ItemMapperV2::convertEventAttributeValue).toList());
        } else if (eventValue.getM() != null) {
            builder.m(fromEvent(eventValue.getM()));
        }
        return builder.build();
    }

    @JacocoGenerated
    private static <T> Map<String, T> toSimpleMapValue(Map<String, AttributeValue> values) {
        if (values == null) {
            return Map.of();
        }

        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, T> result = new LinkedHashMap<>(values.size());
        for (Map.Entry<String, AttributeValue> entry : values.entrySet()) {
            T t = toSimpleValue(entry.getValue());
            result.put(entry.getKey(), t);
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "PMD.CognitiveComplexity"})
    private static <T> T toSimpleValue(AttributeValue value) {
        if (value == null) {
            return null;
        }
        if (Boolean.TRUE.equals(value.nul())) {
            return null;
        } else if (Boolean.FALSE.equals(value.nul())) {
            throw new UnsupportedOperationException("False-NULL is not supported in DynamoDB");
        } else if (value.bool() != null) {
            return (T) value.bool();
        } else if (value.s() != null) {
            return (T) value.s();
        } else if (value.n() != null) {
            return (T) new BigDecimal(value.n());
        } else if (value.b() != null) {
            return (T) value.b().asByteArray();
        } else if (value.hasSs() && !value.ss().isEmpty()) {
            return (T) new LinkedHashSet<>(value.ss());
        } else if (value.hasNs() && !value.ns().isEmpty()) {
            Set<BigDecimal> set = new LinkedHashSet<>(value.ns().size());
            value.ns()
                    .stream()
                    .map(BigDecimal::new)
                    .forEach(set::add);
            return (T) set;
        } else if (value.hasBs() && !value.bs().isEmpty()) {
            Set<byte[]> set = new LinkedHashSet<>(value.bs().size());
            for (SdkBytes bb : value.bs()) {
                set.add(bb.asByteArray());
            }
            return (T) set;
        } else if (value.hasL()) {
            return (T) toSimpleList(value.l());
        } else if (value.hasM()) {
            return (T) toSimpleMapValue(value.m());
        } else {
            return null;
        }
    }

    private static List<Object> toSimpleList(List<AttributeValue> attrValues) {
        if (attrValues == null) {
            return List.of();
        }
        List<Object> result = new ArrayList<>(attrValues.size());
        for (AttributeValue attrValue : attrValues) {
            Object value = toSimpleValue(attrValue);
            result.add(value);
        }
        return result;
    }
}
