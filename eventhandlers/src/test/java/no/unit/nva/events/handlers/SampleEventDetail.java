package no.unit.nva.events.handlers;

import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getMessage(), getIdentifier());
    }
}
