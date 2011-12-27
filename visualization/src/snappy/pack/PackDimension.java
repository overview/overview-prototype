package snappy.pack;

public class PackDimension {
    public short _size =0;
    public short _index=0;

    public static PackDimension[] getfullPDArray(int size ) {
    	PackDimension[] retpdArray = new PackDimension[size];
    	for( int i = 0; i < size; i++ ) {
    		
    		retpdArray[i] = new PackDimension();
    	}
    	return retpdArray;
    }
    
    // The width of a column or the height of a row
    public int getSize()
    { 
    	return (int)_size; 
    }
    public void setSize( int value ) { 
    	_size = (short)value; 
    }
    
    // When a row or column is split, the new row is created at the end of the physical array rather than in the middle.
    // That way, there is no need to copy lots of data. But it does mean you need indirection from the logical index
    // to the physical index.
    // This field provides the physical index.
    public int getIndex()
    { 
    	return (int)_index; 
    }
    public void setIndex( int value ) { 
    	_index = (short)value; 
    }
}
