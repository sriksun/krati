package krati.bds.eventBatch;

import krati.bds.marker.AdvanceMarker;

/**
 * EventBatchCursor
 * 
 * @version 0.4.2
 * @author jwu
 * 
 * <p>
 * 07/31, 2011 - Created <br/>
 */
public interface EventBatchCursor<A extends AdvanceMarker> {
    
    public int getLookup();
    
    public EventBatchHeader<A> getHeader();
    
    public void setHeader(EventBatchHeader<A> header);
}
