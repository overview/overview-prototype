package snappy.graph;

public interface NodeLabeller {

	public SizedLabel[] getSummaryLabel(TopoTreeNode gl);
	public String getLabel( int node_number );
	public void resetLabels();
}
