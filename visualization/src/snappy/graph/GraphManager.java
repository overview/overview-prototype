package snappy.graph;

import snappy.data.*;
import snappy.graph.TagTable.Tag;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.TreeSet;
import java.util.Arrays;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GraphManager {

	boolean isSparse;					// true if we're using an nzd structure
	NZData nzd 				= null;		
	DistanceFunction dfunc 	= null;
	PriorityQueue<SimpleEdge> edge_list = null;
	
	float indiff_value = 1.0f;			// the value at which we don't construct edges
	boolean isComplete = false;			// indicates we need no further updating
	
	int componentCount = 0;				// the number of connected components
	
	public ArrayList<ArrayList<SimpleEdge>> nodeEdgeLookup = null; // maintains a list of edges for each node
//	ArrayList<TreeSet<SimpleEdge>> nodeEdgeLookup = null; // maintains a list of edges for each node
	ArrayList<ArrayList<Integer>> subComponents = null;  // maintains a list of components and their membership
	
	ArrayList<ChangeListener> changeListeners = null;

	public void sortEdges( ) {
		
		for( ArrayList<SimpleEdge> edges : nodeEdgeLookup ) {
			
			Collections.sort( edges, seComp );
		}
	}
	
	
	public class SimpleEdgeComparator implements Comparator<SimpleEdge> {
		
		public int compare(SimpleEdge o1, SimpleEdge o2) {

			return (o1.w - o2.w)>0?1:-1;
		}
	}

	private SimpleEdgeComparator seComp = null;
	
	public ArrayList<ArrayList<Integer>> getSubComponents() {
		
		return subComponents;
	}
	
	/**
	 * get the total number of nodes in the graph
	 * @return
	 */
	public int getNodeCount() {

		return nodeEdgeLookup.size();
	}
	
	public GraphManager() {
		
		seComp = new SimpleEdgeComparator();
		changeListeners = new ArrayList<ChangeListener>();

	}
	
	public void addChangeListener( ChangeListener cl ) {
		
		this.changeListeners.add( cl );
	}
	
	public GraphManager( NZData nzd ) {
		
		this();
		
		nodeEdgeLookup = new ArrayList<ArrayList<SimpleEdge>>(nzd.getPointCount());
		for( int i = 0; i < nzd.getPointCount(); i++ ) {
			
			nodeEdgeLookup.add(new ArrayList<SimpleEdge>( ));
		}
		this.nzd = nzd;
		this.edge_list = new PriorityQueue<SimpleEdge>( nzd.getPointCount(), seComp );
		isSparse = true;
	}
	
	public GraphManager( DistanceFunction dfunc ) {
		
		this();
		
		nodeEdgeLookup = new ArrayList<ArrayList<SimpleEdge>>(dfunc.getPointCount());
		for( int i = 0; i < dfunc.getPointCount(); i++ ) {
			
			nodeEdgeLookup.add(new ArrayList<SimpleEdge>( ));
		}
		this.edge_list = new PriorityQueue<SimpleEdge>( dfunc.getPointCount(), seComp );
		this.dfunc = dfunc;
		isSparse = false;
	}
	
	/*
	 * Get the total number of components
	 */
	public int getComponentCount() {
		
		return this.componentCount;
	}
	
	/*
	 * performs a BFS to count the components
	 */
	public int countComponents() {
		
		subComponents = new ArrayList<ArrayList<Integer>>();
		
		int num_nodes = getNodeCount();
		
		int[] mark			= new int[ num_nodes ];
		int[] stack 		= new int[ num_nodes ];
		int[] stack_count	= new int[ num_nodes ];
		boolean[] instk	   	= new boolean[ num_nodes ];
		
		int stack_ptr = -1;
		int curr_mark = 0;

	    // count connected components
	    
	    for( int n = 0; n < num_nodes; n++ ) {
		        
	        if( mark[n] == 0) {
	            
	            curr_mark++; // indicate that we've got a new component
	            
	            subComponents.add( new ArrayList<Integer>() );
	            
	            // push the first node on the stack
	            
	            stack_ptr = 0;
	            stack[stack_ptr]=n;
	            
	            //subComponents.get(curr_mark-1).add( n );
	            
	            // perform BFS until exhausted
	            while( stack_ptr >= 0 ) {
	                
	                // pop the stack_ptr
	                
	                int cur_node = stack[stack_ptr];
	                stack_ptr--; 
	                
	                // is the popped node touched?
	                
	                if( mark[cur_node] == 0 ) {
	                    
	                    // mark it
	    	            
	                	subComponents.get(curr_mark-1).add( cur_node );
	    	            
	                    mark[cur_node] = curr_mark;
	                    stack_count[curr_mark-1] += 1; // count the mark
	                    
	                    // search the neighboring points that haven't been marked yet
	                    for( SimpleEdge se :nodeEdgeLookup.get(cur_node) ) {
	                    	
	                    	if(mark[se.dst] == 0 && se.w <= indiff_value && !instk[se.dst]) {
	                    		
	                    		stack_ptr++;
	                    		stack[stack_ptr]=se.dst;
	                    		instk[se.dst] = true;
	                    	}
	                    }
	                }
	            }
	        }        
		}    
	
		// update the number of connected components
	
	    componentCount = curr_mark;	
		
		return getComponentCount(); 
	}
	
	/**
	 * Sets the new cutoff.  Returns false if we need to perform further updating. 
	 * 
	 * @param newCutoff
	 * @return
	 */
	public void setCutoff( float newCutoff ) {
		
		// update the indifferentiated value
		
		this.indiff_value = newCutoff;		
		countComponents();			

		// tell everyone about it
		for( ChangeListener cl : changeListeners ) {
			
			cl.stateChanged( new ChangeEvent(this) );
		}
	}
	
	public void outputSimpleEdgesToFile( String filename ) {
		
		try {
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			for( ArrayList<SimpleEdge> edges : nodeEdgeLookup ) {
				for( SimpleEdge edge : edges ) {
					
//					bw.write( "" + edge.iter + " " + edge.w + " " + edge.src + " " + edge.dst+ "\n" );
				}
			}
			bw.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	/**
	 * Updates the graph structure.  Returns false if we require further updating
	 * 
	 * Samples edges using probabilistic importance sampling routine
	 * @return
	 */
	public boolean updateGraph( ) {
	
		SimpleEdge se = null;
		
		if( nzd != null ) {
			se = nzd.sampleEdge();
		}
		else {
			se = dfunc.sampleEdge();
		}
		
		if( se != null ) {
						
			isComplete = false;
			
			nodeEdgeLookup.get(se.src).add(se);
			if( se.dst != se.src )
				nodeEdgeLookup.get(se.dst).add(new SimpleEdge( se.dst, se.src, se.w/*, se.iter */));
			edge_list.add(se);
		}
		else {
			
			isComplete = true;
		}

		return isComplete;
	}

	/*
	 * Returns a set of bin counts
	 */
	public int[] getComponentHisto( ) {
		
		int largest_component = -1;
		
		// get the largest bin count
		for( ArrayList<Integer> component : getSubComponents() ) {
			
			largest_component = Math.max( largest_component, component.size() );
		}
		
		int[] bins = new int[largest_component];
		Arrays.fill(bins, 0);
		
		for( ArrayList<Integer> component : getSubComponents() ) {
			
			bins[component.size()-1]++;
		}
		
		return bins;
	}
	
	public int[] getHisto( int bins ) {
		
		return getHisto( bins, Integer.MAX_VALUE );
	}
	
	/*
	 * return a histogram of the edges (hard coded between 0 and 1)
	 */
	public int[] getHisto( int bins, int max_edge_count ) {
		
		if( nzd != null ) {
			
			return nzd.uniformHistogram( bins, this.getNodeCount() * 2 );
		}
		else {
			int[] retBins = new int[bins];		
			Arrays.fill(retBins, 0);
			
			double bin_size = 1.0 / ((double) bins);
			int n = 0;
	//		System.out.println("edge_list size = " + edge_list.size() + " max edge = " + max_edge_count);
			for( SimpleEdge se : edge_list ) {
				n++;
				int k = Math.max(0,Math.min(bins-1, (int) Math.floor((double)(se.w) / bin_size)));			
				retBins[ k ]++;
	//			if( n++ > max_edge_count )
	//				break;			
			}
			
	//		System.out.println("n = " + n);
			
			return retBins;
		}
	}
	
}
