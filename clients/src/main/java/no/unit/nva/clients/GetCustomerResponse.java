package no.unit.nva.clients;

import java.net.URI;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;

public record GetCustomerResponse(URI id, UUID identifier, String name, String displayName, String shortName,
                                  URI cristinId) implements JsonSerializable {

}
