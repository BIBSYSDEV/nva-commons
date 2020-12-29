package no.unit.commons.apigateway.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import nva.commons.commons.JacocoGenerated;

public class AuthPolicy {

    public static final String VERSION = "2012-10-17";

    @JsonProperty("Version")
    private String version;
    @JsonProperty("Statement")
    private List<StatementElement> statement;

    public AuthPolicy() {
    }

    private AuthPolicy(Builder builder) {
        setVersion(VERSION);
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

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthPolicy that = (AuthPolicy) o;
        return equalVersions(that)
            && equalStatements(that.getStatement());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getVersion(), getStatement());
    }

    private boolean equalStatements(List<StatementElement> otherStatement) {
        boolean leftContainsRight = new HashSet<>(statement).containsAll(otherStatement);
        boolean rightContainsLeft = new HashSet<>(otherStatement).containsAll(statement);
        return leftContainsRight && rightContainsLeft;
    }

    private boolean equalVersions(AuthPolicy that) {
        return Objects.equals(getVersion(), that.getVersion());
    }

    public static final class Builder {

        private List<StatementElement> statement;

        private Builder() {
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
