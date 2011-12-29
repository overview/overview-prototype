package snappy.data;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Data structure representing the sparse dimension data.
 * 
 * @author sfingram
 *
 */
public class NZData {
	
	public class NZTEntryComparator implements Comparator<NZTransposeEntry> {

		@Override
		public int compare(NZTransposeEntry o1, NZTransposeEntry o2) {

			return (o2.value-o1.value)>0.f?1:-1;
		}
		
	}
	public class NZEntryComparator implements Comparator<NZEntry> {

		@Override
		public int compare(NZEntry o1, NZEntry o2) {
						
			if( o1.dimension == o2.dimension && o1.value == o2.value ) 
				return 0;
			
			return ((o2.dim_remains?(o2.alt_dim_val*o2.value):Float.NEGATIVE_INFINITY) - 
					(o1.dim_remains?(o1.alt_dim_val*o1.value):Float.NEGATIVE_INFINITY))>0.f?1:-1;
		}		
	}
	public class NZEntryComparatorSimple implements Comparator<NZEntry> {

		@Override
		public int compare(NZEntry o1, NZEntry o2) {
						
			return (o1.dimension - o2.dimension)>0?1:-1;
		}		
	}
	public class NZTransposeEntry {
		
		public int point;
		public float value;
		public int nextp = 0;
		
		public NZTransposeEntry( int point, float value ) {
			
			this.point = point;
			this.value = value;
		}
		
		public String toString() {
		
			return String.format("(%d,%f)", point, value);
		}
	}
	
	public class NZEntry {
	
		public int dimension;
		public float value;
		public int dim_ptr = 0;
		public float alt_dim_val = 0.f;
		public boolean dim_remains = true;
		
		/**
		 * Create a nonzero entry by parsing it from a string (dim,value)
		 * 
		 * @param entry
		 */
		public NZEntry( String entry ) {
			
			String lessparens 	= entry.substring(1, entry.length()-1);
			int pos 			= lessparens.indexOf(',');
			
			dimension 	= Integer.parseInt( lessparens.substring(0,pos) ) - 1;
			value 		= Float.parseFloat(lessparens.substring(pos+1) );
		}
		
		public String toString() {
			
			//return String.format("(%d,%f)", dimension, value);
			return "("+dimension+","+value+")";
		}
		
		public NZEntry( int dimension, float value ) {
			
			this.dimension = dimension;
			this.value = value;
		}
	}
	
	private int samplerIdx = 0;
	private ArrayList<Integer> currSamplePoints;
	
	int iter_count = 0;
	int dimensionCount = 0;
	int pointCount  = 0;
	
	public ArrayList<ArrayList<NZEntry>> nzEntryDataUnordered = null;
	public ArrayList<String> nzDocIDs = null;
	ArrayList<TreeSet<NZEntry>> nzEntryData = null;
	HashMap<Integer,ArrayList<NZTransposeEntry>> nzEntryTransposeData = null;
	ArrayList<HashMap<Integer,Boolean>> edgeChecker = null;
	
	/*
	 * Generates a uniformly sampled random histogram of the distance matrix with 
	 * bin width = 1 / bin_count
	 */
	public int[] uniformHistogram( int bin_count, int sample_count ) {
		
		int[] local_bins = new int[bin_count];
		
		for( int i = 0; i < sample_count; i++ ) {
			
			int n_i = (int)Math.floor(Math.random()*pointCount);
			int n_j = (int)Math.floor(Math.random()*pointCount);
			if( n_i == n_j ) 
				continue;
			float p = pdist(n_i,n_j);
			int n_bin_idx = Math.max((int)Math.ceil(bin_count * p)-1, 0);
//			System.out.println("p = "+ p);
			local_bins[n_bin_idx]++;
		}
		
		
		return local_bins;
	}
	
	public NZData( ) {
		
		nzEntryDataUnordered    = new ArrayList<ArrayList<NZEntry>>();
		nzDocIDs 		= new ArrayList<String>();
		nzEntryTransposeData 	= new HashMap<Integer,ArrayList<NZTransposeEntry>>();
		nzEntryData 	= new ArrayList<TreeSet<NZEntry>>();
		pointCount 		= nzEntryData.size();
		dimensionCount 	= 0;
	}
	
	/**
	 * Call this function after finishing adding points to the data structure.
	 */
	public void initEntryLists() {
		
		edgeChecker = new ArrayList<HashMap<Integer,Boolean>>();
		
		nzEntryData.clear();

		for( int key : nzEntryTransposeData.keySet() ) {

			Collections.sort(nzEntryTransposeData.get(key), new NZTEntryComparator() );
		}
		
		
		currSamplePoints = new ArrayList<Integer>();
		int i = 0;
		for( ArrayList<NZEntry> entries : nzEntryDataUnordered ) {
			
			edgeChecker.add(new HashMap<Integer,Boolean>());
			currSamplePoints.add(i);
			i++;
			
			TreeSet<NZEntry> newEntry = new TreeSet<NZEntry>( new NZEntryComparator() ); 
			for( NZEntry entry : entries ) {
				
				entry.dim_ptr = 0;
				entry.alt_dim_val = nzEntryTransposeData.get(entry.dimension).get(0).value;
				newEntry.add(entry);
			}
			nzEntryData.add( newEntry );
		}
	}
	
