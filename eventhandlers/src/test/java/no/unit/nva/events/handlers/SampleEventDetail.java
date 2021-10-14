package no.unit.nva.events.handlers;

import java.beans.IntrospectionException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SampleEventDetail implements WithType {

    private String emptyString;
    private String message;
    private Integer identifier;
    private List<String> someEmptyList;

    public static List<String> propertyNamesOfEmptyFields() throws IntrospectionException {
        return List.of("emptyString", "someEmptyList");
    }

    public static SampleEventDetail eventWithEmptyFields() {
        SampleEventDetail sampleEventDetail = new SampleEventDetail();
        sampleEventDetail.setEmptyString("");
        sampleEventDetail.setSomeEmptyList(Collections.emptyList());
        return sampleEventDetail;
    }

    public List<String> getSomeEmptyList() {
        return someEmptyList;
    }

    public void setSomeEmptyList(List<String> someEmptyList) {
        this.someEmptyList = someEmptyList;
    }

    public String getEmptyString() {
        return emptyString;
    }

    public void setEmptyString(String emptyString) {
        this.emptyString = emptyString;
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
        return Objects.hash(getEmptyString(), getMessage(), getIdentifier());
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
        return Objects.equals(getEmptyString(), that.getEmptyString())
               && Objects.equals(getMessage(), that.getMessage())
               && Objects.equals(getIdentifier(), that.getIdentifier());
    }
}
