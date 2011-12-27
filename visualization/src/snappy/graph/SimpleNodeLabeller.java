package snappy.graph;


import java.util.ArrayList;
import java.util.Random;

import snappy.data.FeatureList;

public class SimpleNodeLabeller implements NodeLabeller {

	private FeatureList m_fl = null;
	Random m_random = null;
	boolean is_random = false;
	int[] m_shuffle = null;

	public SimpleNodeLabeller( FeatureList fl ) {

		this.m_fl = fl;	
		
		// generate random permutation
		m_random = new Random();
		m_random.setSeed(System.currentTimeMillis());
		int[] shuffle_temp = new int[ m_fl.featureCount() ];
		for( int i = 0; i < shuffle_temp.length; i++ ) {
			shuffle_temp[i] = i;
		}
		int idx_temp = 0;
		int shuffle_idx = 0;
		for( int i = 0; i < shuffle_temp.length; i++ ) {

			shuffle_idx = i + m_random.nextInt(shuffle_temp.length-i) ;
			idx_temp 	= shuffle_temp[i];
			shuffle_temp[i] 	= shuffle_temp[shuffle_idx];
			shuffle_temp[shuffle_idx] = idx_temp;
		}
		m_shuffle = shuffle_temp;
		
		is_random = false;
	}

	
	@Override
	public String getLabel(int node_number) {
		if( is_random )
			return m_fl.featureAt(m_shuffle[node_number]);
		return m_fl.featureAt(node_number);
	}

	@Override
	public void resetLabels() {
		// do nothing
	}


	@Override
	public SizedLabel[] getSummaryLabel(TopoTreeNode node) {

		ArrayList<SizedLabel> labels = new ArrayList<SizedLabel>();
		
		for( int i = 0; i < node.num_points; i++ ) {
		
			if( !is_random ) {
				labels.add( new SizedLabel(m_fl.featureAt(node.component.get(i)), 1));
			}
			else {
				labels.add( new SizedLabel(getLabel(node.component.get(i)), 1));
			}
		}
		
		SizedLabel[] ret = new SizedLabel[labels.size()];
		labels.toArray(ret);
		return ret;
	}

}
