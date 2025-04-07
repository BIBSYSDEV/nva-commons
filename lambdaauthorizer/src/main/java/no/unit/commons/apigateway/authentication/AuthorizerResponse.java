package no.unit.commons.apigateway.authentication;

import nva.commons.core.JacocoGenerated;

public class AuthorizerResponse {

    private String principalId;
    private AuthPolicy policyDocument;

    @JacocoGenerated
    public AuthorizerResponse() {
    }

    private AuthorizerResponse(Builder builder) {
        this.principalId = builder.principalId;
        this.policyDocument = builder.policyDocument;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @JacocoGenerated
    public String getPrincipalId() {
        return principalId;
    }

    @JacocoGenerated
    public void setPrincipalId(String principalId) {
        this.principalId = principalId;
    }

    @JacocoGenerated
    public AuthPolicy getPolicyDocument() {
        return policyDocument;
    }

    @JacocoGenerated
    public void setPolicyDocument(AuthPolicy policyDocument) {
        this.policyDocument = policyDocument;
    }

    public static final class Builder {

        private String principalId;
        private AuthPolicy policyDocument;

        private Builder() {
        }

        public Builder withPrincipalId(String val) {
            principalId = val;
            return this;
        }

        public Builder withPolicyDocument(AuthPolicy val) {
            policyDocument = val;
            return this;
        }

        public AuthorizerResponse build() {
            return new AuthorizerResponse(this);
        }
    }
}
