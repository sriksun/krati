package krati.bds.marker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import krati.io.SerializationException;
import krati.io.Serializer;
import krati.retention.clock.Occurred;

public class LongAdvanceMarker extends AdvanceMarker {
    private final static LongAdvanceMarker INFIMUM = new LongAdvanceMarker(0L);
    private final long _value;
    
    public LongAdvanceMarker(long value) {
        //TODO: validate >=0;
        this._value = value;
    }

    @Override
    public AdvanceMarker infimum() {
        return INFIMUM;
    }

    @Override
    public Occurred compareTo(AdvanceMarker a) {
        LongAdvanceMarker aa = (LongAdvanceMarker) a;
        if (_value == aa._value) return Occurred.CONCURRENTLY;
        if (_value < aa._value) return Occurred.BEFORE;
        return Occurred.AFTER;
    }

    public static class LongAdvanceMarkerSerializer implements Serializer<LongAdvanceMarker> {

        @Override
        public byte[] serialize(LongAdvanceMarker am) throws SerializationException {
            try {
                ByteArrayOutputStream s = new ByteArrayOutputStream();
                ObjectOutputStream ss = new ObjectOutputStream(s);
                ss.writeLong(am._value);
                return s.toByteArray();
            } catch (IOException e) {
                throw new SerializationException("Shouldn't happen...", e);
            }
        }

        @Override
        public LongAdvanceMarker deserialize(byte[] bytes) throws SerializationException {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            
            return null;
        }

    }
}