	/**
	 * Adds a new point to the nonzero data (does not update the sorted entry lists)  
	 * you'll need to call initEntryLists before using the newly added data
	 * 
	 * This is where parsing of the .vec data file happens
	 * 
	 * @param nzDataString
	 */
	public void addPointData( String nzDataString ) {

		// update the sparse table structure

		ArrayList<NZEntry> pointDataUnordered = new ArrayList<NZEntry>();
		
		String[] entries = nzDataString.split("\\s+");
		String docID = entries[0];							// first token is doc ID string 
		for (int i=1; i<entries.length; i++) {				// start at 1, skip first entry since it was the doc ID
			
			String entry = entries[i];
			if( entry.length() > 0 ) {
				pointDataUnordered.add( new NZEntry( entry ) );
			}
		}
		if( pointDataUnordered.size() == 0 )
			return;
		
		Collections.sort( pointDataUnordered, new NZEntryComparatorSimple() );
		nzEntryDataUnordered.add(pointDataUnordered);
		pointCount = nzEntryDataUnordered.size();
		nzDocIDs.add(docID);
		//System.out.println("Read doc ID:" + docID.toString());
		
		// update the sparse transpose structure
		
		for( NZEntry nze : pointDataUnordered ) {
			
			if( ! nzEntryTransposeData.containsKey(nze.dimension) ) {
				
				nzEntryTransposeData.put(nze.dimension, new ArrayList<NZTransposeEntry>());
			}			
			nzEntryTransposeData.get(nze.dimension).add( new NZTransposeEntry(pointCount-1,nze.value) );
		}
	}
	
	public int getDimensionCount() {
		return dimensionCount;
	}
	public void setDimensionCount(int dimensionCount) {
		this.dimensionCount = dimensionCount;
	}
	public int getPointCount() {
		return pointCount;
	}
	public void setPointCount(int pointCount) {
		this.pointCount = pointCount;
	}
	
	public String getDocIDString(int idx) {
		return nzDocIDs.get(idx);
	}
	
	public int getDocIndexFromIDString(String docID) {
		return nzDocIDs.indexOf(docID);
	}
	
	public String toString() {
		
		StringBuilder builder = new StringBuilder();
		
		// print out the two nonzero structures
		builder.append( "[Nonzeros]\n\n" );
		int k = 0;
		for( TreeSet<NZEntry> vals : nzEntryData ) {
			
			builder.append(""+k+":");
			
			for( NZEntry nze : vals ) {
				
				builder.append(nze.toString() + " ");
			}
			
			builder.append("\n");
			
			k++;
		}
		
		builder.append("[Transpose]\n\n");
		
		for( int key : nzEntryTransposeData.keySet() ) {
			
			builder.append(""+key+":");
			
			for( NZTransposeEntry nzte : nzEntryTransposeData.get(key) ) {
				
				builder.append(nzte.toString() + " ");
			}
			
			builder.append("\n");
		}
		
		return builder.toString();
	}	
	
	public float pdist( int np_i, int np_j ) {
	
		ArrayList<NZEntry> i_list = nzEntryDataUnordered.get(np_i);
		ArrayList<NZEntry> j_list = nzEntryDataUnordered.get(np_j);
		
		float d = 0.f;

		int j = 0;
		for( int i = 0; i < i_list.size(); i++ ) {			
			
			while( j < j_list.size() && j_list.get(j).dimension < i_list.get(i).dimension)
				j++;
			
			if( j < j_list.size() && i_list.get(i).dimension == j_list.get(j).dimension ) {
				
				d += i_list.get(i).value * j_list.get(j).value;
			}
		}
		
		return 1.f - d;
	}
	
	/**
	 * Grab an edge from the sparse data structure.  Returns null if none exist.
	 * 
	 * @return
	 */
	public SimpleEdge sampleEdge() {

		SimpleEdge retEdge = null;
		boolean hasEdge = true;
		
		while( hasEdge && currSamplePoints.size() > 0) {

			// grab next points
			
			int ptIdx = currSamplePoints.get(samplerIdx);
						
			while( hasEdge && nzEntryData.get(ptIdx).first().dim_remains ) {
				
				// get next nonzero
				
				NZEntry nzEntry = nzEntryData.get(ptIdx).first();
				
				// get next nonzero dimension entry
				
				NZTransposeEntry nztEntry = nzEntryTransposeData.get(nzEntry.dimension).get(nzEntry.dim_ptr);
				
				// query if this edge has been computed 
				
				int smallerPt = Math.min(ptIdx, nztEntry.point);
				int biggerPt = Math.max(ptIdx, nztEntry.point);
				hasEdge	= edgeChecker.get(smallerPt).containsKey(biggerPt);
				if( ! hasEdge ) {
				
					// compute the distance between the two points
					float d = pdist(smallerPt, biggerPt);
					retEdge = new SimpleEdge(smallerPt,biggerPt,d/*,iter_count*/);
					edgeChecker.get(smallerPt).put(biggerPt, true);
				}
				
				// update the nonzero entries
	
				TreeSet<NZEntry> modEntry = nzEntryData.get(ptIdx);
				modEntry.remove(nzEntry);
				
				nzEntry.dim_ptr++;
				if( nzEntry.dim_ptr + 1 >= nzEntryTransposeData.get(nzEntry.dimension).size() ) {
					
					nzEntry.dim_remains = false;
				}
				else {
				
					nzEntry.alt_dim_val = nzEntryTransposeData.get(nzEntry.dimension).get(nzEntry.dim_ptr).value;
				}
				modEntry.add(nzEntry);
			}
			
			// check if the point is done 
			
			if( ! nzEntryData.get(ptIdx).first().dim_remains ) {
				
				currSamplePoints.remove(samplerIdx);
			}
			else {
				
				samplerIdx = (samplerIdx + 1) % currSamplePoints.size();
				if( samplerIdx == 0 ) {
					iter_count++;
				}
			}			
		}
		
		return retEdge;
	}
}
