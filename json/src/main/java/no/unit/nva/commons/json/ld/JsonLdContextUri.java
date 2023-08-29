package no.unit.nva.commons.json.ld;

import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;

/**
 * Record to allow representation of a JSON-LD remote context such as:
 *
 * <pre>
 *     {
 *         "@context" : "https://example.org/jsonldcontext.jsonld"
 *     }
 * </pre>
 * @param context The URI for the remote JSON-LD context.
 */
public record JsonLdContextUri(@JsonValue URI context) implements JsonLdContext {

}
