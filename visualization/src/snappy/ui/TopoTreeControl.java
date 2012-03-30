package snappy.ui;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.PApplet;
import processing.core.PFont;
import snappy.graph.NodeLabeller;
import snappy.graph.SizedLabel;
import snappy.graph.TagChangeListener;
import snappy.graph.TagTable;
import snappy.graph.TagTable.Tag;
import snappy.graph.TopoTree;
import snappy.graph.TopoTreeNode;

public class TopoTreeControl extends PApplet implements ComponentListener,
		ChangeListener, TagChangeListener {

	public NodeLabeller node_labeller = null;
	
	boolean is_hovering = false;
	String hover_text = "";
	
	boolean shift_key_down = false;
	boolean option_key_down = false;
	
	public TopoTree m_tt = null;
	int levels = 0;
	int level_size = 0;
	static public int ignore_component_size = 4;

	boolean select_rect_down = false;
	int select_rect_x = -1;
	int select_rect_y = -1;
	int select_rect_w = -1;
	int select_rect_h = -1;

	static final int DEF_NODE_SIZE = 5;
	static final int SEL_NODE_SIZE = 5;
	static final int HI_NODE_SIZE = SEL_NODE_SIZE;
	static final int BORDER_SIZE = 10;
	static final int POWERS_OF_TWO = 7;
	int PRUNE_LABEL_WIDTH = -1;

	static final int HIST_WIDTH = 50;
	static final int HIST_SCALE_WIDTH = 50;

	static boolean draw_hist = false;							// set to true if you want to see the histogram
	static int hist_actual_width = draw_hist ? HIST_WIDTH + HIST_SCALE_WIDTH : 0;
	
	static final int PRUNER_HEIGHT = 30;
	static int HOVER_HEIGHT = 10;
	static int BOTTOM_CTRL_HEIGHT = PRUNER_HEIGHT + HOVER_HEIGHT;

	static final int PREFERRED_WIDTH = 800;
	static final int PREFERRED_HEIGHT = 1000;

	PFont prune_font = null;

	int sel_prune = 3;

	public Object cutoff_changed;
	public Object compon_changed;
	public Object hilight_changed;
	public Object select_changed;

	public TopoTreeNode hilightedNode = null;

	public int[] bins = null;

	ArrayList<ChangeListener> changeListeners = null;
	
	TagTable m_ttable = null;

	ArrayList<TagChangeListener> tagChangeListeners = null; 
	public void addTagChangeListener( TagChangeListener listener ) {
		
		tagChangeListeners.add( listener );
	}

	public void setTagTable( TagTable ttable ) {

		m_ttable = ttable;
		m_ttable.addTagChangeListener(this);
	}

	public boolean updateHilight( TopoTreeNode node ) {
		
//		System.out.println("Begin update hilight");
		if( node != null ) {
			
			if( node.hilighted ) {	
				//return false; 		// commented so as not to suppress highlight of same node; so user can click node, click tag, then click same node
			}
			
			if( hilightedNode != null ) {
				
				hilightedNode.hilighted = false;
			}
			hilightedNode = node;
			hilightedNode.hilighted = true;
			
			// add the new node to the selection
			InteractionLogger.log("TREE VIEW SELECT NODE");
			m_ttable.promoteTagSilent( m_ttable.getListedTag() );
			m_ttable.getListedTag().setItems(node.component);
			m_ttable.notifyListenersTagsChanged();
			redraw();
		}
		else {
			
			if( hilightedNode == null ) {
				
				return false;
			}
			hilightedNode.hilighted = false;
			hilightedNode = null;
			
			InteractionLogger.log("TREE VIEW CLEAR SELECTION");
			m_ttable.promoteTagSilent( m_ttable.getListedTag() );
			m_ttable.getListedTag().setItems(null);
			m_ttable.notifyListenersTagsChanged();
			redraw();
		}
//		System.out.println("End update hilight");
		
		return true;
	}
		
	
	public TopoTreeControl(TopoTree tt, int[] bins) {

		super();

		tagChangeListeners = new ArrayList<TagChangeListener>();
		
		m_tt = tt;

		setIgnoreComponenetSize((int) Math.pow(2, sel_prune));
		this.bins = bins;
		levels = bins.length;
		changeListeners = new ArrayList<ChangeListener>();

		cutoff_changed = new Object();
		compon_changed = new Object();
		hilight_changed = new Object();
		select_changed = new Object();
		
		this.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {

				redraw();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
			
				redraw();
			}
		});
	}

	public void addChangeListener(ChangeListener cl) {

		this.changeListeners.add(cl);
	}

	public void setTopoTree(TopoTree tt) {

		m_tt = tt;

		levels = -1;
		for (TopoTreeNode ttn : m_tt.roots) {

			levels = Math.max(levels, count_depth(ttn));
		}
		redraw();
	}

	public int getIgnoreComponentSize() {

		return ignore_component_size;
	}

	public void setIgnoreComponenetSize(int x) {

		ignore_component_size = x;
		InteractionLogger.log("TREE VIEW PRUNING",Integer.toString(x));
		redraw();
	}

	// simple recursive routine to count the depth of a tree
	public int count_depth(TopoTreeNode node) {

		if (node.children.size() > 0) {

			int depth = -1;
			for (TopoTreeNode ttn : node.children) {

				depth = Math.max(depth, count_depth(ttn));
			}

			return 1 + depth;
		}

		return 0;
	}

	public void setup() {

		smooth();

		prune_font = createFont("SansSerif", 12);
		textFont(prune_font);
		HOVER_HEIGHT = (int)Math.round(textAscent()+textDescent()) + 5;
		BOTTOM_CTRL_HEIGHT = PRUNER_HEIGHT + HOVER_HEIGHT;
		noLoop();
		this.setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
	}
	
	
	void roundrect(int x, int y, int w, int h, int r) {

		 int  ax, ay, hr;

		 ax=x+w-1;
		 ay=y+h-1;
		 hr = r/2;

		 rect(x, y, w, h);
		 arc(x, y, r, r, radians(180.f), radians(270.f));
		 arc(ax, y, r,r, radians(270.f), radians(360.f));
		 arc(x, ay, r,r, radians(90.f), radians(180.f));
		 arc(ax, ay, r,r, radians(0.f), radians(90.f));
		 rect(x, y-hr, w, hr);
		 rect(x-hr, y, hr, h);
		 rect(x, y+h, w, hr);
		 rect(x+w,y,hr, h);
	}
	
	public void draw() {

		background(255);

		
//		rect(0,0,HIST_SCALE_WIDTH,getHeight()-PRUNER_HEIGHT);
		
		// break up the scene into levels (at which the tree will slice things
		// up)

		level_size = (int) Math.round(((float) getHeight() - BOTTOM_CTRL_HEIGHT)
				/ (levels + 1.f));

		// draw the histogram scale 
		if (draw_hist) {
			fill(0);
			stroke(0);
			
			line(HIST_WIDTH + 5,level_size,HIST_WIDTH + 5 + 1*HIST_SCALE_WIDTH/3,level_size);
			line(HIST_WIDTH + 5,level_size*levels,HIST_WIDTH + 5 + 1*HIST_SCALE_WIDTH/3,level_size*levels);
			text("1.0", HIST_WIDTH + 10 + 1*HIST_SCALE_WIDTH/3,level_size*2);
			text("0.0", HIST_WIDTH + 10 + 1*HIST_SCALE_WIDTH/3,level_size*levels);
			
			line(HIST_WIDTH + 5 + HIST_SCALE_WIDTH/6,level_size,HIST_WIDTH + 5 + HIST_SCALE_WIDTH/6,level_size*levels);
			
			rotate(-PI/2.f);
			String scale_title = "Distance Threshold"; 
			text(scale_title,-(level_size*levels + level_size)/2 - textWidth(scale_title)/2, HIST_WIDTH + 5 + 2*HIST_SCALE_WIDTH/3 );
			rotate(PI/2.f);
		}
		
		// draw the levels (ruled horizontal lines going through tree)	
		stroke(128 + 64 + 32);
	
		for (int i = 0; i < levels; i++) {
			int y = level_size + i * level_size;
			line(hist_actual_width, y, getWidth(), y);
		}

		
		// slice up the top level
		int topsize = 0;
		for (TopoTreeNode ttn : m_tt.roots) {

			if (ttn.num_points >= ignore_component_size)
				topsize += ttn.num_points;
		}
		int left = BORDER_SIZE + hist_actual_width;
		int right = left;
		for (TopoTreeNode ttn : m_tt.roots) {

			if (ttn.num_points >= ignore_component_size) {

				right += (int) Math
						.round(((getWidth() - hist_actual_width) - 2 * BORDER_SIZE)
								* ((float) ttn.num_points) / ((float) topsize));
				
				// recursively draw edges, in reverse tag queue order
				drawEdge(ttn, left, right, 0, -1, -1, true,null);
				for( int i = m_ttable.tag_queue.size()-1; i >= 0; i-- ) {

					Tag tag = m_ttable.tag_queue.get(i);
					drawEdge(ttn, left, right, 0, -1, -1, true,tag);
				}

				// recursively draw nodes, in reverse tag queue order
				drawNode(ttn, left, right, 0, -1, -1, true,null);
				for( int i = m_ttable.tag_queue.size()-1; i >= 0; i-- ) {

					Tag tag = m_ttable.tag_queue.get(i);
					drawNode(ttn, left, right, 0, -1, -1, true,tag);
				}
				left = right;
			}
		}

		// draw the histogram on the side
		if (draw_hist) {
			fill(255);
			stroke(255);
			rect(0, 0, HIST_WIDTH, getHeight() - BOTTOM_CTRL_HEIGHT);
	
			// do some normalization
	
			float max_binlen = Float.MIN_VALUE;
			float min_binlen = Float.MAX_VALUE;
			for (int i = 0; i < bins.length; i++) {
	
				if (bins[i] > 0) {
	
					max_binlen = Math.max(max_binlen, (float) bins[i]);
					min_binlen = Math.min(min_binlen, (float) bins[i]);
	//				max_binlen = Math.max(max_binlen, (float) Math.log10(bins[i]));
	//				min_binlen = Math.min(min_binlen, (float) Math.log10(bins[i]));
				} else {
	//				max_binlen = Math.max(max_binlen, -2.f);
	//				min_binlen = Math.min(min_binlen, -2.f);
					max_binlen = Math.max(max_binlen, 0);
					min_binlen = Math.min(min_binlen, 0);
				}
			}
			max_binlen += 1.0;
	
			stroke(255);
			fill(0x25, 0x8B, 0xC1);
	
			float binrange = max_binlen - min_binlen;
	
			for (int i = 0; i < bins.length; i++) {
	
				int y = level_size / 2 + i * level_size;
				float bf_val = 0.f;
				if (bins[(bins.length - 1) - i] == 0) {
	
					bf_val = 0;
				} else {
	//				bf_val = (float) Math.log10(bins[(bins.length - 1) - i]);
					bf_val = (float) bins[(bins.length - 1) - i];
				}
				if( i == 0 ) {
					rect((HIST_WIDTH - 1) - (HIST_WIDTH
							* (max_binlen - min_binlen) / binrange), y, HIST_WIDTH
							* (max_binlen - min_binlen) / binrange, level_size);
				}
				else if ( i < bins.length-1 ) {
					rect((HIST_WIDTH - 1) - ((bf_val-min_binlen > 0)?Math.max(2, HIST_WIDTH
							* (bf_val - min_binlen) / binrange):0), y, (bf_val-min_binlen > 0)?Math.max(2, HIST_WIDTH
							* (bf_val - min_binlen) / binrange):0, level_size);				
				}
			}
		}

		// draw the pruner control
		noStroke();
		fill(64);
		rect(0,getHeight() - BOTTOM_CTRL_HEIGHT - 15,getWidth(),PRUNER_HEIGHT+15);
		
		fill(255);
		String prune_label = " Show Nodes >= ";
		PRUNE_LABEL_WIDTH = (int)Math.ceil(textWidth(" Show Nodes >= "));
		text(prune_label, 5, getHeight()
				- BOTTOM_CTRL_HEIGHT
				+ (PRUNER_HEIGHT / 2 - (textAscent() + textDescent())/2 ));
		
		fill(157, 106, 94);
		noStroke();

		
		int prune_bin_width = (getWidth()-PRUNE_LABEL_WIDTH) / POWERS_OF_TWO;
		for (int i = 0; i < POWERS_OF_TWO; i++) {

			if (i > sel_prune) {
				fill(157, 106, 94);
				stroke(255);
			} else if (i < sel_prune) {
				fill(145, 144, 144);
				stroke(255);
			} else if (i == sel_prune) {
				
				fill(PrettyColors.Red.getRed(), PrettyColors.Red.getGreen(), PrettyColors.Red.getBlue());
				stroke(5, 199, 215);
			}

			noStroke();
//			rect(i * prune_bin_width, getHeight() - PRUNER_HEIGHT,
//					prune_bin_width-5, getHeight()-5);
			roundrect(PRUNE_LABEL_WIDTH + i * prune_bin_width + 10, getHeight() - BOTTOM_CTRL_HEIGHT-5,
					prune_bin_width-20, PRUNER_HEIGHT-10,10);
			
			fill(255);
			String str = "" + ((int) Math.pow(2, i));
			text(str, PRUNE_LABEL_WIDTH + i * prune_bin_width
					+ (prune_bin_width / 2 - textWidth(str)) + 10, getHeight()
					- BOTTOM_CTRL_HEIGHT
					+ (PRUNER_HEIGHT / 2 - (textAscent() + textDescent())/2 ));

			fill(0);
			stroke(0);
			strokeWeight(1.f);
//			int rad = SEL_NODE_SIZE + (int)Math.round(SEL_NODE_SIZE * Math.log(((int) Math.pow(2, i))));
//			ellipse( PRUNE_LABEL_WIDTH + i * prune_bin_width
//					+ (prune_bin_width / 2 - textWidth(str)) - rad - 2, getHeight()
//					- BOTTOM_CTRL_HEIGHT - 10
//					+ (PRUNER_HEIGHT / 2 + (textAscent() + textDescent()) / 2) - rad,rad,rad);
			strokeWeight(5.f);
		}
		strokeWeight(1.f);

		// draw selection rect

		if (select_rect_down) {

			noFill();
			stroke(128);
			rect(select_rect_x, select_rect_y, select_rect_w, select_rect_h);
		}
		
		if( this.hasFocus() ) {
			
			noFill();
			stroke(128);
			strokeWeight(3.f);
			rect(3/2,3/2,getWidth()-3,getHeight()-3);
			strokeWeight(1.f);
		}
		
		// draw hover box
		
		stroke(128);
		fill(255);
		rect(0,getHeight() - HOVER_HEIGHT,getWidth(),HOVER_HEIGHT);
		if( is_hovering ) {
			fill(PrettyColors.DarkGrey.getRed(),PrettyColors.DarkGrey.getGreen(),PrettyColors.DarkGrey.getBlue());
			text(hover_text, 5, getHeight() - (textAscent() + textDescent()) / 2);
		}
		
	}

	// recursive draw routine
	public void drawNode(TopoTreeNode node, int left, int right, int level,
			int px, int py, boolean drawNode, Tag t ) {

		boolean draw_full = false;
		boolean self_in_tag = t==null?true:(t.part_components.contains(node) || t.full_components.contains(node));
		if( !self_in_tag ) {
			
			return;
		}
		
		// draw the edge to its parent
		if (px != -1) {

			if( t != null && t.part_components.contains(node) ) {
				
				stroke(	t.tag_color.getRed(),
						t.tag_color.getGreen(),
						t.tag_color.getBlue());
				if( t == m_ttable.topTag() ) {
					strokeWeight(3.f);
				}
				else {
					strokeWeight(2.f);
				}
			}
			else {
				
				if( t != null && t.full_components.contains(node) ) {
					
					draw_full = true;
					
					stroke(	t.tag_color.getRed(),
							t.tag_color.getGreen(),
							t.tag_color.getBlue());
					if( t == m_ttable.topTag() || t == m_ttable.topNonListedTag() ) {
						strokeWeight(3.f);
					}
					else {
						strokeWeight(2.f);
					}
				}
				else {
				
					stroke(128);
					strokeWeight(1.f);
				}
			}
			
//			line((left + right) / 2, level_size * (level + 1), px, py);
		}

		// break out if there's no room to recurse

		if (left != right) {

			// slice up the remaining space

			int topsize = 0;
			for (TopoTreeNode ttn : node.children) {

				if (ttn.num_points >= ignore_component_size)
					topsize += ttn.num_points;
			}
			int newleft = left;
			int newright = left;
			for (TopoTreeNode ttn : node.children) {
				
				boolean child_in_tag = t==null?true:(t.part_components.contains(ttn) || t.full_components.contains(ttn));
				
				if (ttn.num_points >= ignore_component_size ) {

					newright += (int) Math.round((right - left)
							* ((float) ttn.num_points) / ((float) topsize));
					
					drawNode(ttn, newleft, newright, level + 1,
							(left + right) / 2, level_size * (level + 1), true, t);
					
					newleft = newright;
				}
				else {

					if( child_in_tag && t != null ) {
						
						draw_full = true; 
					}
				}
			}
		}

		node.x = (left + right) / 2;
		node.y = level_size * (level + 1);

		// draw the node
		if ((drawNode || node.hilighted) && !node.isSameAsChild) {

			// chose color based on tags
			if (draw_full && t != null) {
				
				fill(	t.tag_color.getRed(),
						t.tag_color.getGreen(), 
						t.tag_color.getBlue());
			}
			else {
				fill(	PrettyColors.Grey.getRed(), 
						PrettyColors.Grey.getGreen(), 
						PrettyColors.Grey.getBlue());
			}
			stroke(0);

			if( !node.hilighted )
				noStroke();

			int node_size = DEF_NODE_SIZE;
			if (node.hilighted || (draw_full && t==m_ttable.topTag())) {
				node_size = 3*DEF_NODE_SIZE;
			}
			if( t != null && !draw_full )
				return;
			rect((left + right) / 2 - node_size/2, level_size * (level + 1) - node_size/2,node_size,node_size);
			
			if (node.hilighted) {

				if ( draw_full ) {
					
					stroke(	2*t.tag_color.getRed()/3,
							2*t.tag_color.getGreen()/3, 
							2*t.tag_color.getBlue()/3);
				}
				strokeWeight(2.f);
				rect((left + right) / 2 - 3*node_size/4, level_size * (level + 1) - 3*node_size/4,3*node_size/2,3*node_size/2);
			}
			
			stroke(0);
			strokeWeight(1.f);
		}
	}

	public void drawEdge(TopoTreeNode node, int left, int right, int level,
			int px, int py, boolean drawNode, Tag t ) {

		boolean self_in_tag = t==null?true:(t.part_components.contains(node) || t.full_components.contains(node));
		if( !self_in_tag ) {
			
			return;
		}
		
		// draw the edge to its parent
		if (px != -1) {

			if( t != null && t.part_components.contains(node) ) {
				
				stroke(	t.tag_color.getRed(),
						t.tag_color.getGreen(),
						t.tag_color.getBlue());
				if( t == m_ttable.topTag() || t == m_ttable.topTag()) {
					strokeWeight(6.f);
				}
				else {
					strokeWeight(2.f);
				}
			}
			else {
				
				if( t != null && t.full_components.contains(node) ) {
					
					stroke(	t.tag_color.getRed(),
							t.tag_color.getGreen(),
							t.tag_color.getBlue());
					if( t == m_ttable.topTag() || t == m_ttable.topTag() ) {
						strokeWeight(6.f);
					}
					else {
						strokeWeight(2.f);
					}
				}
				else {
				
					stroke(128);
					strokeWeight(1.f);
				}
			}
			
			line((left + right) / 2, level_size * (level + 1), px, py);
		}

		// break out if there's no room to recurse

		if (left != right) {

			// slice up the remaining space

			int topsize = 0;
			for (TopoTreeNode ttn : node.children) {

				if (ttn.num_points >= ignore_component_size)
					topsize += ttn.num_points;
			}
			int newleft = left;
			int newright = left;
			for (TopoTreeNode ttn : node.children) {
				
				if (ttn.num_points >= ignore_component_size ) {

					newright += (int) Math.round((right - left)
							* ((float) ttn.num_points) / ((float) topsize));
					
					drawEdge(ttn, newleft, newright, level + 1,
							(left + right) / 2, level_size * (level + 1), true, t);
					
					newleft = newright;
				}
			}
		}
	}

