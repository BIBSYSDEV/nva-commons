package no.unit.nva.commons.dlq;

import java.util.Collection;

@FunctionalInterface
public interface FailedEventHandlingService {

    void handleFailedEvents(Collection<String> failedEvents);
}
