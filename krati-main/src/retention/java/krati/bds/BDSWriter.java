package krati.bds;

import krati.bds.marker.AdvanceMarker;

/**
 * Public writing interface to a Retention.
 * @author spike(alperez)
 *
 * @param <T>
 * @param <A>
 */
public interface BDSWriter<T, A extends AdvanceMarker> {
    /**
     * Stores a new event into the Retention.
     * @param event the event to store.
     * @throws OutOfOrderException if this new event is before any of the previously
     * stored events.
     */
    public void put(Event<T, A> event) throws OutOfOrderException;
}
