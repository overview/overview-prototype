package snappy.pack;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;

public class MapperOptimalEfficiency<T extends ISprite> extends MapperOptimalEfficiencyBase<T> {

	/// <summary>
    /// See MapperIterative_Base
    /// </summary>
    /// <param name="canvas">
    /// Canvas to be used by the Mapping method.
    /// </param>
    public MapperOptimalEfficiency(ICanvas canvas, T foo)
    {
    	super(canvas, foo);
    }

    /// <summary>
    /// See MapperIterative_Base
    /// </summary>
    /// <param name="canvas">
    /// Canvas to be used by the Mapping method.
    /// </param>
    /// <param name="cutoffEfficiency">
    /// The Mapping method will stop trying to get a better solution once it has found a solution
    /// with this efficiency.
    /// </param>
    /// <param name="maxNbrCandidateSprites">
    /// The Mapping method will stop trying to get a better solution once it has generated this many
    /// candidate sprites.
    /// </param>
    public MapperOptimalEfficiency(ICanvas canvas, float cutoffEfficiency, int maxNbrCandidateSprites, T foo )
    {
    	super( canvas, cutoffEfficiency, maxNbrCandidateSprites, foo );
    }

    /// <summary>
    /// See IMapper.
    /// </summary>
    /// <param name="images"></param>
    /// <returns></returns>
    public T Mapping(Iterable<IImageInfo> images, IMapperStats mapperStats)
    {
        int candidateSpriteFails = 0;
        int candidateSpritesGenerated = 0;
        int canvasRectangleAddAttempts = 0;
        int canvasNbrCellsGenerated = 0;

        // Sort the images by height descending
        
        
        PriorityQueue<IImageInfo> imageInfosHighestFirst = new PriorityQueue<IImageInfo>( 100, new Comparator<IImageInfo>() {
        	public int compare(IImageInfo i1, IImageInfo i2 ) {
        		return -(i1.getHeight()-i2.getHeight());
        	}
        } );
        
        
        int totalAreaAllImages = 0;
        int widthWidestImage = Integer.MIN_VALUE;
        for( IImageInfo image : images ) {
        	totalAreaAllImages += image.getHeight() * image.getWidth();
        	widthWidestImage = Math.max( image.getWidth(), widthWidestImage );
        	imageInfosHighestFirst.add(image);
        }
        int heightHighestImage = imageInfosHighestFirst.peek().getHeight();

        T bestSprite = null;

        int canvasMaxWidth = this.getCanvas().getUnlimitedSize();
        int canvasMaxHeight = heightHighestImage;

        int lowestFreeHeightDeficitTallestRightFlushedImage=0;
        
        System.out.println("canvasMaxWidth = " + canvasMaxWidth);
        System.out.println("widthWidestImage = " + widthWidestImage);
        
        while (canvasMaxWidth >= widthWidestImage)
        {
            SnappyCanvasStats canvasStats = new SnappyCanvasStats();
            OutInteger outlowestFreeHeightDeficitTallestRightFlushedImage = 
            	new OutInteger(lowestFreeHeightDeficitTallestRightFlushedImage);
            T spriteInfo = 
                MappingRestrictedBox(imageInfosHighestFirst, 
                		canvasMaxWidth, canvasMaxHeight, 
                		canvasStats, outlowestFreeHeightDeficitTallestRightFlushedImage);
            lowestFreeHeightDeficitTallestRightFlushedImage = 
            	outlowestFreeHeightDeficitTallestRightFlushedImage.x;

            canvasRectangleAddAttempts += canvasStats.getRectangleAddAttempts();
            canvasNbrCellsGenerated += canvasStats.getNbrCellsGenerated();

            if (spriteInfo == null)
            {            	
                // Failure - Couldn't generate a SpriteInfo with the given maximum canvas dimensions

                candidateSpriteFails++;

                // Try again with a greater max height. Add enough height so that 
                // you don't get the same rectangle placement as this time.

                if (canvasStats.getLowestFreeHeightDeficit() == Integer.MAX_VALUE)
                {
                    canvasMaxHeight++;
                }
                else
                {
                    canvasMaxHeight += canvasStats.getLowestFreeHeightDeficit();
                }
                continue;
            }
            else
            {
                // Success - Managed to generate a SpriteInfo with the given maximum canvas dimensions

                candidateSpritesGenerated++;

                // Find out if the new SpriteInfo is better than the current best one
                if ((bestSprite == null) || (bestSprite.getArea() > spriteInfo.getArea()))
                {
                    bestSprite = spriteInfo;

                    float bestEfficiency = (float)totalAreaAllImages / spriteInfo.getArea();
                    if (bestEfficiency >= this.getCutoffEfficiency()) { break; }
                }

                if (candidateSpritesGenerated >= this.getMaxNbrCandidateSprites()) { break; }

                // Try again with a reduce maximum canvas width, to see if we can squeeze out a smaller sprite
                // Note that in this algorithm, the maximum canvas width is never increased, so a new sprite
                // always has the same or a lower width than an older sprite.
                canvasMaxWidth = bestSprite.getWidth() - 1;

                // Now that we've decreased the width of the canvas to 1 pixel less than the width
                // taken by the images on the canvas, we know for sure that the images whose
                // right borders are most to the right will have to move up.
                //
                // To make sure that the next try is not automatically a failure, increase the height of the 
                // canvas sufficiently for the tallest right flushed image to be placed. Note that when
                // images are placed sorted by highest first, it will be the tallest right flushed image
                // that will fail to be placed if we don't increase the height of the canvas sufficiently.

                if (lowestFreeHeightDeficitTallestRightFlushedImage == Integer.MAX_VALUE)
                {
                    canvasMaxHeight++;
                }
                else
                {
                    canvasMaxHeight += lowestFreeHeightDeficitTallestRightFlushedImage;
                }
            }

            // ---------------------
            // Adjust max canvas width and height to cut out sprites that we'll never accept

            int bestSpriteArea = bestSprite.getArea();
            boolean candidateBiggerThanBestSprite=false;
            boolean candidateSmallerThanCombinedImages=false;
            OutBoolean outcandidateBiggerThanBestSprite =
            	new OutBoolean(candidateBiggerThanBestSprite);
            OutBoolean outcandidateSmallerThanCombinedImages =
            	new OutBoolean(candidateSmallerThanCombinedImages);
            boolean cond_CandidateCanvasFeasable = CandidateCanvasFeasable(
                    canvasMaxWidth, canvasMaxHeight, bestSpriteArea, totalAreaAllImages,
                    outcandidateBiggerThanBestSprite, outcandidateSmallerThanCombinedImages);
            candidateBiggerThanBestSprite = outcandidateBiggerThanBestSprite.x;
            candidateSmallerThanCombinedImages = outcandidateSmallerThanCombinedImages.x;
            while (
                (canvasMaxWidth >= widthWidestImage) &&
                (!cond_CandidateCanvasFeasable) )
            {
                if (candidateBiggerThanBestSprite) { canvasMaxWidth--; }
                if (candidateSmallerThanCombinedImages) { canvasMaxHeight++; }
                
                outcandidateBiggerThanBestSprite.x = candidateBiggerThanBestSprite;
                outcandidateSmallerThanCombinedImages.x = candidateSmallerThanCombinedImages;
                cond_CandidateCanvasFeasable = CandidateCanvasFeasable(
                        canvasMaxWidth, canvasMaxHeight, bestSpriteArea, totalAreaAllImages,
                        outcandidateBiggerThanBestSprite, outcandidateSmallerThanCombinedImages);
                candidateBiggerThanBestSprite = outcandidateBiggerThanBestSprite.x;
                candidateSmallerThanCombinedImages = outcandidateSmallerThanCombinedImages.x;
            }
        }

        if (mapperStats != null)
        {
            mapperStats.setCandidateSpriteFails( candidateSpriteFails );
            mapperStats.setCandidateSpritesGenerated( candidateSpritesGenerated );
            mapperStats.setCanvasNbrCellsGenerated( canvasNbrCellsGenerated );
            mapperStats.setCanvasRectangleAddAttempts( canvasRectangleAddAttempts );
        }

        return bestSprite;
    }

