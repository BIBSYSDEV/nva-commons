package no.unit.commons.apigateway.authentication;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class StatementElement {

    private String effect;
    private String action;
    private String resource;

    @JacocoGenerated
    public StatementElement() {
    }

    private StatementElement(Builder builder) {
        setEffect(builder.effect);
        setAction(builder.action);
        setResource(builder.resource);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @JacocoGenerated
    public String getEffect() {
        return effect;
    }

    @JacocoGenerated
    public void setEffect(String effect) {
        this.effect = effect;
    }

    @JacocoGenerated
    public String getAction() {
        return action;
    }

    @JacocoGenerated
    public void setAction(String action) {
        this.action = action;
    }

    @JacocoGenerated
    public String getResource() {
        return resource;
    }

    @JacocoGenerated
    public void setResource(String resource) {
        this.resource = resource;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getEffect(), getAction(), getResource());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StatementElement that = (StatementElement) o;
        return Objects.equals(getEffect(), that.getEffect())
               && Objects.equals(getAction(), that.getAction())
               && Objects.equals(getResource(), that.getResource());
    }

    public static final class Builder {

        private String effect;
        private String action;
        private String resource;

        private Builder() {
        }

        public Builder withEffect(String val) {
            effect = val;
            return this;
        }

        public Builder withAction(String val) {
            action = val;
            return this;
        }

        public Builder withResource(String val) {
            resource = val;
            return this;
        }

        public StatementElement build() {
            return new StatementElement(this);
        }
    }
}
