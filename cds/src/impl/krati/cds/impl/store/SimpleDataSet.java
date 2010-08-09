package krati.cds.impl.store;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import krati.cds.array.DataArray;
import krati.cds.impl.array.AddressArray;
import krati.cds.impl.array.SimpleDataArray;
import krati.cds.impl.array.basic.SimpleLongArray;
import krati.cds.impl.segment.SegmentFactory;
import krati.cds.impl.segment.SegmentManager;
import krati.cds.store.DataSet;
import krati.cds.store.SetDataHandler;
import krati.util.FnvHashFunction;
import krati.util.HashFunction;

/**
 * A simple implementation of data set with a fixed capacity.
 * 
 * The values are stored in the underlying DataArray using the following format:
 * <pre>
 * [count:int][value-length:int][value:bytes][value-length:int][value:bytes]...
 *            +-----------value 1------------+-----------value 2-----------+
 * </pre>
 * 
 * @author jwu
 *
 */
public class SimpleDataSet implements DataSet<byte[]>
{
    private final static Logger _log = Logger.getLogger(SimpleDataSet.class);
    
    private final SimpleDataArray _dataArray;
    private final SetDataHandler _dataHandler;
    private final HashFunction<byte[]> _hashFunction;
    
    /**
     * Creates a DataSet instance with the settings below:
     * 
     * <pre>
     *    Entry Size             : 10000
     *    Max Entries            : 5
     *    Segment File Size      : 256MB
     *    Segment Compact Trigger: 0.1
     *    Segment Compact Factor : 0.5
     *    Hash Function          : krati.util.FnvHashFunction
     * </pre>
     * 
     * @param homeDir              the home directory
     * @param capacity             the capacity of data set
     * @param segmentFactory       the segment factory
     * @throws Exception
     */
    public SimpleDataSet(File homeDir, int capacity, SegmentFactory segmentFactory) throws Exception
    {
        this(homeDir,
             capacity,
             10000,
             5,
             256,
             segmentFactory,
             0.1, /* segment compact trigger */
             0.5, /* segment compact factor  */
             new FnvHashFunction());
    }
    
    /**
     * Creates a DataSet instance with the settings below:
     * 
     * <pre>
     *    Entry Size             : 10000
     *    Max Entries            : 5
     *    Segment Compact Trigger: 0.1
     *    Segment Compact Factor : 0.5
     *    Hash Function          : krati.util.FnvHashFunction
     * </pre>
     * 
     * @param homeDir              the home directory
     * @param capacity             the capacity of data set
     * @param segmentFileSizeMB    the size of segment file in MB
     * @param segmentFactory       the segment factory
     * @throws Exception
     */
    public SimpleDataSet(File homeDir,
                         int capacity,
                         int segmentFileSizeMB,
                         SegmentFactory segmentFactory) throws Exception
    {
        this(homeDir,
             capacity,
             10000,
             5,
             256,
             segmentFactory,
             0.1, /* segment compact trigger */
             0.5, /* segment compact factor  */
             new FnvHashFunction());
    }
    
    /**
     * Creates a DataSet instance with the settings below:
     * 
     * <pre>
     *    Segment Compact Trigger: 0.1
     *    Segment Compact Factor : 0.5
     *    Hash Function          : krati.util.FnvHashFunction
     * </pre>
     * 
     * @param homeDir              the home directory
     * @param capacity             the capacity of data set
     * @param entrySize            the redo entry size (i.e., batch size)
     * @param maxEntries           the number of redo entries required for updating the underlying address array
     * @param segmentFileSizeMB    the size of segment file in MB
     * @param segmentFactory       the segment factory
     * @throws Exception
     */
    public SimpleDataSet(File homeDir,
                         int capacity,
                         int entrySize,
                         int maxEntries,
                         int segmentFileSizeMB,
                         SegmentFactory segmentFactory) throws Exception
    {
        this(homeDir,
             capacity,
             entrySize,
             maxEntries,
             segmentFileSizeMB,
             segmentFactory,
             0.1, /* segment compact trigger */
             0.5, /* segment compact factor  */
             new FnvHashFunction());
    }
    
    /**
     * Creates a DataSet instance with the settings below:
     * 
     * <pre>
     *    Segment Compact Trigger: 0.1
     *    Segment Compact Factor : 0.5
     * </pre>
     * 
     * @param homeDir              the home directory
     * @param capacity             the capacity of data set
     * @param entrySize            the redo entry size (i.e., batch size)
     * @param maxEntries           the number of redo entries required for updating the underlying address array
     * @param segmentFileSizeMB    the size of segment file in MB
     * @param segmentFactory       the segment factory
     * @param hashFunction         the hash function for mapping values to indexes
     * @throws Exception
     */
    public SimpleDataSet(File homeDir,
                         int capacity,
                         int entrySize,
                         int maxEntries,
                         int segmentFileSizeMB,
                         SegmentFactory segmentFactory,
                         HashFunction<byte[]> hashFunction) throws Exception
    {
        this(homeDir,
             capacity,
             entrySize,
             maxEntries,
             segmentFileSizeMB,
             segmentFactory,
             0.1, /* segment compact trigger */
             0.5, /* segment compact factor  */
             hashFunction);
    }
    