    /// <summary>
    /// Works out whether there is any point in trying to fit the images on a canvas
    /// with the given width and height.
    /// </summary>
    /// <param name="canvasMaxWidth">Candidate canvas width</param>
    /// <param name="canvasMaxHeight">Candidate canvas height</param>
    /// <param name="bestSpriteArea">Area of the smallest sprite produces so far</param>
    /// <param name="totalAreaAllImages">Total area of all images</param>
    /// <param name="candidateBiggerThanBestSprite">true if the candidate canvas is bigger than the best sprite so far</param>
    /// <param name="candidateSmallerThanCombinedImages">true if the candidate canvas is smaller than the combined images</param>
    /// <returns></returns>
    protected boolean CandidateCanvasFeasable(
        int canvasMaxWidth, int canvasMaxHeight, int bestSpriteArea, int totalAreaAllImages,
        OutBoolean candidateBiggerThanBestSprite, OutBoolean candidateSmallerThanCombinedImages)
    {
        int candidateArea = canvasMaxWidth * canvasMaxHeight;
        candidateBiggerThanBestSprite.x = (candidateArea > bestSpriteArea);
        candidateSmallerThanCombinedImages.x = (candidateArea < totalAreaAllImages);

        return !(candidateBiggerThanBestSprite.x || candidateSmallerThanCombinedImages.x);
    }

}
