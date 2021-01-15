package no.unit.nva.events.handlers;

/**
 * Interface for adding type to Json objects. The interface fixes the problem of Jackson where the annotations include
 * the type only for the top object and not for the children object.
 */
public interface WithType {

    default String getType() {
        return this.getClass().getName();
    }

    default void setType() {
        // do nothing;
    }
}
