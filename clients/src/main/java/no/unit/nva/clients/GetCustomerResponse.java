package no.unit.nva.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;

public record GetCustomerResponse(@JsonProperty("id") URI id, UUID identifier, String name, String displayName,
                                  String shortName,
                                  URI cristinId) implements JsonSerializable {

}
