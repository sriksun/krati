/*
 * Copyright (c) 2010-2012 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package krati.bds.eventBatch;

import krati.bds.marker.AdvanceMarker;

/**
 * SimpleEventBatchCursor
 * 
 * @version 0.4.2
 * @author jwu
 * 
 * <p>
 * 07/31, 2011 - Created <br/>
 */
public class SimpleEventBatchCursor<A extends AdvanceMarker> implements EventBatchCursor<A> {
    private int _batchLookup;
    private EventBatchHeader<A> _batchHeader;
    
    /**
     * SimpleEventBatchCursor
     * 
     * @param batchLookup - the lookup index of an EventBatch
     * @param batchHeader - the batch header of an EventBatch
     */
    public SimpleEventBatchCursor(int batchLookup, EventBatchHeader<A> batchHeader) {
        this._batchLookup = batchLookup;
        this._batchHeader = batchHeader;
    }
    
    @Override
    public int getLookup() {
        return _batchLookup;
    }
    
    @Override
    public EventBatchHeader<A> getHeader() {
        return _batchHeader;
    }
    
    @Override
    public void setHeader(EventBatchHeader<A> header) {
        this._batchHeader = header;
    }
}
