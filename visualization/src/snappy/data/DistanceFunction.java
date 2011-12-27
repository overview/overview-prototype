package snappy.data;

public interface DistanceFunction {
	
	/**
	 * Gets the number of points in the data set
	 * @return
	 */
	public int getPointCount();
	
	/**
	 * Returns the next point in the data set 
	 */
	public float pdist( int np_i, int np_j );
	
	/**
	 * Samples an edge.  Uses a simple sampling strategy that increases the 
	 * probability of sampling smaller edges before larger edges
	 * @return
	 */
	public SimpleEdge sampleEdge();
}
