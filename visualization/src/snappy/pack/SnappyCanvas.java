package snappy.pack;

public class SnappyCanvas implements ICanvas {

    private DynamicTwoDimensionalArray _canvasCells = null;

    // Make _canvasCells available to canvas classes derived from this class.
    protected DynamicTwoDimensionalArray getCanvasCells() { return _canvasCells; }

    private int _nbrRectangleAddAttempts = 0;
    public int getNbrRectangleAddAttempts(){ return _nbrRectangleAddAttempts; }

    private int _canvasWidth = 500;
    private int _canvasHeight = 500;

    // Lowest free height deficit found since the last call to SetCanvasDimension
    private int _lowestFreeHeightDeficitSinceLastRedim = Integer.MAX_VALUE;

    private int _nbrCellsGenerated = 0;

    public SnappyCanvas()
    {
    	_canvasCells = new DynamicTwoDimensionalArray(  );
    }

    /// <summary>
    /// See ICanvas
    /// </summary>
    public int getUnlimitedSize() { 
//    	return Short.MAX_VALUE; 
    	return 2000; 
    }

    /// <summary>
    /// See ICanvas
    /// </summary>
    public void SetCanvasDimensions(int canvasWidth, int canvasHeight)
    {
        // Right now, it is unknown how many rectangles need to be placed.
        // So guess that a 100 by 100 capacity will be enough.
        int initialCapacityX = 2000;
        int initialCapacityY = 2000;

        // Initially, there is one free cell, which covers the entire canvas.
        try {
			_canvasCells.Initialize(initialCapacityX, initialCapacityY, canvasWidth, canvasHeight, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        _nbrCellsGenerated = 0;
        _nbrRectangleAddAttempts = 0;
        _lowestFreeHeightDeficitSinceLastRedim = Integer.MAX_VALUE;

        _canvasWidth = canvasWidth;
        _canvasHeight = canvasHeight;
    }

    /// <summary>
    /// See ICanvas.
    /// </summary>
    public boolean addRectangle(
        int rectangleWidth, int rectangleHeight, OutInteger rectangleXOffset, OutInteger rectangleYOffset,
        OutInteger lowestFreeHeightDeficit)
    {
        rectangleXOffset.setValue(0);
        rectangleYOffset.setValue(0);
        lowestFreeHeightDeficit.setValue(Integer.MAX_VALUE);

        int requiredWidth = rectangleWidth;
        int requiredHeight = rectangleHeight;

        _nbrRectangleAddAttempts++;

        int x = 0;
        int y = 0;
        int offsetX = 0;
        int offsetY = 0;
        boolean rectangleWasPlaced = false;
        int nbrRows = _canvasCells.getNbrRows();

        do
        {
            int nbrRequiredCellsHorizontally=0;
            int nbrRequiredCellsVertically=0;
            int leftOverWidth=0;
            int leftOverHeight=0;

            // First move upwards until we find an unoccupied cell. 
            // If we're already at an unoccupied cell, no need to do anything.
            // Important to clear all occupied cells to get 
            // the lowest free height deficit. This must be taken from the top of the highest 
            // occupied cell.

            while ((y < nbrRows) && (_canvasCells.Item(x, y)))
            {
                offsetY += _canvasCells.RowHeight(y);
                y += 1;
            }

            // If we found an unoccupied cell, than see if we can place a rectangle there.
            // If not, than y popped out of the top of the canvas.

            if ((y < nbrRows) && (FreeHeightDeficit(_canvasHeight, offsetY, requiredHeight) <= 0))
            {
            	OutInteger outnbrRequiredCellsHorizontally = new OutInteger(nbrRequiredCellsHorizontally);
            	OutInteger outnbrRequiredCellsVertically = new OutInteger(nbrRequiredCellsVertically);
            	OutInteger outleftOverWidth = new OutInteger(leftOverWidth);
            	OutInteger outleftOverHeight = new OutInteger(leftOverHeight);
            	
            	boolean isAvailableTest = IsAvailable(
                        x, y, requiredWidth, requiredHeight,
                        outnbrRequiredCellsHorizontally, outnbrRequiredCellsVertically,
                        outleftOverWidth, outleftOverHeight);
            	
            	nbrRequiredCellsHorizontally = outnbrRequiredCellsHorizontally.x;
            	nbrRequiredCellsVertically = outnbrRequiredCellsVertically.x;
            	leftOverWidth = outleftOverWidth.x;
            	leftOverHeight = outleftOverHeight.x;
            	
                if (isAvailableTest)
                {
                    try {
						PlaceRectangle(
						    x, y, requiredWidth, requiredHeight,
						    nbrRequiredCellsHorizontally, nbrRequiredCellsVertically,
						    leftOverWidth, leftOverHeight);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

                    rectangleXOffset.setValue(offsetX);
                    rectangleYOffset.setValue(offsetY);

                    rectangleWasPlaced = true;
                    break;
                }

                // Go to the next cell
                offsetY += _canvasCells.RowHeight(y);
                y += 1;
            }

            // If we've come so close to the top of the canvas that there is no space for the
            // rectangle, go to the next column. This automatically also checks whether we've popped out of the top
            // of the canvas (in that case, _canvasHeight == offsetY).

            int freeHeightDeficit = FreeHeightDeficit(_canvasHeight, offsetY, requiredHeight);
            if (freeHeightDeficit > 0)
            {
                offsetY = 0;
                y = 0;

                offsetX += _canvasCells.ColumnWidth(x);
                x += 1;

                // This update is far from perfect, because if the rectangle could not be placed at this column
                // because of insufficient horizontal space, than this update should not be made (because it may lower
                // _lowestFreeHeightDeficitSinceLastRedim while in raising the height of the canvas by this lowered amount
                // may not result in the rectangle being placed here after all.
                //
                // However, checking for sufficient horizontal width takes a lot of CPU ticks. Tests have shown that this
                // far outstrips the gains through having fewer failed sprite generations.
                if (_lowestFreeHeightDeficitSinceLastRedim > freeHeightDeficit) { _lowestFreeHeightDeficitSinceLastRedim = freeHeightDeficit;  }
            }

            // If we've come so close to the right edge of the canvas that there is no space for
            // the rectangle, return false now.
            if ((_canvasWidth - offsetX) < requiredWidth)
            {
                rectangleWasPlaced = false;
                break;
            }
        } while (true);

        lowestFreeHeightDeficit.setValue( _lowestFreeHeightDeficitSinceLastRedim );
        
        return rectangleWasPlaced;
    }

    /// <summary>
    /// Works out the free height deficit when placing a rectangle with a required height at a given offset.
    /// 
    /// If the free height deficit is 0 or negative, there may be room to place the rectangle (still need to check for blocking
    /// occupied cells).
    /// 
    /// If the free height deficit is greater than 0, you're too close to the top edge of the canvas to place the rectangle.
    /// </summary>
    /// <param name="canvasHeight"></param>
    /// <param name="offsetY"></param>
    /// <param name="requiredHeight"></param>
    /// <returns></returns>
    private int FreeHeightDeficit(int canvasHeight, int offsetY, int requiredHeight)
    {
        int spaceLeftVertically = canvasHeight - offsetY;
        int freeHeightDeficit = requiredHeight - spaceLeftVertically;

        return freeHeightDeficit;
    }


    /// <summary>
    /// Sets the cell at x,y to occupied, and also its top and right neighbours, as needed
    /// to place a rectangle with the given width and height.
    /// 
    /// If the rectangle takes only part of a row or column, they are split.
    /// </summary>
    /// <param name="x"></param>
    /// <param name="y"></param>
    /// <param name="requiredWidth"></param>
    /// <param name="requiredHeight"></param>
    /// <param name="nbrRequiredCellsHorizontally">
    /// Number of cells that the rectangle requires horizontally
    /// </param>
    /// <param name="nbrRequiredCellsVertically">
    /// Number of cells that the rectangle requires vertically
    /// </param>
    /// <param name="leftOverWidth">
    /// The amount of horizontal space left in the right most cells that could be used for the rectangle
    /// </param>
    /// <param name="leftOverHeight">
    /// The amount of vertical space left in the bottom most cells that could be used for the rectangle
    /// </param>
    private void PlaceRectangle(
        int x, int y, 
        int requiredWidth, int requiredHeight,
        int nbrRequiredCellsHorizontally, int nbrRequiredCellsVertically,
        int leftOverWidth,
        int leftOverHeight) throws Exception
    {
        // Split the far most row and column if needed.

        if (leftOverWidth > 0)
        {
            _nbrCellsGenerated += _canvasCells.getNbrRows();

            int xFarRightColumn = x + nbrRequiredCellsHorizontally - 1;
            _canvasCells.InsertColumn(xFarRightColumn, leftOverWidth);
        }

        if (leftOverHeight > 0)
        {
            _nbrCellsGenerated += _canvasCells.getNbrColumns();

            int yFarBottomColumn = y + nbrRequiredCellsVertically - 1;
            _canvasCells.InsertRow(yFarBottomColumn, leftOverHeight);
        }

        for (int i = x + nbrRequiredCellsHorizontally - 1; i >= x; i--)
        {
            for (int j = y + nbrRequiredCellsVertically - 1; j >= y; j--)
            {
                _canvasCells.SetItem(i, j, true);
            }
        }
    }

    /// <summary>
    /// Returns true if a rectangle with the given width and height can be placed
    /// in the cell with the given x and y, and its right and top neighbours.
    /// 
    /// This method assumes that x,y is far away enough from the edges of the canvas
    /// that the rectangle could actually fit. So this method only looks at whether cells
    /// are occupied or not.
    /// </summary>
    /// <param name="x"></param>
    /// <param name="y"></param>
    /// <param name="requiredWidth"></param>
    /// <param name="requiredHeight"></param>
    /// <param name="nbrRequiredCellsHorizontally">
    /// Number of cells that the rectangle requires horizontally
    /// </param>
    /// <param name="nbrRequiredCellsVertically">
    /// Number of cells that the rectangle requires vertically
    /// </param>
    /// <param name="leftOverWidth">
    /// The amount of horizontal space left in the right most cells that could be used for the rectangle
    /// </param>
    /// <param name="leftOverHeight">
    /// The amount of vertical space left in the bottom most cells that could be used for the rectangle
    /// </param>
    /// <returns></returns>
    private boolean IsAvailable(
        int x, int y, int requiredWidth, int requiredHeight, 
        OutInteger nbrRequiredCellsHorizontally,
        OutInteger nbrRequiredCellsVertically,
        OutInteger leftOverWidth,
        OutInteger leftOverHeight)
    {
        nbrRequiredCellsHorizontally.setValue(0);
        nbrRequiredCellsVertically.setValue(0);
        leftOverWidth.setValue(0);
        leftOverHeight.setValue(0);

        int foundWidth = 0;
        int foundHeight = 0;
        int trialX = x;
        int trialY = y;

        // Check all cells that need to be unoccupied for there to be room for the rectangle.

        while (foundHeight < requiredHeight)
        {
            trialX = x;
            foundWidth = 0;

            while (foundWidth < requiredWidth)
            {
                if (_canvasCells.Item(trialX, trialY))
                {
                    return false;
                }

                foundWidth += _canvasCells.ColumnWidth(trialX);
                trialX++;
            }

            foundHeight += _canvasCells.RowHeight(trialY);
            trialY++;
        }

        // Visited all cells that we'll need to place the rectangle,
        // and none were occupied. So the space is available here.

        nbrRequiredCellsHorizontally.setValue( trialX - x );
        nbrRequiredCellsVertically.setValue( trialY - y );

        leftOverWidth.setValue(foundWidth - requiredWidth);
        leftOverHeight.setValue(foundHeight - requiredHeight);

        return true;
    }

    /// <summary>
    /// See ICanvas
    /// </summary>
    /// <param name="canvasStats"></param>
    public void GetStatistics(ICanvasStats canvasStats)
    {
        canvasStats.setNbrCellsGenerated( _nbrCellsGenerated );
        canvasStats.setRectangleAddAttempts( _nbrRectangleAddAttempts );
        canvasStats.setLowestFreeHeightDeficit( _lowestFreeHeightDeficitSinceLastRedim );
    }
	
}
