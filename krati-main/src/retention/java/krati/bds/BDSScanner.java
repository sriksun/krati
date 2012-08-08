package krati.bds;

import java.util.List;

import krati.bds.marker.AdvanceMarker;

/**
 * Low level version of a RetentionReader.
 * 
 * @author spike(alperez)
 *
 * @param <T>
 * @param <A>
 */
interface BDSScanner<T, A extends AdvanceMarker, Cursor extends AdvanceMarker> {
    
    /**
     * @return the current cursor in Retention.
     */
    public Cursor getCurrentCursor();
    
    /**
     * Gets the cursor of the first event that occurred at <tt>sinceClock</tt>
     * or the cursor of an event that occurred right before <tt>sinceClock</tt>.
     * 
     * @param sinceClock - the since Clock.
     * @return RetentionCursor <tt>null</tt> if the first event at <tt>sinceClock</tt>
     * is removed from retention (i.e. out of the retention period).
     */
    public Cursor getCursor(A since);
    
    /**
     * Gets a number of events starting from a give cursor in the Retention.
     * The number of events is determined internally by the Retention and it is
     * up to the batch size.   
     * 
     * @param pos  - the retention cursor from where events will be read
     * @param list - the event list to fill in
     * @return the next cursor from where new events will be read. 
     */
    public Pair<Cursor, List<Event<T, A>>> get(Cursor pos);
    
}
