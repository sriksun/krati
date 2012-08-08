package krati.bds;

import java.util.List;

import krati.bds.marker.AdvanceMarker;

/**
 * Public reading interface for a Retention.
 * @author spike(alperez)
 *
 * @param <T>
 * @param <A>
 */
interface BDSReader<T, A extends AdvanceMarker> {
    
    /**
     * Gets the most current Advance marker.
     * The contract is that for two serial calls to this method, the second
     * one will return a marker that's after the first one.
     */
    public A getCurrentMarker();
    
    /**
     * Gets a list of events, in order of AdvanceMarker, starting from the given marker.
     * First:
     *  <A1, ?> getEvent(A0)
     *  A1 is after or equal to A0. This is insured even if A0 is after to getCurrentMarker().
     * Second:
     *  <A1, ?> getEvents(A0)
     *  <A2, ?> getEvents(A0)
     *  Assures than A2 is not "before" A1.
     * Third:
     *  <?, L1> getEvents(A0)
     *  <?, L2> getEvents(A0)
     *  L1 is included in L2.
     *  The inclusion instead of equality is necessary to support non monotonically increasing
     *  source event streams.
     * Fourth:
     *  <A1, L> getEvents(A0)
     *  L may contain events that happened before A0, but must contain all events seen by
     *  the retention timed after or equal A0.
     *  event streams. Special implementations may decide to give a stronger warranty here.
     * It has to be noted that there's no warranty that the same event won't be returned more
     * than once. In general the semantics for reading from this kind of store are "at least
     * once".
     * Also, it's important to take into account that no explicit contract is made on the
     * length of the returned event list. In practice implementations need provide more
     * information about their behavior on this respect.
     * 
     * @param since
     * @return
     */
    public Pair<A, List<Event<T, A>>> getEvents(A since);
    
}
