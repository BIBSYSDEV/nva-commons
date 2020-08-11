package nva.commons.handlers.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AuthPolicy {

    public static final String VERSION = "2012-10-17";

    @JsonProperty("Version")
    private String version;
    @JsonProperty("Statement")
    private List<StatementElement> statement;

    public AuthPolicy() {
    }

    private AuthPolicy(Builder builder) {
        setVersion(builder.version);
        setStatement(builder.statement);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<StatementElement> getStatement() {
        return statement;
    }

    public void setStatement(List<StatementElement> statement) {
        this.statement = statement;
    }

    public static final class Builder {

        private String version;
        private List<StatementElement> statement;

        private Builder() {
            version = VERSION;
        }

        public Builder withVersion(String val) {
            version = val;
            return this;
        }

        public Builder withStatement(List<StatementElement> val) {
            statement = val;
            return this;
        }

        public AuthPolicy build() {
            return new AuthPolicy(this);
        }
    }
}
