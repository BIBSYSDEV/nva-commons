package no.unit.nva.hamcrest;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.databind.JsonNode;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.JacocoGenerated;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

@JacocoGenerated
public class DoesNotHaveNullOrEmptyFields<T> extends BaseMatcher<T> {

    public static final String FIELD_DELIMITER = ",";
    public static final String PROPERTY_READ_ERRROR = "Could not read value for property:";

    private List<PropertyValuePair> emptyFields;

    /**
     * use {@link DoesNotHaveEmptyValues#doesNotHaveEmptyValues} instead.
     *
     * @param <R> the type of the object
     * @return a new matcher
     */
    @Deprecated
    public static <R> DoesNotHaveNullOrEmptyFields<R> doesNotHaveNullOrEmptyFields() {
        return new DoesNotHaveNullOrEmptyFields<>();
    }

    @Override
    public boolean matches(Object actual) {
        return assertThatNoPublicFieldIsNull(actual);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("All fields to be non empty");
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        String emptyFieldNames = emptyFields.stream()
            .map(res -> res.propertyName)
            .collect(Collectors.joining(FIELD_DELIMITER));

        description.appendText("The following fields were found empty:")
            .appendText(emptyFieldNames);
    }

    private boolean assertThatNoPublicFieldIsNull(Object input) {
        Stream<PropertyDescriptor> properties = retrieveProperties(input);
        emptyFields = properties
            .map(prop -> readPropertyValue(prop, input))
            .filter(this::isEmpty)
            .collect(Collectors.toList());
        return emptyFields.isEmpty();
    }

    private PropertyValuePair readPropertyValue(PropertyDescriptor prop, Object input) {
        try {
            Method getter = prop.getReadMethod();
            Object propertyValue = getter.invoke(input);
            return new PropertyValuePair(prop.getName(), propertyValue);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(PROPERTY_READ_ERRROR + prop.getName());
        }
    }

    private Stream<PropertyDescriptor> retrieveProperties(Object input) {
        try {
            return Arrays.stream(Introspector
                                     .getBeanInfo(input.getClass(), Object.class)
                                     .getPropertyDescriptors());
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isEmpty(PropertyValuePair propertyValuePair) {
        if (isNull(propertyValuePair.value)) {
            return true;
        } else {
            if (propertyValuePair.value instanceof Collection<?>) {
                Collection col = (Collection) propertyValuePair.value;
                return col.isEmpty();
            } else if (propertyValuePair.value instanceof Map<?, ?>) {
                Map map = (Map) propertyValuePair.value;
                return map.isEmpty();
            } else if (propertyValuePair.value instanceof String) {
                String str = (String) propertyValuePair.value;
                return str.isBlank();
            } else if (propertyValuePair.value instanceof JsonNode) {
                JsonNode node = (JsonNode) propertyValuePair.value;
                return node.isEmpty();
            } else {
                return false;
            }
        }
    }

    private static class PropertyValuePair {

        public final String propertyName;
        public final Object value;

        public PropertyValuePair(String propertyName, Object value) {
            this.propertyName = propertyName;
            this.value = value;
        }

        @JacocoGenerated
        @Override
        public String toString() {
            return this.propertyName;
        }
    }
}
