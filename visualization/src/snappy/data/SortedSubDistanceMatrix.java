package snappy.data;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class SortedSubDistanceMatrix implements DistanceFunction {

	private int m_numPoints = 0;
	private int m_numCols = 0;
	private int m_currentEnteredPoint = 0;
	
	int m_last_sampled_pt = 0;
	int m_last_sampled_idx = 0;
	DistanceIndexer[][] pt_data = null;
	
	boolean done_sampling = false;
	
	public void setColCount(int numCols) {
		
		m_numCols = numCols;
	}
	public void setInitialPointCount(int num_points) {
		
		m_numPoints = num_points;
		pt_data = new DistanceIndexer[ num_points ][];
	}
	
	public void addPointData(String dataLine ) {
		
		String[] entries = dataLine.split(",");

		pt_data[m_currentEnteredPoint] = new DistanceIndexer[m_numCols];
		
		int entry_num = 0;
		for ( String entry : entries ) {
						
			if( entry.length() > 0 ) {

				String[] subentries = entry.split(":");
				
				pt_data[m_currentEnteredPoint][entry_num]=new DistanceIndexer();
				pt_data[m_currentEnteredPoint][entry_num].index = Integer.parseInt(subentries[0]); 
				pt_data[m_currentEnteredPoint][entry_num].distance = Float.parseFloat(subentries[1]); 
						
				entry_num++;
			}			
		}
		
		m_currentEnteredPoint++;
	}
	
	class DistanceIndexer {
		
		public float distance;
		public int index;
	}
	
	@Override
	public int getPointCount() {

		return m_numPoints;
	}

	@Override
	public float pdist(int np_i, int np_j) {
		
		for(DistanceIndexer di : pt_data[np_i]) {
			
			if( di.index == np_j )
				return di.distance;
		}
		
		return 0;
	}

	@Override
	public SimpleEdge sampleEdge() {

		if( done_sampling )	// no more edges to sample
			return null;
		
		SimpleEdge retSE = new SimpleEdge(m_last_sampled_pt, 
							  pt_data[m_last_sampled_pt][m_last_sampled_idx].index, 
							  pt_data[m_last_sampled_pt][m_last_sampled_idx].distance);
		
		// increment sample pointers
		
		m_last_sampled_pt = (m_last_sampled_pt + 1) % m_numPoints;
		if( m_last_sampled_pt == 0 ) {
			m_last_sampled_idx++;
			if( m_last_sampled_idx >= m_numCols )
				done_sampling = true;
		}
		
		return retSE;
	}
	
	public static void main(String[] args) {
		
		if( args.length < 0 ) {
			
			System.out.println( "ERROR: Please input filename." );
			System.exit(0);
		}
		
		System.out.println("TESTING SORTED DISTANCE MATRIX.");
		
		DistanceFunction df = null;
		try {
			System.out.print( "Reading Distance Matrix ... " );
			int ptCount = DistanceReader.readDistancePointSize(new FileReader(args[0]));
			df = DistanceReader.readSortedDistanceData(new FileReader(args[0]),ptCount);
			System.out.println( "done." );
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		
		if( df == null ) {
			
			System.out.println( "ERROR: Problem reading distance matrix." );
		}
		
		System.out.println("\n [EDGES] \n");
		
		SimpleEdge simpleEdge = df.sampleEdge();
		long millis_start = System.currentTimeMillis();
		long edge_count = 0;
		
		// keep track of stats
		float[] avgs = new float[100];
		int chunk_count = 1000;
		int curr_count = 0;
		int count_idx = 0;
		
		while( (System.currentTimeMillis() - millis_start) < (5000L) && simpleEdge != null ) {
			
			curr_count = ( curr_count + 1 ) % chunk_count;
			if( curr_count == 0) {
				
				avgs[count_idx] /= (float)chunk_count;
				System.out.println(""+avgs[count_idx]);
				count_idx += 1;
				if(count_idx >= avgs.length) {
					System.exit(0);
				}				
			}
			
			avgs[count_idx] = avgs[count_idx] + simpleEdge.w;
			simpleEdge = df.sampleEdge();
			edge_count++;
		}
		
		if( simpleEdge == null ) {
			
			System.out.println("We've exhausted the edges.");
		}
		
		System.out.println("Number of edges sampled = " + edge_count);
	}

}
