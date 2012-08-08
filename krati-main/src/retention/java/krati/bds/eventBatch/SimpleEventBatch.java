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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import krati.bds.Event;
import krati.bds.marker.AdvanceMarker;
import krati.retention.clock.Occurred;

/**
 * SimpleEventBatch
 * 
 * @version 0.4.2
 * @author jwu
 * 
 * <p>
 * 08/01, 2011 - Created <br/>
 * 02/09, 2012 - Made Cloneable <br/>
 */
public final class SimpleEventBatch<T, A extends AdvanceMarker> implements EventBatch<T, A>, Cloneable {
    private static final long serialVersionUID = 1L;
    private final long _origin;
    private final int _capacity;
    private volatile A _minClock;
    private volatile A _maxClock;
    private volatile long _creationTime;
    private volatile long _completionTime;
    private final ArrayList<Event<T,A>> _events;
    
    public SimpleEventBatch(long origin, A initClock) {
        this(origin, initClock, EventBatch.DEFAULT_BATCH_SIZE);
    }
    
    public SimpleEventBatch(long origin, A initClock, int capacity) {
        this._origin = origin;
        this._minClock = initClock;
        this._maxClock = initClock;
        this._capacity = Math.max(EventBatch.MINIMUM_BATCH_SIZE, capacity);
        this._events = new ArrayList<Event<T,A>>(this._capacity);
        this._creationTime = System.currentTimeMillis();
        this._completionTime = System.currentTimeMillis();
    }
    
    @Override
    public int getVersion() {
        return EventBatch.VERSION;
    }
    
    @Override
    public int getSize() {
        return _events.size();
    }
    
    @Override
    public long getOrigin() {
        return _origin;
    }
    
    @Override
    public long getCreationTime() {
        return _creationTime;
    }
    
    @Override
    public void setCreationTime(long time) {
        this._creationTime = time;
    }
    
    @Override
    public long getCompletionTime() {
        return _completionTime;
    }
    
    @Override
    public void setCompletionTime(long time) {
        this._completionTime = time;
    }
    
    @Override
    public A getMinMarker() {
        return _minClock;
    }
    
    @Override
    public A getMaxMarker() {
        return _maxClock;
    }
    
    @Override
    public Iterator<Event<T,A>> iterator() {
        return _events.iterator();
    }
    
    @Override
    public boolean isEmpty() {
        return _events.isEmpty();
    }
    
    @Override
    public boolean isFull() {
        return _events.size() >= _capacity;
    }
    
    @Override
    public EventBatchHeader<A> getHeader() {
        return new SimpleEventBatchHeader<A>(
                getVersion(),
                getSize(),
                getOrigin(),
                getCreationTime(),
                getCompletionTime(),
                getMinMarker(), getMaxMarker());
    }
    
    @Override
    public A getMarker(long offset) {
        if(_origin <= offset && offset < (_origin + _events.size())) {
            return _events.get((int)(offset - _origin)).getAdvanceMarker();
        } else if(_origin == offset) {
            return _minClock;
        }
        
        return null;
    }
    
    @Override
    public long getOffset(A sinceClock) {
        if(_minClock.before(sinceClock) && !sinceClock.after(_maxClock)) {
            int i = 0;
            for(; i < _events.size(); i++) {
                Event<T, A> e = _events.get(i);
                Occurred occ = sinceClock.compareTo(e.getAdvanceMarker());
                if(occ == Occurred.EQUICONCURRENTLY) {
                    break;
                } else if(occ == Occurred.BEFORE || occ == Occurred.CONCURRENTLY) {
                    i--;
                    break;
                }
            }
            return _origin + i;
        }
        
        return -1;
    }
    
    @Override
    public boolean put(Event<T, A> event) {
        A marker = event.getAdvanceMarker();
        int size = _events.size();
        
        if(size == 0 && _minClock.before(marker)) {
            _minClock = marker;
            _maxClock = marker;
        }
        
        if(size < _capacity && _maxClock.beforeEqual(marker)) {
            _events.add(event);
            _maxClock = marker;
            return true;
        }
        
        return false;
    }
    
    @Override
    public long get(long offset, List<Event<T, A>> list) {
        return get(offset, Integer.MAX_VALUE, list);
    }
    
    @Override
    public long get(long offset, int count, List<Event<T, A>> list) {
        int ind = (int)(offset - getOrigin());
        if(0 <= ind && ind < _events.size()) {
            for(; ind < _events.size(); ind++) {
                Event<T, A> e = _events.get(ind);
                list.add(e);
                
                count--;
                if(0 == count) {
                    return getOrigin() + ind + 1;
                }
            }
            return getOrigin() + ind;
        }
        
        return offset;
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(SimpleEventBatch.class.getSimpleName()).append("{");
        b.append("version=").append(getVersion()).append(",");
        b.append("size=").append(_events.size()).append(",");
        b.append("origin=").append(_origin).append(",");
        b.append("creationTime=").append(_creationTime).append(",");
        b.append("completionTime=").append(_completionTime).append(",");
        b.append("minClock=").append(_minClock).append(",");
        b.append("maxClock=").append(_maxClock).append("}");
        return b.toString();
    }
    
    @Override
    public SimpleEventBatch<T, A> clone() {
        SimpleEventBatch<T, A> batch = new SimpleEventBatch<T, A>(getOrigin(), _minClock, _capacity);
        batch._maxClock = _maxClock;
        batch._events.addAll(_events);
        batch._creationTime = _creationTime;
        batch._completionTime = _completionTime;
        return batch;
    }
}
