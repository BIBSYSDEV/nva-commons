package no.unit.nva.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;

public record GetCustomerResponse(@JsonProperty("id") URI id, UUID identifier, String name, String displayName,
                                  String shortName, URI cristinId, String publicationWorkflow, boolean nviInstitution,
                                  boolean rboInstitution, boolean generalSupportEnabled,
                                  List<String> allowFileUploadForTypes, RightsRetentionStrategy rightsRetentionStrategy)
    implements JsonSerializable {

    public record RightsRetentionStrategy(String type, URI id) {

    }
}
