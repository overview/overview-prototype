package snappy.data;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;

/*
 * Class just holds a set of text features.  Can be either node, edge, or nonzero data features
 */
public class FeatureList {
	
	ArrayList<String> features = null;
	
	public FeatureList() {
		features = new ArrayList<String>();
	}	
	
	public int featureCount() {
		
		return features.size();
	}
	
	public void addFeature( String dataLine ) {
		
		features.add(dataLine);
	}
	public String featureAt(int i ) {
		
		return features.get(i);
	}
	
	
	
	public static FeatureList readFeatureList( Reader reader ) {
		try{
			
			BufferedReader breader = new BufferedReader( reader );

			FeatureList fl = new FeatureList();
			
			String lineStr = breader.readLine();
			while( lineStr != null && lineStr.length() > 0 ) {
				
				fl.addFeature( lineStr );				
				lineStr = breader.readLine();
			}
			
			return fl;
		}
		catch(Exception e) {
			
			e.printStackTrace();
		}
		
		return null;
	}
}
