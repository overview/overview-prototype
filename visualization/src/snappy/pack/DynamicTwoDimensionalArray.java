package snappy.pack;

import java.util.Arrays;

public class DynamicTwoDimensionalArray {

    // Describe the rows and columns
    private PackDimension[] _columns;
    private PackDimension[] _rows;

    private boolean[][] _data;

    // Number of logical columns in the 2 dimensional array
    private int _nbrColumns = 0;

    // Number of logical rows in the 2 dimensional array
    private int _nbrRows = 0;

    /// <summary>
    /// Number of columns
    /// </summary>
    public int getNbrColumns() { return _nbrColumns; }

    /// <summary>
    /// Number of rows
    /// </summary>
    public int getNbrRows() { return _nbrRows; }

    /// <summary>
    /// Constructor
    /// </summary>
    public DynamicTwoDimensionalArray( )
    {

    }

    /// <summary>
    /// After you've constructed the array, you need to initialize it.
    /// 
    /// This removes any content and creates the first cell - so the array
    /// will have height is 1 and width is 1.
    /// </summary>
    /// <param name="capacityX">
    /// The array will initially have capacity for at least this many columns. 
    /// Must be greater than 0.
    /// Set to the expected maximum width of the array or greater.
    /// The array will resize itself if you make this too small, but resizing is expensive.
    /// </param>
    /// <param name="capacityY">
    /// The array will initially have capacity for at least this many rows.
    /// Must be greater than 0.
    /// Set to the expected maximum height of the array or greater.
    /// The array will resize itself if you make this too small, but resizing is expensive.
    /// </param>
    /// <param name="firstColumnWidth">
    /// Width of the first column.
    /// </param>
    /// <param name="firstRowHeight">
    /// Width of the first column.
    /// </param>
    /// <param name="firstCellValue">
    /// Width of the first column.
    /// </param>
    public void Initialize(int capacityX, int capacityY, int firstColumnWidth, int firstRowHeight, boolean firstCellValue) throws Exception
    {
        if (capacityX <= 0) { throw new Exception("capacityX cannot be 0 or smaller"); }
        if (capacityY == 0) { throw new Exception("capacityY cannot be 0 or smaller"); }

        if ((_columns == null) || (_columns.length < capacityX))
        {
            _columns = PackDimension.getfullPDArray(capacityX);
        }

        if ((_rows == null) || (_rows.length < capacityY))
        {
            _rows = PackDimension.getfullPDArray(capacityY);
        }

        if ((_data == null) || (_data.length < capacityX) || (_data[0].length < capacityY))
        {
            _data = new boolean[capacityX][capacityY];	
            for( int i = 0; i < capacityX; i++ ) 
            	for( int j = 0; j < capacityY; j++ ) 
            		_data[i][j] = false;
        }

        _nbrColumns = 1;
        _nbrRows = 1;

        _columns[0].setIndex(0);
        _columns[0].setSize(firstColumnWidth);

        _rows[0].setIndex(0);
        _rows[0].setSize(firstRowHeight);

        _data[0][0] = firstCellValue;
    }

    /// <summary>
    /// Returns the item at the given location.
    /// </summary>
    /// <param name="x"></param>
    /// <param name="y"></param>
    /// <returns></returns>
    public boolean Item(int x, int y)
    {
        return _data[_columns[x].getIndex()][_rows[y].getIndex()];
    }

    /// <summary>
    /// Sets an item to the given value
    /// </summary>
    /// <param name="x"></param>
    /// <param name="y"></param>
    /// <param name="value"></param>
    public void SetItem(int x, int y, boolean value)
    {
        _data[_columns[x].getIndex()][ _rows[y].getIndex()] = value;
    }

    public static <T> void ArrayCopy(	T[] sourceArray, 
    								int sourceIndex, 
    								T[] destArray, 
    								int destIndex, 
    								int length ) {
    	
    	while( --length >= 0 ) {
    		destArray[ destIndex + length ] = sourceArray[sourceIndex + length];
    	}
    }
    
    /// <summary>
    /// Inserts a row at location y.
    /// If y equals 2, than all rows at y=3 and higher will now have y=4 and higher.
    /// The new row will have y=3.
    /// The contents of the row at y=2 will be copied to the row at y=3.
    /// 
    /// If there is not enough capacity in the array for the additional row,
    /// than the internal data structure will be copied to a structure with twice the size
    /// (this copying is expensive).
    /// </summary>
    /// <param name="y">
    /// Identifies the row to be split.
    /// </param>
    /// <param name="heightNewRow">
    /// The height of the new row (the one at y=3 in the example).
    /// Must be smaller than the current height of the existing row.
    /// 
    /// The old row will have height = (old height of old row) - (height of new row). 
    /// </param>
    public void InsertRow(int y, int heightNewRow) throws Exception
    {
        if (y >= _nbrRows) { throw new Exception(String.format("y is %d but height is only %d", y, _nbrRows)); } 

        // If there are as many phyiscal rows as there are logical rows, we need to get more physical rows before the number
        // of logical rows can be increased.
        if (_data[0].length == _nbrRows) { IncreaseCapacity(); }

        // Copy the cells with the given y to a new row after the last used row. The y of the new row equals _nbrRows.
        int physicalY = _rows[y].getIndex();
        for (int x = 0; x < _nbrColumns; x++)
        {
            _data[x][ _nbrRows] = _data[x][ physicalY];
        }

        // Make room in the _rows array by shifting all items that come after the one indexed by y one position to the right.
        // If y is at the end of the array, there is no need to shift anything.
        if (y < (_nbrRows - 1)) {
        	for( int i = (_nbrRows - y - 2); i >= 0; i-- ) {
        		_rows[(y+2)+i]._index = _rows[((y+2)+i)-1]._index;
        		_rows[(y+2)+i]._size = _rows[((y+2)+i)-1]._size;
        	}
        	//ArrayCopy(_rows, y + 1, _rows, y + 2, (_nbrRows - y - 1)); 
        }

        // Let the freed up element point at the newly copied row 
        _rows[y + 1].setIndex( _nbrRows );

        // Set the heights of the old and new rows.
        int oldHeight = _rows[y].getSize();
        int newHeightExistingRow = oldHeight - heightNewRow;
        //Debug.Assert((heightNewRow > 0) && (newHeightExistingRow > 0));
        _rows[y + 1].setSize( heightNewRow );
        _rows[y].setSize( newHeightExistingRow );

        // The logical height of the array has increased by 1.
        _nbrRows++;
    }

