package nva.commons.handlers.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestAuthorizerResponse {

    @JsonProperty("principalId")
    private String principalId;

    @JsonProperty("policyDocument")
    private AuthPolicy policyDocument;

    public RequestAuthorizerResponse() {
    }

    private RequestAuthorizerResponse(Builder builder) {
        setPrincipalId(builder.principalId);
        setPolicyDocument(builder.policyDocument);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(String principalId) {
        this.principalId = principalId;
    }

    public AuthPolicy getPolicyDocument() {
        return policyDocument;
    }

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

        public RequestAuthorizerResponse build() {
            return new RequestAuthorizerResponse(this);
        }
    }
}