//	public TagQuery drawNode(TopoTreeNode node, int left, int right, int level,
//			int px, int py, boolean drawNode) {
//
//		TagQuery tq_self = m_ttable.queryNode(node);
//		
//		// draw the edge to its parent
//		if (px != -1) {
//
//			if( tq_self.isPartial ) {
//				
//				stroke(	tq_self.partialColor.getRed(),
//						tq_self.partialColor.getGreen(),
//						tq_self.partialColor.getBlue());
//				if( tq_self.queuePos == 0 ) {
//					strokeWeight(3.f);
//				}
//				else {
//					strokeWeight(2.f);
//				}
//			}
//			else {
//				
//				if( tq_self.isFull ) {
//					
//					stroke(	tq_self.fullColor.getRed(),
//							tq_self.fullColor.getGreen(),
//							tq_self.fullColor.getBlue());
//					if( tq_self.queuePos == 0 ) {
//						strokeWeight(3.f);
//					}
//					else {
//						strokeWeight(2.f);
//					}
//				}
//				else {
//				
//					stroke(128);
//					strokeWeight(1.f);
//				}
//			}
//			
//			line((left + right) / 2, level_size * (level + 1), px, py);
//		}
//
//		boolean has_children = false;
//		// break out if there's no room to recurse
//
//		if (left != right) {
//
//			// slice up the remaining space
//
//			int topsize = 0;
//			for (TopoTreeNode ttn : node.children) {
//
//				if (ttn.num_points >= ignore_component_size)
//					topsize += ttn.num_points;
//			}
//			int newleft = left;
//			int newright = left;
//			for (TopoTreeNode ttn : node.children) {
//
//				if (ttn.num_points >= ignore_component_size) {
//
//					newright += (int) Math.round((right - left)
//							* ((float) ttn.num_points) / ((float) topsize));
//					
//					drawNode(ttn, newleft, newright, level + 1,
//							(left + right) / 2, level_size * (level + 1), true);
//					
//					newleft = newright;
//				}
//				else {
//					
//					TagQuery tq_child = m_ttable.queryNode(ttn);
//					if( tq_child.hasTag && tq_child.queuePos <= tq_self.queuePos ) {
//						
//						tq_self.queuePos = tq_child.queuePos ;
//						tq_self.isFull = true;
//						tq_self.fullColor = tq_child.fullColor;
//					}
//				}
//			}
//		}
//
//		node.x = (left + right) / 2;
//		node.y = level_size * (level + 1);
//
//		// draw the node
//		if ((drawNode || node.hilighted) && !node.isSameAsChild) {
//
//			// if (level != sel_level) {
//			
//			// chose color based on tags
//			if (tq_self.hasTag && tq_self.isFull) {
//				
//				fill(	tq_self.fullColor.getRed(),
//						tq_self.fullColor.getGreen(), 
//						tq_self.fullColor.getBlue());
//			}
//			else {
//				fill(	PrettyColors.Grey.getRed(), 
//						PrettyColors.Grey.getGreen(), 
//						PrettyColors.Grey.getBlue());
//			}
//			stroke(0);
//
//			if( !node.hilighted )
//				noStroke();
//
//			int node_size = 2*DEF_NODE_SIZE + (int)Math.round(DEF_NODE_SIZE * Math.log(node.num_points));
//			if (node.hilighted || (tq_self.hasTag && tq_self.isFull && tq_self.queuePos==0)) {
//				node_size = Math.max(6, HI_NODE_SIZE + (int)Math.round(1.5*DEF_NODE_SIZE * Math.log(node.num_points)));
//			}
//			rect((left + right) / 2 - node_size/2, level_size * (level + 1) - node_size/2,node_size,node_size);
//			
//			if (node.hilighted) {
//
//				if (tq_self.hasTag && tq_self.isFull) {
//					
//					stroke(	2*tq_self.fullColor.getRed()/3,
//							2*tq_self.fullColor.getGreen()/3, 
//							2*tq_self.fullColor.getBlue()/3);
//				}
//				strokeWeight(2.f);
//				rect((left + right) / 2 - 3*node_size/4, level_size * (level + 1) - 3*node_size/4,3*node_size/2,3*node_size/2);
//			}
//			
//			stroke(0);
//			strokeWeight(1.f);
//		}
//		
//		return tq_self;
//	}

	public void keyReleased() {
		
		if (key == CODED) {
			if( keyCode == ALT ) {
				
				option_key_down = false;
			}
			if( keyCode == SHIFT ) {
				
				shift_key_down = false;
			}
		}		
	}
	public void keyPressed() {
		
		if (key == CODED) {
			if( keyCode == ALT ) {
				
				option_key_down = true;
			}
			if( keyCode == SHIFT ) {
				
				shift_key_down = true;
			}
			if( keyCode == UP ) {
				
				if( hilightedNode != null && hilightedNode.diffParent != null && 
						hilightedNode.diffParent.num_points >= ignore_component_size  && !hilightedNode.isSameAsChild) {

					updateHilight(hilightedNode.diffParent);
				}				
			}
			if( keyCode == DOWN ) {
				if( hilightedNode != null && 
						!hilightedNode.diffChildren.isEmpty() ) {
					
					
					TopoTreeNode node = null;
					
					// search the children for a selected node
					for( TopoTreeNode child : hilightedNode.diffChildren )
						//node = (node == null )?((child.selected&&child.num_points >= ignore_component_size)?child:null):node;
						node = (node == null )?((child.num_points >= ignore_component_size && !child.isSameAsChild)?child:null):node;

					if( node != null ) {
						
						updateHilight( node );
						
					}
				}
			}
			if( keyCode == LEFT ) {
				
				//System.out.println("LEFT");
				if( hilightedNode != null && hilightedNode.diffParent != null ) {
					
					
					TopoTreeNode node = null;
					
					// search the children for a selected node
					for( TopoTreeNode child : hilightedNode.diffParent.diffChildren ) {
						
						if( child == hilightedNode ) {
							break;
						}
						else if( /*child.selected && */child.num_points >= ignore_component_size && !child.isSameAsChild) {
							node = child;
						}
					}

					if( node != null ) {
						
						updateHilight( node );
					}
				}
			}
			if( keyCode == RIGHT) {
				
				//System.out.println("RIGHT");
				if( hilightedNode != null && hilightedNode.diffParent != null ) {
					
					
					TopoTreeNode node = null;
					boolean haveFoundSelf = false;
					// search the children for a selected node
					for( TopoTreeNode child : hilightedNode.diffParent.diffChildren ) {
						
						if( child == hilightedNode ) {
							haveFoundSelf = true;
						}
						else if( /*child.selected && */haveFoundSelf && child.num_points >= ignore_component_size && !child.isSameAsChild) {
							node = child;
							break;
						}
					}

					if( node != null ) {
						
						updateHilight( node );
					}
				}
			}
		}
		else {
			if( key =='[') {
				
				this.pruneTree(Math.max(0,sel_prune-1));
			}
			if( key ==']') {
				
				this.pruneTree(Math.min(POWERS_OF_TWO-1,sel_prune+1));
			}
		}
	}

	public void mouseMoved() {

		if (mouseY < getHeight() - BOTTOM_CTRL_HEIGHT && mouseY >= 0 && mouseX >= 0
				&& mouseX < getWidth()) {

			int mindist = Integer.MAX_VALUE;
			TopoTreeNode minnode = null;

			for (ArrayList<TopoTreeNode> nodes : m_tt.level_lookup) {
				for (TopoTreeNode node : nodes) {

					if (node.num_points >= ignore_component_size  && !node.isSameAsChild
							/*&& node.selected*/) {

						int dx = mouseX - node.x;
						int dy = mouseY - node.y;
						int dist = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy));
						if (dist <= HI_NODE_SIZE + (int)Math.round(HI_NODE_SIZE * Math.log(node.num_points)) && dist < mindist) {

							minnode = node;
							mindist = dist;
						}
					} 
				}
			}
			
			if( minnode != null ) {

				is_hovering = true;
				hover_text = " " + minnode.num_points + ": ";
				for (SizedLabel sl : node_labeller.getSummaryLabel(minnode)) {

					hover_text += sl.label + " ";
				}
				
				redraw();
			}
		}
	}

	public void mouseClicked() {

		if (mouseY < getHeight() - BOTTOM_CTRL_HEIGHT && mouseY >= 0 && mouseX >= 0
				&& mouseX < getWidth()) {

			int mindist = Integer.MAX_VALUE;
			TopoTreeNode minnode = null;

			for (ArrayList<TopoTreeNode> nodes : m_tt.level_lookup) {
				for (TopoTreeNode node : nodes) {

					if (node.num_points >= ignore_component_size  && !node.isSameAsChild
							/*&& node.selected*/) {

						int dx = mouseX - node.x;
						int dy = mouseY - node.y;
						int dist = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy));
						if (dist <= HI_NODE_SIZE + (int)Math.round(HI_NODE_SIZE * Math.log(node.num_points)) && dist < mindist) {

							minnode = node;
							mindist = dist;
						}
					} 
				}
			}
			
			if( minnode != null ) {
				
				updateHilight(minnode);
			}
		} else {

			if( mouseX > PRUNE_LABEL_WIDTH ) {
				pruneTree(Math
						.min(POWERS_OF_TWO,
								(int) ((mouseX-PRUNE_LABEL_WIDTH) / ((getWidth() - HIST_WIDTH) / POWERS_OF_TWO))));
			}			
		}
	}
	
	public void pruneTree( int prune_amount ) {
		
		sel_prune = prune_amount;
		this.setIgnoreComponenetSize((int) Math.pow(2, sel_prune));
		
		redraw();
		
		// tell everyone about it
		for (ChangeListener cl : changeListeners) {

			cl.stateChanged(new ChangeEvent(compon_changed));
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2650830987573047590L;

	@Override
	public void componentHidden(ComponentEvent arg0) {

		// TODO Auto-generated method stub

	}

	@Override
	public void componentMoved(ComponentEvent arg0) {

		// TODO Auto-generated method stub

	}

	@Override
	public void componentResized(ComponentEvent arg0) {

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {

				redraw();
			}
		});
	}

	@Override
	public void componentShown(ComponentEvent arg0) {

	}

	public Dimension getPreferredSize() {

		return new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
	}
	
	
	@Override
	public void stateChanged(ChangeEvent arg0) {

		if( arg0.getSource() instanceof TopoTreeNode ) {
		
			redraw();
		}		
	}


	@Override
	public void tagsChanged() {

//		System.out.println("GOT A TAG EVENT HERE.");
		
		redraw();
	}

}
