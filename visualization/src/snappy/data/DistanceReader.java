package snappy.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

public class DistanceReader {
	
	public static int readDistancePointSize( Reader reader ) {
		try{
			
			BufferedReader breader = new BufferedReader( reader );
			
			String lineStr = breader.readLine();
			int line_numbers=0;
			while( lineStr != null && lineStr.length() > 0 ) {
			
				line_numbers++;
				lineStr = breader.readLine();
			}
			
			return line_numbers;
		}
		catch(Exception e) {
			
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public static DistanceFunction readSortedDistanceData( Reader reader, int num_points ) {
		
		try{
			
			BufferedReader breader = new BufferedReader( reader );
			SortedDistanceMatrix df = new SortedDistanceMatrix();			
			
			df.setInitialPointCount(num_points);
			
			String lineStr = breader.readLine();
			while( lineStr != null && lineStr.length() > 0 ) {
				
				df.addPointData( lineStr );				
				lineStr = breader.readLine();
			}
			
			breader.close();
			return df;
		}
		catch(Exception e) {
			
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	public static DistanceFunction readSortedSubDistanceData( Reader reader, int num_points ) {
		
		try{
			
			BufferedReader breader = new BufferedReader( reader );
			SortedSubDistanceMatrix df = new SortedSubDistanceMatrix();			
			
			df.setInitialPointCount(num_points);
			
			String lineStr = breader.readLine();
			
			// count the number of fields
			df.setColCount(lineStr.split(",").length);			
			
			while( lineStr != null && lineStr.length() > 0 ) {
				
				df.addPointData( lineStr );				
				lineStr = breader.readLine();
			}
			
			breader.close();
			return df;
		}
		catch(Exception e) {
			
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	public static DistanceFunction readFullDistanceData(  Reader reader ) {
		
		try{
			
			BufferedReader breader = new BufferedReader( reader );
			FullDistanceMatrix df = new FullDistanceMatrix();			
			
			String lineStr = breader.readLine();
			int line_numbers=0;
			while( lineStr != null && lineStr.length() > 0 ) {
			
				line_numbers++;
				lineStr = breader.readLine();
			}
			df.setInitialPointCount(line_numbers);
			
			breader.reset();
			lineStr = breader.readLine();
			while( lineStr != null && lineStr.length() > 0 ) {
				
				df.addPointData( lineStr );				
				lineStr = breader.readLine();
			}
			
			breader.close();
			return df;
		}
		catch(Exception e) {
			
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void main( String[] args ) {
		
		if( args.length < 0 ) {
			
			System.out.println( "ERROR: Please input filename." );
		}
		
		NZData nzd = null;
		
		try {
			
			nzd = SparseReader.readNZData( new FileReader( args[0] ) );
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		
		if( nzd == null ) {
			
			System.out.println( "ERROR: Problem reading nonzero data." );
		}
		
		//System.out.println( "" + nzd );
		
		System.out.println("\n [EDGES] \n");
		
		SimpleEdge simpleEdge = nzd.sampleEdge();
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
			simpleEdge = nzd.sampleEdge();
			edge_count++;
		}
		
		if( simpleEdge == null ) {
			
			System.out.println("We've exhausted the edges.");
		}
		
		System.out.println("Number of edges sampled = " + edge_count);
	}	
}
