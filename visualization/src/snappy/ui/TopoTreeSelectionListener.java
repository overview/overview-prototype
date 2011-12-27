package snappy.ui;

import java.util.ArrayList;
import java.util.EventListener;

import snappy.graph.TopoTreeNode;

public interface TopoTreeSelectionListener extends EventListener {	
	
	public void selectionChanged( ArrayList<TopoTreeNode> nodes, 
									TopoTreeNode hilighted, boolean selChanged, boolean hiChanged );
}
