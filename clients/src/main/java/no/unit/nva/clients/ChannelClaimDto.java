package no.unit.nva.clients;

import static no.unit.nva.clients.ChannelClaimDto.CLAIMED_CHANNEL_TYPE;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(CLAIMED_CHANNEL_TYPE)
public record ChannelClaimDto(CustomerSummaryDto claimedBy, ChannelClaim channelClaim) implements JsonSerializable {

    static final String CLAIMED_CHANNEL_TYPE = "ClaimedChannel";

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonTypeName(ChannelClaim.CHANNEL_CLAIM_TYPE)
    public record ChannelClaim(URI channel, ChannelConstraint constraint) {

        private static final String CHANNEL_CLAIM_TYPE = "ChannelClaim";

        public record ChannelConstraint(String publishingPolicy, String editingPolicy, List<String> scope) {

        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonTypeName(CustomerSummaryDto.TYPE)
    public record CustomerSummaryDto(URI id, URI organizationId) {

        private static final String TYPE = "Customer";
    }
}
