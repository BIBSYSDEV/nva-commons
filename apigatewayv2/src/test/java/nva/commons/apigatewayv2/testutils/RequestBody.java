package nva.commons.apigatewayv2.testutils;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.jr.ob.JSON;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RequestBody {

    public static final String FIELD1 = "field1";
    public static final String FIELD2 = "field2";
    public static final String EMPTY_LIST = "emptyList";

    public static final String TYPE_ATTRIBUTE = "type";
    private List<Object> emptyList = Collections.emptyList();
    private String field1;
    private String field2;

    public RequestBody() {
    }

    public RequestBody(String field1, String field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    public List<Object> getEmptyList() {
        return emptyList;
    }

    public void setEmptyList(List<Object> emptyList) {
        this.emptyList = emptyList;
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
    public int hashCode() {
        return Objects.hash(field1, field2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RequestBody that = (RequestBody) o;
        return Objects.equals(field1, that.field1)
               && Objects.equals(field2, that.field2);
    }

    @Override
    public String toString() {
        return attempt(() -> JSON.std.asString(this)).orElseThrow();
    }
}