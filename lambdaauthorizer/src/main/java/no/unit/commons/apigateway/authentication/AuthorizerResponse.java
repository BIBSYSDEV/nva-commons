package no.unit.commons.apigateway.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import nva.commons.commons.JsonUtils;

public class AuthorizerResponse {

    @JsonProperty("principalId")
    private String principalId;

    @JsonProperty("policyDocument")
    private AuthPolicy policyDocument;

    public AuthorizerResponse() {
    }

    private AuthorizerResponse(Builder builder) {
        setPrincipalId(builder.principalId);
        setPolicyDocument(builder.policyDocument);
    }

    public static AuthorizerResponse fromOutputStream(ByteArrayOutputStream outputStream)
        throws JsonProcessingException {
        String content = outputStream.toString(StandardCharsets.UTF_8);
        return JsonUtils.objectMapper.readValue(content, AuthorizerResponse.class);
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

        public AuthorizerResponse build() {
            return new AuthorizerResponse(this);
        }
    }
}
