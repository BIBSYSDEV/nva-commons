package nva.commons.core;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Objects;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = SamplePojo.TYPE_ATTRIBUTE)
public class SamplePojo {

    public static final String TYPE_ATTRIBUTE = "type";
    String field1;
    String field2;

    public SamplePojo() {
    }

    public SamplePojo(String field1, String field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SamplePojo that = (SamplePojo) o;
        return Objects.equals(field1, that.field1)
            && Objects.equals(field2, that.field2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field1, field2);
    }
}