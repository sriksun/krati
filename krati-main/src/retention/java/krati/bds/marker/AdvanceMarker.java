package krati.bds.marker;

import krati.retention.clock.Occurred;

/**
 * @author jwu
 * @author alperez
 *
 */
public abstract class AdvanceMarker {
    public abstract AdvanceMarker infimum();
    
    public abstract Occurred compareTo(AdvanceMarker a);
    
    /**
     * @return <code>true</code> if this Clock occurred before the specified Clock <code>c</code>.
     *         Otherwise, <code>false</code>.
     */
    public boolean before(AdvanceMarker c) {
        return compareTo(c) == Occurred.BEFORE;
    }
    
    /**
     * @return <code>true</code> if this Clock occurred after the specified Clock <code>c</code>.
     *         Otherwise, <code>false</code>.
     */
    public boolean after(AdvanceMarker c) {
        return compareTo(c) == Occurred.AFTER;
    }
    
    /**
     * @return <code>true</code> if this Clock is equal to or occurred before the specified Clock <code>c</code>.
     *         Otherwise, <code>false</code>.
     */
    public boolean beforeEqual(AdvanceMarker c) {
        Occurred o = compareTo(c);
        return o == Occurred.BEFORE || o == Occurred.EQUICONCURRENTLY;
    }
    
    /**
     * @return <code>true</code> if this Clock is equal to or occurred after the specified Clock <code>c</code>.
     *         Otherwise, <code>false</code>.
     */
    public boolean afterEqual(AdvanceMarker c) {
        Occurred o = compareTo(c);
        return o == Occurred.AFTER || o == Occurred.EQUICONCURRENTLY; 
    }
}
