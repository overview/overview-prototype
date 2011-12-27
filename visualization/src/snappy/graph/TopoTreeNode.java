package snappy.graph;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class TopoTreeNode {
	
	public int num_points;		
	public TopoTreeNode parent;
	public TopoTreeNode diffParent;
	public ArrayList<TopoTreeNode> children;
	public ArrayList<TopoTreeNode> diffChildren;
	public boolean hilighted;
	public ArrayList<Integer> component;
	public int x;
	public int y;
	public int level = 0;
	public HashMap<Integer,Integer> tags;
	public HashMap<Integer,Color> tag_colors;
	public boolean isSameAsChild = false;
	
	public boolean containsOnlyItems( Collection<Integer> items ) {
		
		if( items.size() >= component.size() ) {
			
			return items.containsAll(component);
		}
		
		return false;
	}
	
	public boolean containsAnyItems( Collection<Integer> items ) {
	
		boolean foundAnItem = false;
		
		for( Integer item : items ) {
			
			if( component.contains(item) )
				return true;
		}
		
		return foundAnItem;
	}
	
	public TopoTreeNode() {
		
		x = 0;
		hilighted=false;
		num_points = 0;
		parent = null;
		diffParent = null;
		diffChildren = new ArrayList<TopoTreeNode>();
		children = new ArrayList<TopoTreeNode>();
		tags = new HashMap<Integer,Integer>();
		tag_colors = new HashMap<Integer,Color>();
		component = null;
	}
	
	public void setDifferentChildren() {
	
		for( TopoTreeNode child : children ) {
			TopoTreeNode temp_self = child;
			while( temp_self.sameAsChild() && !temp_self.children.isEmpty() ) {
				
				temp_self = temp_self.children.get(0);
			}		
			diffChildren.add(temp_self);
		}
	}
	
	public void setDifferentParent() {
				
		TopoTreeNode temp_parent = parent;
		while( temp_parent != null && temp_parent.sameAsChild() ) {
			
			temp_parent = temp_parent.parent;
		}
		diffParent = temp_parent;
	}
	
	public void setSameAsChild() {
		
		isSameAsChild = sameAsChild();
	}
	public boolean sameAsChild( ) {
		
		return children.size() == 1 && num_points == children.get(0).num_points;
	}
}