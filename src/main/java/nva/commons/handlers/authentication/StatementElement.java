package nva.commons.handlers.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatementElement {

    @JsonProperty("Effect")
    private String effect;
    @JsonProperty("Action")
    private String action;
    @JsonProperty("Resource")
    private String resource;

    private StatementElement(Builder builder) {
        setEffect(builder.effect);
        setAction(builder.action);
        setResource(builder.resource);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
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
