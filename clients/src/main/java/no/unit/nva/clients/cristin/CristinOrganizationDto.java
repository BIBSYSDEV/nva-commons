package no.unit.nva.clients.cristin;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.SingletonCollector;

public record CristinOrganizationDto(URI id, URI context, String type, List<CristinOrganizationDto> partOf,
                                     List<CristinOrganizationDto> hasPart, String country, Map<String, String> labels,
                                     String acronym) implements JsonSerializable {

    @JsonIgnore
    public CristinOrganizationDto getTopLevelOrganization() {
        if (hasPartOf(this)) {

            var organization = partOf().stream().collect(SingletonCollector.collect());

            while (hasPartOf(organization)) {
                organization = organization.partOf().stream().collect(SingletonCollector.collect());
            }

            return organization;
        }

        return this;
    }

    private static boolean hasPartOf(CristinOrganizationDto organization) {
        return nonNull(organization.partOf()) && !organization.partOf().isEmpty();
    }
}
