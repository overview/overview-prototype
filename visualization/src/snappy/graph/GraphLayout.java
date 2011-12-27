package snappy.graph;

import java.util.ArrayList;

import snappy.pack.IImageInfo;

public class GraphLayout implements IImageInfo {

	private ArrayList<PointData> pts = null; 
	private TopoTreeNode node = null;
	private PointData upperLeft = null;
	private PointData lowerRight = null;
	int num_points = -1;
	
	public int getNumPoints() {
		
		return num_points;
	}
	
	public PointData getPoint( int point_idx ) {
		
		return pts.get(point_idx);
	}
	
	public enum LayoutType {
		
		RANDOM_LAYOUT,
		VERTICAL_LAYOUT,
		MDS_LAYOUT,
		BOX_COX_LAYOUT,
		SUMMARY
	}
	
	LayoutType layoutType = LayoutType.VERTICAL_LAYOUT;
	
	/*
	 * returns the width
	 */
	public int getWidth() {

		if( layoutType == LayoutType.RANDOM_LAYOUT )
			return (int)Math.floor(((lowerRight.x - upperLeft.x) * Math.sqrt( pts.size() )) + 0.5 );
		if(layoutType == LayoutType.SUMMARY ) {
			return (int)Math.ceil(Math.sqrt( pts.size() ));
		}
		
		return (int)Math.floor((lowerRight.x - upperLeft.x) + 0.5);
	}
	
	
	public int getHeight() {
		
		if( layoutType == LayoutType.RANDOM_LAYOUT )
			return (int)Math.floor(((lowerRight.y - upperLeft.y) * Math.sqrt( pts.size() )) + 0.5);
		if(layoutType == LayoutType.SUMMARY ) {
			return (int)Math.ceil(Math.sqrt( pts.size() ));
		}
		
		return (int)Math.ceil(Math.sqrt( pts.size() )) + 2*15 + (int)Math.floor((lowerRight.y - upperLeft.y) + 0.5);
	}
	
	public boolean updateRandomLayout() {

		return true;
	}
	
	public boolean updateVerticalLayout() {

		return true;
	}
	
	public boolean updateMDSLayout() {

		return true;
	}
	
	public boolean updateBCLayout() {

		return true;
	}
	
	/*
	 * Returns true if the layout is done
	 */
	public boolean updateLayout( ) {

		if( layoutType == LayoutType.RANDOM_LAYOUT ) {
			
			return updateRandomLayout();
		}
		
		if( layoutType == LayoutType.VERTICAL_LAYOUT ) {
			
			return updateVerticalLayout();
		}
		
		if( layoutType == LayoutType.MDS_LAYOUT ) {
			
			return updateMDSLayout();
		}
		
		if( layoutType == LayoutType.BOX_COX_LAYOUT ) {
			
			return updateBCLayout();
		}
		
		return true;
	}
	
	public void setLayoutType( LayoutType type ) {
		
		this.layoutType = type; 
		
		for( int i = 0; i < num_points; i++ ) {
			
			if(layoutType == LayoutType.VERTICAL_LAYOUT) {
				
				pts.get(i).x = 15;
				pts.get(i).y = i*15;
			}
			if(layoutType == LayoutType.RANDOM_LAYOUT ) {
				
			}
			upperLeft.x = Math.min( upperLeft.x, pts.get(i).x );
			upperLeft.y = Math.min( upperLeft.y, pts.get(i).y );
			
			lowerRight.x = Math.max( lowerRight.x, pts.get(i).x );
			lowerRight.y = Math.max( lowerRight.y, pts.get(i).y );
		}		
	}
	
	public LayoutType getLayouttype() {
		
		return this.layoutType;
	}
	
//	public GraphLayout(GraphManager gm, int component, LayoutType type ) {
//		
//		this.layoutType=type;
//		num_points = gm.getSubComponents().get(component).size();
//		
//		// initialize the points
//		
//		pts = new ArrayList<PointData>( num_points );
//		upperLeft  = new PointData();
//		lowerRight = new PointData();
//		
//		for( int i = 0; i < num_points; i++ ) {
//			
//			pts.add( new PointData() );
//			pts.get(i).idx = gm.getSubComponents().get(component).get(i);
//		}
//		
//		setLayoutType(layoutType);
//	}
	
	public GraphLayout(TopoTreeNode node, LayoutType type ) {
		
		this.layoutType=type;
		this.node = node;
		num_points = node.component.size();
		
		// initialize the points
		
		pts = new ArrayList<PointData>( num_points );
		upperLeft  = new PointData();
		lowerRight = new PointData();
		
		for( int i = 0; i < num_points; i++ ) {
			
			pts.add( new PointData() );
			pts.get(i).idx = node.component.get(i);
		}
		
		setLayoutType(layoutType);
	}
	
//	public GraphLayout( GraphManager gm, int component ) {
//		
//		this(gm,component,LayoutType.VERTICAL_LAYOUT);
//	}
	
	public TopoTreeNode getNode() {
		return node;
	}
	
	public GraphLayout(TopoTreeNode node) {
		
		this(node,LayoutType.VERTICAL_LAYOUT);
	}
}
