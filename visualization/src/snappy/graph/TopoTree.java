package snappy.graph;

import java.util.ArrayList;
import java.util.HashMap;

public class TopoTree {

	public int num_levels;  						// number of levels in the tree
	float[] levels;  						// the indifferentiated values at each level 
	public ArrayList<TopoTreeNode> roots = null; 	// root nodes to the tree (usually just one)
	public HashMap<Integer,Integer> component_sizes = null;
	
	public ArrayList<TopoTreeNode[]> tree_lookup;			// tree lookup that maps node numbers to their current node
	public ArrayList<ArrayList<TopoTreeNode>> level_lookup;
	
	public ArrayList<TopoTreeNode> getLCA( ArrayList<Integer> items ) {
		
		ArrayList<TopoTreeNode> nodes = new ArrayList<TopoTreeNode>();
		
		// calculate the LCA of these items
		
		for( int i = 0; i < num_levels; i++ ) {
			
			if( i == 0 ) {  
				
				// if we're on the lowest level add the nodes to the hashset
				
			}
			else {
				
			}
		}
		
		return nodes;
	}
	
	public TopoTree() {
		
		component_sizes = new HashMap<Integer, Integer>();
		roots = new ArrayList<TopoTreeNode>();
		tree_lookup = new ArrayList<TopoTreeNode[]>();
		level_lookup = new ArrayList<ArrayList<TopoTreeNode>>();
	}
	
	//
	// constructor takes a graph and the number of levels to tree you want to compute
	//
	public TopoTree( GraphManager gm, float[] levels ) {
		
		this();
		
		num_levels = levels.length;
		
		for( int i = 0; i < num_levels; i++ ) {
			
			// init a lookup table for this layer 
			
			tree_lookup.add( new TopoTreeNode[gm.getNodeCount()] );
			level_lookup.add(new ArrayList<TopoTreeNode>());
			
			// set the cutoff and build the components
			
			gm.setCutoff( levels[i] );
			int junk = gm.countComponents();		
			
			System.out.println("level " + i + ":" + junk);
			
			// for each of the subcomponents at this level
			
			ArrayList<ArrayList<Integer>> sub_components = gm.getSubComponents();
			for( int j = 0; j < sub_components.size(); j++ ) {

				// build the new node
				
				TopoTreeNode ttn = new TopoTreeNode();	
				ttn.level = i;
				ttn.num_points = sub_components.get(j).size();
				ttn.component = sub_components.get(j);
				
				// update the list of possible component sizes
				
				if( ! component_sizes.containsKey(ttn.num_points) ) {
					
					component_sizes.put(ttn.num_points, 1);
				}
				else {
					
					component_sizes.put(ttn.num_points, component_sizes.get(ttn.num_points)+1);
				}
				
				if( i > 0 ) {
					
					// forge a link between parent and child node
					
					ttn.parent = tree_lookup.get(i-1)[ sub_components.get(j).get(0) ];
					ttn.parent.children.add(ttn);
				}
				else {
					
					// attach to the roots ( if we're at level 0 )
					roots.add(ttn);
				}
				
				// add the new node to the lookup tables
				
				for( int k = 0; k < ttn.num_points; k++ ) {
					
					tree_lookup.get(i)[sub_components.get(j).get(k)] = ttn;
				}
				level_lookup.get(i).add(ttn);
			}
		}
		
		// mark and link up to nonduplicate topo nodes
		for(ArrayList<TopoTreeNode> nodes : level_lookup) {
			
			for( TopoTreeNode node : nodes ) {
				
				node.setSameAsChild();
				node.setDifferentParent();
				node.setDifferentChildren();
			}
		}
	}
	
	public TopoTreeNode lookupNode( int level ) {
	
		return null;
	}
}
