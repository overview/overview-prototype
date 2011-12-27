package snappy.data;

import java.util.ArrayList;

import snappy.data.NZData.NZEntry;

public class FullDistanceMatrix implements DistanceFunction {

	private int m_numPoints = 0;
	private int m_currentEnteredPoint = 0;
	private float[] raw_data;
	
	public void setInitialPointCount(int num_points) {
		
		int k = num_points + 1;
		raw_data = new float[ (k*(k-1))/2 ];
	}
	
	@Override
	public int getPointCount() {

		return m_numPoints;
	}

	public void addPointData(String dataLine ) {
		
		String[] entries = dataLine.split("\\s+");

		m_currentEnteredPoint++;
		int entry_num = 0;
		for ( String entry : entries ) {
						
			
			if( entry_num >= m_currentEnteredPoint )
				break;
			
			if( entry.length() > 0 ) {

				entry_num++;
				raw_data[((m_currentEnteredPoint*(m_currentEnteredPoint-1))/2)+entry_num] = Float.parseFloat(entry);
			}
		}
	}
	
	@Override
	public float pdist(int np_i, int np_j) {
		
		int i = Math.max(np_i,np_j);
		int j = Math.min(np_i,np_j);
		
		return raw_data[(i*(i-1))/2 + (j-1)];
	}

	@Override
	public SimpleEdge sampleEdge() {

		
		return null;
	}

}
