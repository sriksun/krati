package krati.bds;

import java.io.Serializable;

import krati.bds.marker.AdvanceMarker;

public interface Event<T ,A extends AdvanceMarker> extends Serializable {
    
    /**
     * Gets the value of this Event.
     */
    public T getValue();
    
    /**
     * Gets the clock of this Event.
     */
    public A getAdvanceMarker();
}