    /**
     * Creates a DataSet instance.
     * 
     * @param homeDir                the home directory
     * @param capacity               the capacity of data set
     * @param entrySize              the redo entry size (i.e., batch size)
     * @param maxEntries             the number of redo entries required for updating the underlying address array
     * @param segmentFileSizeMB      the size of segment file in MB
     * @param segmentFactory         the segment factory
     * @param segmentCompactTrigger  the percentage of segment capacity, which triggers compaction once per segment
     * @param segmentCompactFactor   the load factor of segment, below which a segment is eligible for compaction
     * @param hashFunction           the hash function for mapping values to indexes
     * @throws Exception
     */
    public SimpleDataSet(File homeDir,
                         int capacity,
                         int entrySize,
                         int maxEntries,
                         int segmentFileSizeMB,
                         SegmentFactory segmentFactory,
                         double segmentCompactTrigger,
                         double segmentCompactFactor,
                         HashFunction<byte[]> hashFunction) throws Exception
    {
        _dataHandler = new DefaultSetDataHandler();
        
        // Create address array
        AddressArray addressArray = createAddressArray(capacity, entrySize, maxEntries, homeDir);
        
        if(addressArray.length() != capacity)
        {
            throw new IOException("Capacity expected: " + addressArray.length() + " not " + capacity);
        }
        
        // Create segment manager
        String segmentHome = homeDir.getCanonicalPath() + File.separator + "segs";
        SegmentManager segmentManager = SegmentManager.getInstance(segmentHome, segmentFactory, segmentFileSizeMB);
        
        this._dataArray = new SimpleDataArray(addressArray, segmentManager, segmentCompactTrigger, segmentCompactFactor);
        this._hashFunction = hashFunction;
    }
    
    protected AddressArray createAddressArray(int length,
                                              int entrySize,
                                              int maxEntries,
                                              File homeDirectory) throws Exception
    {
        return new SimpleLongArray(length, entrySize, maxEntries, homeDirectory);
    }
    
    protected long hash(byte[] value)
    {
        return _hashFunction.hash(value);
    }
    
    protected long nextScn()
    {
        return System.currentTimeMillis();
    }
    
    @Override
    public void sync() throws IOException
    {
        _dataArray.sync();
    }
    
    @Override
    public void persist() throws IOException
    {
        _dataArray.persist();
    }
    
    @Override
    public boolean has(byte[] value)
    {
        if(value == null) return false;
        
        long hashCode = hash(value);
        int index = (int)(hashCode % _dataArray.length());
        if (index < 0) index = -index;
        
        byte[] existingData = _dataArray.getData(index);
        return existingData == null ? false : _dataHandler.find(value, existingData);
    }
    
    public final int countCollisions(byte[] value)
    {
        if(value == null) return 0;
        
        long hashCode = hash(value);
        int index = (int)(hashCode % _dataArray.length());
        if (index < 0) index = -index;
        
        byte[] existingData = _dataArray.getData(index);
        return existingData == null ? 0 : _dataHandler.countCollisions(value, existingData);
    }
    
    public final boolean hasWithoutCollisions(byte[] value)
    {
        return countCollisions(value) == 1;
    }
    
    @Override
    public synchronized boolean add(byte[] value) throws Exception
    {
        if(value == null) return false;
        
        long hashCode = hash(value);
        int index = (int)(hashCode % _dataArray.length());
        if (index < 0) index = -index;
        
        byte[] existingData = _dataArray.getData(index);
        
        try
        {
            if(existingData == null || existingData.length == 0)
            {
                _dataArray.setData(index, _dataHandler.assemble(value), nextScn());
            }
            else
            {
                if(!_dataHandler.find(value, existingData))
                {
                    _dataArray.setData(index, _dataHandler.assemble(value, existingData), nextScn());
                }
            }
        }
        catch(Exception e)
        {
            _log.warn("Value reset at index="+ index + " value=\"" + new String(value) + "\"", e);
            _dataArray.setData(index, _dataHandler.assemble(value), nextScn());
        }
        
        return true;
    }
    
    @Override
    public synchronized boolean delete(byte[] value) throws Exception
    {
        long hashCode = hash(value);
        int index = (int)(hashCode % _dataArray.length());
        if (index < 0) index = -index;
        
        try
        {
            byte[] existingData = _dataArray.getData(index);
            if(existingData != null)
            {
               int newLength = _dataHandler.remove(value, existingData);
               if(newLength == 0)
               {
                   // entire data is removed
                   _dataArray.setData(index, null, nextScn());
                   return true;
               }
               else if(newLength < existingData.length)
               {
                   // partial data is removed
                   _dataArray.setData(index, existingData, 0, newLength, nextScn());
                   return true;
               }
            }
        }
        catch(Exception e)
        {
            _log.warn("Failed to delete value=\""+ new String(value) + "\"", e);
            _dataArray.setData(index, null, nextScn());
        }
        
        // no data is removed
        return false;
    }
    
    @Override
    public synchronized void clear() throws IOException
    {
        _dataArray.clear();
    }
    
    /**
     * @return the underlying data array.
     */
    public final DataArray getDataArray()
    {
        return _dataArray;
    }
}
