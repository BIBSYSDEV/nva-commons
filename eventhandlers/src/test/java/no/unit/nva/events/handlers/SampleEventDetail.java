package no.unit.nva.events.handlers;

import static no.unit.nva.events.handlers.EventHandlerTest.CLASS_PROPERTY;
import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SampleEventDetail implements WithType {

    private String name;
    private String message;
    private Integer identifier;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getMessage(), getIdentifier());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SampleEventDetail that = (SampleEventDetail) o;
        return Objects.equals(getName(), that.getName())
               && Objects.equals(getMessage(), that.getMessage())
               && Objects.equals(getIdentifier(), that.getIdentifier());
    }

    public static List<String> extractPropertyNamesFromSampleEventDetailClass() throws IntrospectionException {
        return Arrays.stream(
                Introspector.getBeanInfo(SampleEventDetail.class).getPropertyDescriptors())
            .map(FeatureDescriptor::getName)
            .filter(name -> !name.equals(CLASS_PROPERTY)).collect(Collectors.toList());
    }
}
