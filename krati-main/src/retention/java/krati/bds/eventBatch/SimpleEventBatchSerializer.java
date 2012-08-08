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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

import krati.io.SerializationException;
import krati.io.Serializer;
import krati.bds.Event;
import krati.bds.SimpleEvent;
import krati.bds.marker.AdvanceMarker;

/**
 * SimpleEventBatchSerializer
 * 
 * <pre>
 *               HEADER                                               [        EVENT         ] ...
 *  ------- ---- ------ ------------ -------------- -------- -------- [----------- ----------] ...
 *  int     int  long   long         long           Clock    Clock    [int         byte[]    ] ...
 *  ------- ---- ------ ------------ -------------- -------- -------- [----------- ----------] ...
 *  version size origin creationTime completionTime minClock maxClock [valueLength valueBytes] ... 
 * </pre>
 * 
 * @version 0.4.2
 * @author jwu
 * 
 * <p>
 * 08/01, 2011 - Created
 */
public class SimpleEventBatchSerializer<T, A extends AdvanceMarker> implements EventBatchSerializer<T, A> {
    private final static int NUM_NON_CLOCK_BYTES_IN_HEADER = 34;
    
    private final Serializer<T> _valueSerializer;
    private final Serializer<A> _clockSerializer;
    
    public SimpleEventBatchSerializer(Serializer<T> valueSerializer, Serializer<A> clockSerializer) {
        this._valueSerializer = valueSerializer;
        this._clockSerializer = clockSerializer;
    }
    
    @Override
    public EventBatch<T, A> deserialize(byte[] bytes) throws SerializationException {
        if(bytes == null) return null;
        
        // Deserialize header first
        EventBatchHeader<A> header = deserializeHeader(bytes);
        //FIXME
        int minClockLength = header.getMinMarker().values().length << 3;
        int maxClockLength = header.getMaxMarker().values().length << 3;
        int headerLength = NUM_NON_CLOCK_BYTES_IN_HEADER + minClockLength + maxClockLength;
        
        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes, headerLength, bytes.length - headerLength);
            SimpleEventBatch<T, A> batch = new SimpleEventBatch<T, A>(header.getOrigin(), header.getMinMarker(), header.getSize());
            batch.setCreationTime(header.getCreationTime());
            batch.setCompletionTime(header.getCompletionTime());
            
            // Read individual events
            for(int i = 0, size = header.getSize(); i < size; i++) {
                // Deserialize value
                int length = buffer.getInt();
                byte[] valueBytes = new byte[length];
                buffer.get(valueBytes);
                
                T value = _valueSerializer.deserialize(valueBytes);
                
                // Deserialize clock
                length = buffer.get();
                byte[] clockBytes = new byte[length];
                buffer.get(clockBytes);
                
                A clock = _clockSerializer.deserialize(clockBytes);
                
                Event<T, A> event = new SimpleEvent<T, A>(value, clock);
                if(!batch.put(event)) {
                    throw new SerializationException("Invalid clocks:" + " clock=" + clock + " minClock=" + header.getMinMarker() + " maxClock=" + header.getMaxMarker());
                }
            }
            
            return batch;
        } catch(SerializationException e) {
            throw e;
        } catch(Exception e) {
            throw new SerializationException("Failed to deserialize", e);
        }
    }
    
    @Override
    public byte[] serialize(EventBatch<T, A> object) throws SerializationException {
        if(object == null) return null;
         
        byte[] valueBytes = null;
        byte[] clockBytes = null;
        byte[] intBytes = new byte[4];
        byte[] clockLenByte = new byte[1];
        ByteBuffer bbInt = ByteBuffer.wrap(intBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Serialize header first
        byte[] headerBytes = serializeHeader(object);
        
        try {
            // Write header bytes
            baos.write(headerBytes);
            
            Iterator<Event<T, A>> iter = object.iterator();
            while(iter.hasNext()) {
                Event<T, A> event = iter.next();
                T value = event.getValue();
                A clock = event.getAdvanceMarker();
                
                // Serialize value
                valueBytes = _valueSerializer.serialize(value);
                
                bbInt.clear();
                bbInt.putInt(valueBytes.length);
                
                baos.write(intBytes);
                baos.write(valueBytes);
                
                // Serialize clock
                clockBytes = _clockSerializer.serialize(clock);
                clockLenByte[0] = (byte)clockBytes.length;
                
                baos.write(clockLenByte);
                baos.write(clockBytes);
            }
            
            return baos.toByteArray();
        } catch(SerializationException e) {
            throw e;
        } catch(Exception e) {
            throw new SerializationException("Failed to serialize", e);
        }
    }
    
    @Override
    public EventBatchHeader<A> deserializeHeader(byte[] bytes) throws SerializationException {
        if(bytes == null) {
            return null;
        }
        
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        
        // Version
        int version = bb.getInt();
        if(version < 0) {
            throw new SerializationException("Invalid version: " + version);
        }
        
        // Size
        int size = bb.getInt();
        if(size < 0) {
            throw new SerializationException("Invalid size: " + size);
        }
        
        // Origin
        long origin = bb.getLong();
        
        long creationTime = bb.getLong();
        
        long completionTime = bb.getLong();
        
        // minClock
        int length = bb.get();
        byte[] minClockBytes = new byte[length];
        bb.get(minClockBytes);
        A minClock = _clockSerializer.deserialize(minClockBytes);
        
        // maxClock
        length = bb.get();
        byte[] maxClockBytes = new byte[length];
        bb.get(maxClockBytes);
        A maxClock = _clockSerializer.deserialize(maxClockBytes);
        
        return new SimpleEventBatchHeader<A>(version, size, origin, creationTime, completionTime, minClock, maxClock);
    }
    
    @Override
    public byte[] serializeHeader(EventBatchHeader<A> header) throws SerializationException {
        if(header == null) {
            return null;
        }
        
        byte[] minClockBytes = _clockSerializer.serialize(header.getMinMarker());
        byte[] maxClockBytes = _clockSerializer.serialize(header.getMaxMarker());
        
        // Int Int Long Long Long Byte Clock Byte Clock
        // 4 + 4 + 8 + 8 + 8 + 1 + length + 1 + length
        
        byte[] byteArray = new byte[NUM_NON_CLOCK_BYTES_IN_HEADER + minClockBytes.length + maxClockBytes.length];
        ByteBuffer bb = ByteBuffer.wrap(byteArray);
        
        bb.putInt(header.getVersion());         // 4
        bb.putInt(header.getSize());            // 4
        bb.putLong(header.getOrigin());         // 8
        bb.putLong(header.getCreationTime());   // 8
        bb.putLong(header.getCompletionTime()); // 8
        bb.put((byte)minClockBytes.length);     // 1
        bb.put(minClockBytes);                  // minClockBytes
        bb.put((byte)maxClockBytes.length);     // 1
        bb.put(maxClockBytes);                  // maxClockBytes
        
        return byteArray;
    }
    
}
