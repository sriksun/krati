package krati.cds.store;

public interface SetDataHandler
{
    public int count(byte[] data);
    
    public byte[] assemble(byte[] value);
    
    public byte[] assemble(byte[] value, byte[] data);
    
    public int countCollisions(byte[] value, byte[] data);
    
    public int remove(byte[] value, byte[] data);
    
    public boolean find(byte[] value, byte[] data);
}