    /// <summary>
    /// Same as InsertRow, but than for columns.
    /// </summary>
    /// <param name="x"></param>
    public void InsertColumn(int x, int widthNewColumn) throws Exception
    {
        if (x >= _nbrColumns) { throw new Exception(String.format("x is %d but width is only %d", x, _nbrColumns)); } 

        // If there are as many phyiscal columns as there are logical columns, we need to get more physical columns before the number
        // of logical columns can be increased.
        if (_data.length == _nbrColumns) { IncreaseCapacity(); }

        // Copy the cells with the given x to a new column after the last used column. The x of the new column equals _nbrColumns.
        int physicalX = _columns[x].getIndex();
        for (int y = 0; y < _nbrRows; y++)
        {
            _data[_nbrColumns][ y] = _data[physicalX][ y];
        }

        // Make room in the _columns array by shifting all items that come after the one indexed by x one position to the right.
        // If x is at the end of the array, there is no need to shift anything.
        if (x < (_nbrColumns - 1)) { 
        	for( int i = (_nbrColumns - x - 2); i >= 0; i-- ) {
        		_columns[(x+2)+i]._index = _columns[((x+2)+i)-1]._index;
        		_columns[(x+2)+i]._size = _columns[((x+2)+i)-1]._size;
        	}
        	//ArrayCopy(_columns, x + 1, _columns, x + 2, (_nbrColumns - x - 1)); 
        }

        // Let the freed up element point at the newly copied column 
        _columns[x + 1].setIndex( _nbrColumns );

        // Set the widths of the old and new columns.
        int oldWidth = _columns[x].getSize();
        int newWidthExistingColumn = oldWidth - widthNewColumn;
        _columns[x + 1].setSize( widthNewColumn );
        _columns[x].setSize(newWidthExistingColumn);

        // The logical width of the array has increased by 1.
        _nbrColumns++;

    }

    /// <summary>
    /// Returns the width of the column at the given location
    /// </summary>
    /// <param name="y"></param>
    /// <returns></returns>
    public int ColumnWidth(int x)
    {
        return _columns[x].getSize();
    }

    /// <summary>
    /// Returns the height of the row at the given location
    /// </summary>
    /// <param name="y"></param>
    /// <returns></returns>
    public int RowHeight(int y)
    {
        return _rows[y].getSize();
    }

    /// <summary>
    /// Doubles the capacity of the internal data structures.
    /// 
    /// Creates a new array with double the width and height of the old array.
    /// Copies the element of the old array to the new array.
    /// Then replaces the old array with the new array.
    /// </summary>
    private void IncreaseCapacity()
    {
        int oldCapacityX = _data.length;
        int oldCapacityY = _data[0].length;

        int newCapacityX = oldCapacityX * 2;
        int newCapacityY = oldCapacityY * 2;

        boolean[][] newData = new boolean[newCapacityX][newCapacityY];
        for( int i = 0; i < oldCapacityX; i++ ) {
        	for( int j = 0; j < oldCapacityY; j++ ) {
        		newData[i][j]=_data[i][j];
        	}
        }
        for( int i = 0; i < newCapacityX; i++ ) {
        	for( int j = 0; j < newCapacityY; j++ ) {
        		if( i < oldCapacityX && j < oldCapacityY )
        			newData[i][j]=_data[i][j];
				else
        			newData[i][j]=false;
        	}
        }

        _data = newData;
    }

    /// <summary>
    /// Represents the DynamicTowDimensionalArray as a string.
    /// </summary>
    /// <returns></returns>
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");

        sb.append(" X      ");
        for (int x = 0; x < _nbrColumns + 1; x++) { sb.append(String.format("   %02d ", x)); }
        sb.append("\n");

        sb.append("Y       ");
        for (int x = 0; x < _nbrColumns + 1; x++) { sb.append(String.format(" (%03d)", ColumnWidth(x))); }
        sb.append("\n");

        for (int y = 0; y < _nbrRows + 1; y++)
        {
            sb.append(String.format("%02d %03d) ", y, RowHeight(y)));

            for (int x = 0; x < _nbrColumns + 1; x++)
            {
                sb.append(String.format("   %s  ", Item(x, y)));
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
