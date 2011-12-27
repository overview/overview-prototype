package snappy.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import snappy.data.FeatureList;
import snappy.data.NZData;
import snappy.data.SimpleEdge;
import snappy.data.NZData.NZEntry;

public class EdgeIntersectionLabeller implements NodeLabeller {

	private GraphManager m_gm = null;
	private FeatureList m_fl = null;
	private NZData m_nz = null;
	public static int MAXLABELS = 25;
	
	class TermMultiplier {
		
		public String term = "";
		public float multiplier = 0.f;
		
		public TermMultiplier( String term, float multiplier ) {
			
			this.term = term;
			this.multiplier = multiplier;
		}
		
	}
	
	public EdgeIntersectionLabeller( GraphManager gm, 
									 FeatureList fl,
									 NZData nz ) {
		
		this.m_gm = gm;
		this.m_fl = fl;
		this.m_nz = nz;
		
		labelCache = new String[gm.getNodeCount()];
		Arrays.fill(labelCache,null);
		summaryLabelCache = new HashMap<TopoTreeNode,SizedLabel[]>();
	}
	
	private String[] labelCache = null;
	private HashMap<TopoTreeNode,SizedLabel[]> summaryLabelCache = null;
	
	@Override
	public String getLabel(int node_number) {

		if( labelCache[node_number] == null ) {
		
			ArrayList<TermMultiplier> terms = new ArrayList<TermMultiplier>();
			HashMap<String,Float> termsMap  = new HashMap<String,Float>();
			
			for( SimpleEdge se : m_gm.nodeEdgeLookup.get(node_number) ) {
				
				if( se.w > m_gm.indiff_value )
					break;
				
				// get the intersection of the terms
				
				ArrayList<NZEntry> i_list = m_nz.nzEntryDataUnordered.get(se.src);
				ArrayList<NZEntry> j_list = m_nz.nzEntryDataUnordered.get(se.dst);
				
				int j = 0;
				for( int i = 0; i < i_list.size(); i++ ) {			
					
					while( j < j_list.size() && j_list.get(j).dimension < i_list.get(i).dimension)
						j++;
					
					if( j < j_list.size() && i_list.get(i).dimension == j_list.get(j).dimension ) {

						
						// only add features who are closer
						
						boolean pass_test = true;
						float d = 1.f-i_list.get(i).value * j_list.get(j).value;
						if( termsMap.containsKey(m_fl.featureAt(i_list.get(i).dimension)) ) {

							pass_test = false;
							
							if( termsMap.get(m_fl.featureAt(i_list.get(i).dimension)) > d ) {
							
								pass_test = true;
							}
						}
						
						if( pass_test ) {

							termsMap.put(m_fl.featureAt(i_list.get(i).dimension), d);
						}
					}
				}
			}
			
			for( String s : termsMap.keySet() ) {
				
				terms.add(new TermMultiplier(s, termsMap.get(s)));
			}
			
			// sort the terms by importance
			
			TermMultiplier[] termArray = new TermMultiplier[terms.size()];
			terms.toArray(termArray);
			Arrays.sort(termArray, new Comparator<TermMultiplier>() {

				@Override
				public int compare(TermMultiplier arg0, TermMultiplier arg1) {

					return (int)Math.signum(arg0.multiplier-arg1.multiplier);
				}} );
			
			// build the label 
			
			String label = "";
			int inner_label_count = 0;
			for( TermMultiplier tm : termArray) {
				
				label += (tm.term + " ");
				inner_label_count++;
				if(inner_label_count>MAXLABELS)
					break;
			}
			
			labelCache[node_number] = label; 
		}
		
		return labelCache[node_number];
	}

	@Override
	public void resetLabels() {

		summaryLabelCache.clear();
		labelCache = new String[m_gm.getNodeCount()];
		Arrays.fill(labelCache,null);
	}
	
	public static void main(String[] args ) {
		
		
	}

	@Override
	public SizedLabel[] getSummaryLabel( TopoTreeNode node ) {
		
		if( !summaryLabelCache.containsKey(node) ) {
			
			
			ArrayList<TermMultiplier> terms = new ArrayList<TermMultiplier>();
			HashMap<String,Float> termsMap  = new HashMap<String,Float>();
			
			for( int i = 0; i < node.num_points; i++ ) {
				
				for( NZEntry nze : m_nz.nzEntryDataUnordered.get(node.component.get(i)) ) {
					
					String feature = m_fl.featureAt(nze.dimension);
					if( termsMap.containsKey(feature) ) {

						termsMap.put(feature, termsMap.get(feature)+nze.value);
					}
					else {
						
						termsMap.put(feature, nze.value);
					}
				}
			}
			
			for( String s : termsMap.keySet() ) {
				
				terms.add(new TermMultiplier(s, termsMap.get(s)));
			}
			
			// sort the terms by importance
			
			TermMultiplier[] termArray = new TermMultiplier[terms.size()];
			terms.toArray(termArray);
			Arrays.sort(termArray, new Comparator<TermMultiplier>() {

				@Override
				public int compare(TermMultiplier arg0, TermMultiplier arg1) {

					return (int)Math.signum((1.f-arg0.multiplier)-(1.f-arg1.multiplier));
				}} );
			
			// build the label 
			
			int inner_label_count = 0;
			SizedLabel[] summaryLabelCachePut = new SizedLabel[termArray.length>MAXLABELS?MAXLABELS:termArray.length];
			for( TermMultiplier tm : termArray) {
				
				summaryLabelCachePut[inner_label_count]=new SizedLabel(tm.term,tm.multiplier);
				inner_label_count++;
				if(inner_label_count>=MAXLABELS)
					break;
			}
			summaryLabelCache.put(node, summaryLabelCachePut);
		}
		
		return summaryLabelCache.get(node);
	}

}
