package krati.bds.eventBatch;

import krati.bds.marker.AdvanceMarker;

/**
 * EventBatchHeader
 * 
 * @version 0.4.2
 * @author jwu
 * 
 * <p>
 * 07/31, 2011 - Created
 */
public interface EventBatchHeader<A extends AdvanceMarker> {
    
    public int getVersion();
    
    public int getSize();
    
    public long getOrigin();
    
    public long getCreationTime();
    
    public long getCompletionTime();
    
    public A getMinMarker();
    
    public A getMaxMarker();
}
