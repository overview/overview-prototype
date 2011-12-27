package snappy.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;

import snappy.graph.NodeLabeller;
import snappy.graph.SizedLabel;
import snappy.graph.TagChangeListener;
import snappy.graph.TagTable;
import snappy.graph.TagTable.Tag;
import snappy.graph.TopoTreeNode;

/*
 * Control for displaying and listening to selections in the topological tree of the graph
 * will display tree nodes as parent nodes with summary strings and tree leaves as leaf nodes 
 * with group intersection strings.
 * 
 * The icons of the nodes reflect tag membership
 */
public class NodeTree extends JPanel implements ListSelectionListener,
		TagChangeListener {

	boolean ignore_selection_events = false;
	TagTable m_ttable = null;

	public JList item_jlist = null;
	public JList node_jlist = null;

	NodeLabeller node_labeller = null;
	JButton clear_button = null;

	ArrayList<ChangeListener> changeListeners = null;
	ArrayList<TagChangeListener> tagChangeListeners = null;

	public void addTagChangeListener(TagChangeListener listener) {

		tagChangeListeners.add(listener);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -4957324767502224293L;

	public void doLayout() {

		int width = this.getWidth();
		int height = this.getHeight();
		Insets insets = this.getInsets();
		int myWidth = (width - insets.left) - insets.right;
		int myHeight = (height - insets.top) - insets.bottom;

		title_label.setBounds(insets.left, insets.top,
				title_label.getPreferredSize().width,
				title_label.getPreferredSize().height);

		clear_button.setBounds(
				myWidth - insets.right - clear_button.getPreferredSize().width,
				insets.top, clear_button.getPreferredSize().width,
				title_label.getPreferredSize().height);

		scrollPane_items.setBounds(insets.left + (myWidth / 2) +2, insets.top
				+ title_label.getPreferredSize().height + 5, (myWidth / 2) - 2,
				myHeight - insets.top - title_label.getPreferredSize().height
						- 5);

		scrollPane_nodes.setBounds(insets.left,
				insets.top + title_label.getPreferredSize().height + 5,
				myWidth / 2 - 3,
				myHeight - insets.top - title_label.getPreferredSize().height
						- 5);
	}

	JLabel title_label = null;
	JScrollPane scrollPane_items = null;
	JScrollPane scrollPane_nodes = null;

	public NodeTree(NodeLabeller node_labeller, TagTable ttable) {

		super();

		this.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 1, 1, PrettyColors.Grey),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		this.setBackground(Color.white);

		title_label = new JLabel("Active Set List");
		title_label.setForeground(PrettyColors.DarkGrey);
		title_label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		this.add(title_label);

		clear_button = new JButton("CLEAR");
		clear_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				item_jlist.setModel(new NodeTreeListModel());
				node_jlist.setModel(new NodeTreeListModel());
				item_jlist.validate();
				node_jlist.validate();
			}
		});
		this.add(clear_button);

		// construct an empty tree

		this.m_ttable = ttable;
		m_ttable.addTagChangeListener(this);
		this.node_labeller = node_labeller;

		item_jlist = new JList();
		node_jlist = new JList();

		item_jlist.addListSelectionListener(this);
		node_jlist.addListSelectionListener(this);

		item_jlist
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		node_jlist
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		item_jlist.setCellRenderer(new TopoNodeCellRenderer());
		node_jlist.setCellRenderer(new TopoNodeCellRenderer());

		scrollPane_items = new JScrollPane(item_jlist);
		scrollPane_nodes = new JScrollPane(node_jlist);
		
		this.add(scrollPane_items);
		this.add(scrollPane_nodes);

		tagChangeListeners = new ArrayList<TagChangeListener>();
		changeListeners = new ArrayList<ChangeListener>();

		item_jlist.setModel(new NodeTreeListModel());
		node_jlist.setModel(new NodeTreeListModel());
	}

	public ArrayList<TopoTreeNode> getActiveNodeSet() {
		
		ArrayList<TopoTreeNode> active_set = new ArrayList<TopoTreeNode>();

		for( Object o : ((NodeTreeListModel)node_jlist.getModel()).inner_data ) {
			active_set.add((TopoTreeNode)o);
		}

		return active_set;
	}
	public ArrayList<Integer> getActiveSet() {

		ArrayList<Integer> active_set = new ArrayList<Integer>();

		for( Object o : ((NodeTreeListModel)item_jlist.getModel()).inner_data ) {
			active_set.add((Integer)o);
		}

		return active_set;
	}

	public void addChangeListener(ChangeListener cl) {

		this.changeListeners.add(cl);
	}

	public class TopoNodeCellIcon implements Icon {

		ArrayList<Color> m_colors = null;
		boolean m_isNode = false;

		Icon openIcon = null;
		Icon leafIcon = null;

		public TopoNodeCellIcon(ArrayList<Color> colors, boolean isNode,
				Icon openIcon, Icon leafIcon) {

			m_isNode = isNode;

			if (colors == null) {

				m_colors = null;
			} else {
				m_colors = colors;
			}

			this.openIcon = openIcon;
			this.leafIcon = leafIcon;
		}

		@Override
		public int getIconHeight() {

			return Math.max(
					16,
					m_isNode ? openIcon.getIconHeight() : leafIcon
							.getIconHeight());
		}

		@Override
		public int getIconWidth() {

			return 16
					* m_colors.size()
					+ (m_isNode ? openIcon.getIconWidth() : leafIcon
							.getIconWidth());
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {

			for (int i = 0; i < m_colors.size(); i++) {

				g.setColor(m_colors.get(i));
				g.fillRect(x + i * 16, y, 16, 16);
			}
			if (m_isNode) {

				openIcon.paintIcon(c, g, x + m_colors.size() * 16, y);
			} else {

				leafIcon.paintIcon(c, g, x + m_colors.size() * 16, y);
			}
		}
	}

	public class IconGrabTreeCellRenderer extends DefaultTreeCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7220436670014559754L;

		public Icon getOpenIcon() {

			return openIcon;
		}

		public Icon getLeafIcon() {

			return leafIcon;
		}
	}

	public class TopoNodeCellRenderer extends DefaultListCellRenderer {

		private IconGrabTreeCellRenderer m_innerIconGrabber = null;

		/**
		 * 
		 */
		private static final long serialVersionUID = -1808991006152654882L;

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);

			this.setBackground(isSelected ? Color.black : Color.white);
			this.setForeground(isSelected ? Color.white : Color.black);
			
			m_innerIconGrabber = new IconGrabTreeCellRenderer();

			if (value instanceof Integer) {

				this.setText(node_labeller.getLabel(((Integer) value)
						.intValue()));

				// set the icon for the object
				ArrayList<Color> color_temp = new ArrayList<Color>();
				for (Tag t : m_ttable.tag_queue) {

					if (t.items.contains((Integer) value)) {
						if (!t.is_item && !t.is_select) {

							color_temp.add(t.tag_color);
						}
					}
				}
				this.setIcon(new TopoNodeCellIcon(color_temp, false,
						m_innerIconGrabber.getOpenIcon(), m_innerIconGrabber
								.getLeafIcon()));

			} else if (value instanceof TopoTreeNode) {

				TopoTreeNode topo_node = (TopoTreeNode) value;
				String summaryLabel = " " + topo_node.num_points + ": ";
				for (SizedLabel sl : node_labeller.getSummaryLabel(topo_node)) {

					summaryLabel += sl.label + " ";
				}

				this.setText(summaryLabel);

				ArrayList<Color> color_temp = new ArrayList<Color>();
				for (Tag t : m_ttable.tag_queue) {

					if (t.part_components.contains(topo_node)
							|| t.full_components.contains(topo_node)) {
						if (!t.is_item && !t.is_select) {

							color_temp.add(t.tag_color);
						}
					}
				}
				this.setIcon(new TopoNodeCellIcon(color_temp, true,
						m_innerIconGrabber.getOpenIcon(), m_innerIconGrabber
								.getLeafIcon()));
			}

			return this;
		}
	}

	// @Override
	// public void selectionChanged(ArrayList<TopoTreeNode> nodes,
	// TopoTreeNode hilighted,
	// boolean selChanged,
	// boolean hiChanged) {
	//
	// if( selChanged ) {
	// // build a model
	// DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	//
	// boolean node_in_selection = false;
	// DefaultMutableTreeNode hi_node = null;
	//
	// if (nodes != null) {
	//
	// for (TopoTreeNode node : nodes) {
	//
	// DefaultMutableTreeNode parent = new DefaultMutableTreeNode(node);
	// for (int i = 0; i < node.num_points; i++) {
	//
	// parent.add(new DefaultMutableTreeNode(node.component.get(i)));
	// }
	// root.add(parent);
	// if( node == hilighted ) {
	// node_in_selection = true;
	// hi_node = parent;
	// }
	// }
	// }
	//
	// if( ! node_in_selection && hilighted != null ) {
	//
	// hi_node = new DefaultMutableTreeNode(hilighted);
	// for (int i = 0; i < hilighted.num_points; i++) {
	//
	// hi_node.add(new DefaultMutableTreeNode(hilighted.component.get(i)));
	// }
	// root.add(hi_node);
	// }
	//
	// TreeModel my_model = new DefaultTreeModel(root);
	//
	// tree.setModel(my_model);
	// if( hilighted != null ) {
	//
	// TreePath path = new TreePath(hi_node.getPath());
	// tree.setSelectionPath(path);
	// tree.scrollPathToVisible(path);
	// }
	// this.getParent().validate();
	// } else if (hiChanged) {
	//
	// DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel()
	// .getRoot();
	// for (int i = 0; i < root.getChildCount(); i++) {
	//
	// DefaultMutableTreeNode cnode = (DefaultMutableTreeNode) root
	// .getChildAt(i);
	// if (cnode.getUserObject() == hilighted) {
	//
	// TreePath path = new TreePath(cnode.getPath());
	// tree.setSelectionPath(path);
	// tree.scrollPathToVisible(path);
	// }
	// }
	// }
	// }

	@Override
	public void tagsChanged() {

		Comparator<TopoTreeNode> comparator = new Comparator<TopoTreeNode>() {

			@Override
			public int compare(TopoTreeNode o1, TopoTreeNode o2) {
				
				return o1.level - o2.level;
			}
		}; 

//		 System.out.println("BEGIN NODETREE TAGSCHANGED");

		if (m_ttable.topTag().is_item) {

			ignore_selection_events = true;

			HashSet<Integer> A_U_B_item = new HashSet<Integer>(
					m_ttable.getItemTag().items);
			for (int i = 0; i < item_jlist.getModel().getSize(); i++) {
				if( !A_U_B_item.contains( item_jlist.getModel().getElementAt(i)) ){
				
					A_U_B_item.add((Integer) item_jlist.getModel().getElementAt(i));
				}
			}
			ArrayList<Integer> C_item = new ArrayList<Integer>(m_ttable.getItemTag().items);
			ArrayList<Integer> D_item = new ArrayList<Integer>();
			for( Integer k : A_U_B_item ) {
				
				if( !m_ttable.getItemTag().items.contains(k) ) {
				
					D_item.add( k );
				}				
			}
			item_jlist.clearSelection();  // clear selection
//			((DefaultListModel)item_jlist.getModel()).clear();  // clear model
			NodeTreeListModel temp_item_model = new NodeTreeListModel();
			temp_item_model.addSlew(C_item);
			temp_item_model.addSlew(D_item);
			item_jlist.setModel(temp_item_model);
			if( C_item.size() > 0 ) {
				item_jlist.setSelectionInterval(0, C_item.size()-1);
				item_jlist.ensureIndexIsVisible(0);
			}
			
			// now handle the node list

			HashSet<TopoTreeNode> A_U_B_node = new HashSet<TopoTreeNode>();
			for( TopoTreeNode ttNode : m_ttable.getItemTag().full_components ) {
				
				if( ttNode.num_points >= TopoTreeControl.ignore_component_size && !ttNode.isSameAsChild ) {
					
					A_U_B_node.add(ttNode);
				}
			}
			
			for (int i = 0; i < node_jlist.getModel().getSize(); i++) {
				if( !A_U_B_node.contains( node_jlist.getModel().getElementAt(i)) ){
				
					A_U_B_node.add((TopoTreeNode) node_jlist.getModel().getElementAt(i));
				}
			}
			ArrayList<TopoTreeNode> C_node = new ArrayList<TopoTreeNode>();
			ArrayList<TopoTreeNode> D_node = new ArrayList<TopoTreeNode>();
			for( TopoTreeNode ttNode : m_ttable.getItemTag().full_components ) {
				
				if( ttNode.num_points >= TopoTreeControl.ignore_component_size && !ttNode.isSameAsChild ) {
					
					C_node.add(ttNode);
				}
			}
			for( TopoTreeNode ttNode : A_U_B_node ) {
				
				if( !m_ttable.getItemTag().full_components.contains(ttNode) ) {
				
					D_node.add( ttNode );
				}				
			}
			Collections.sort(C_node, comparator );
			Collections.sort(D_node, comparator );
			
			node_jlist.clearSelection();  // clear selection
			NodeTreeListModel temp_node_model = new NodeTreeListModel();
			temp_node_model.addSlew(C_node);
			temp_node_model.addSlew(D_node);
			node_jlist.setModel(temp_node_model);
			if( C_node.size() > 0 ) {
				node_jlist.setSelectionInterval(0, C_node.size()-1);
				node_jlist.ensureIndexIsVisible(0);
			}

			ignore_selection_events = false;
		} else {

			// System.out.println("IN TAG ACTIVATION");

			// change the content of the list

			Tag t = m_ttable.topTag();

			HashSet<TopoTreeNode> comp_lookup = new HashSet<TopoTreeNode>();
			if (t.full_components != null) {

				// add the components
				for (TopoTreeNode node : t.full_components) {

					if (node.num_points >= TopoTreeControl.ignore_component_size
							&& !node.sameAsChild()) {

						if (!comp_lookup.contains(node)) {
							
							comp_lookup.add(node);
						}
					} else {

						TopoTreeNode parent = node.parent;
						while (parent != null
								&& (parent.num_points < TopoTreeControl.ignore_component_size || parent.isSameAsChild) ) {

							parent = parent.parent;
						}
						if (!comp_lookup.contains(parent)) {

							comp_lookup.add(parent);
						}
					}				
				}
				
				ArrayList<TopoTreeNode> A = new ArrayList<TopoTreeNode>(comp_lookup);
				Collections.sort(A, comparator );
				NodeTreeListModel temp_node_model = new NodeTreeListModel(); 
				temp_node_model.addSlew(A);
				node_jlist.setModel(temp_node_model);
				NodeTreeListModel temp_item_model = new NodeTreeListModel(); 
				temp_item_model.addSlew(t.items);
				item_jlist.setModel(temp_item_model);
			}
		}

		item_jlist.repaint();
		node_jlist.repaint();
//		 System.out.println("END NODETREE TAGSCHANGED");

	}

	public class NodeTreeListModel extends AbstractListModel {

		public ArrayList<Object> inner_data = null;
		
		public NodeTreeListModel( ) {
			
			inner_data = new ArrayList<Object>(); 
		}
		
		public <E> void addSlew( Collection<E> coll ) {
			
			if( coll.size() < 1 ) {
				return;
			}
			
			int nold = inner_data.size();
			for( E e : coll ) {
				
				inner_data.add( e );
			}
			
			fireIntervalAdded(this, nold, nold+coll.size() - 1);
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 7376468525801631921L;

		@Override
		public Object getElementAt(int arg0) {
			
			if( inner_data.size()-1 < arg0 ) {
				
				return null;
			}
			
			return inner_data.get(arg0);
		}

		@Override
		public int getSize() {

			// TODO Auto-generated method stub
			return inner_data.size();
		}
		
	}
	
	@Override
	public void valueChanged(ListSelectionEvent arg0) {

//		 System.out.println("BLAH1");
		if( arg0.getValueIsAdjusting() )
			return;
		
		// if we're changing the selection, then don't do anything
		
		if (ignore_selection_events)
			return;

		Comparator<TopoTreeNode> comparator = new Comparator<TopoTreeNode>() {

			@Override
			public int compare(TopoTreeNode o1, TopoTreeNode o2) {
				
				return o1.level - o2.level;
			}
		}; 
		
		if( arg0.getSource() == item_jlist ) {
			
			int[] indices = item_jlist.getSelectedIndices();
			
			m_ttable.getItemTag().removeItem(
					new ArrayList<Integer>(m_ttable.getItemTag().items));
			
			if( indices.length > 0 ) {
				
				ArrayList<Integer> itemsToAdd = new ArrayList<Integer>();
				for( int index : indices ) {

					itemsToAdd.add((Integer)item_jlist.getModel().getElementAt(index));
				}
				m_ttable.getItemTag().addItem(itemsToAdd);
				m_ttable.promoteTagSilent(m_ttable.getItemTag());
			}
			
			// change the component list
			
			ignore_selection_events = true;
			if( ((NodeTreeListModel)node_jlist.getModel()).getSize() > 0 ) {
				
				ArrayList<TopoTreeNode> C_node = new ArrayList<TopoTreeNode>();
				ArrayList<TopoTreeNode> D_node = new ArrayList<TopoTreeNode>();
				for( TopoTreeNode ttNode : getActiveNodeSet() ) {
					
					if (indices.length > 0
							&& ttNode.component.contains((Integer) item_jlist
									.getModel().getElementAt(indices[0]))) {
						
						C_node.add(ttNode);
					}
					else {
						
						D_node.add(ttNode);
					}
				}
				Collections.sort(C_node, comparator );
				Collections.sort(D_node, comparator );
				
				node_jlist.clearSelection();  // clear selection
				NodeTreeListModel temp_node_model = new NodeTreeListModel();
				temp_node_model.addSlew(C_node);
				temp_node_model.addSlew(D_node);
				node_jlist.setModel(temp_node_model);
				if( C_node.size() > 0 ) {
					node_jlist.setSelectionInterval(0, C_node.size()-1);
					node_jlist.ensureIndexIsVisible(0);
				}

				ignore_selection_events = false;
			}
			
			for (TagChangeListener tagChangeListener : tagChangeListeners) {

				tagChangeListener.tagsChanged();
			}
		}
		else if( arg0.getSource() == node_jlist ) {
			
			int[] indices = node_jlist.getSelectedIndices();
			
			m_ttable.getItemTag().removeItem(
					new ArrayList<Integer>(m_ttable.getItemTag().items));
			if( indices.length > 0 ) {
				
				for( int index : indices ) {
					
					m_ttable.getItemTag().addComponent((TopoTreeNode)node_jlist.getModel().getElementAt(index));
				}
				m_ttable.promoteTagSilent(m_ttable.getItemTag());

			}
			
			// change the component list
			
			ignore_selection_events = true;
			
			ArrayList<Integer> C_item = new ArrayList<Integer>();
			ArrayList<Integer> D_item = new ArrayList<Integer>();
			TopoTreeNode topoNode = indices.length<1?null:(TopoTreeNode)node_jlist.getModel().getElementAt(indices[0]);
			HashSet<Integer> component = topoNode!=null?(new HashSet<Integer>(topoNode.component)):null;
			for( Integer k : getActiveSet() ) {
				
				if( topoNode != null && component.contains(k) ) {
					
					C_item.add( k );
				}
				else {
				
					D_item.add( k );
				}				
			}
			item_jlist.clearSelection();  // clear selection
			NodeTreeListModel temp_item_model = new NodeTreeListModel();
			temp_item_model.addSlew(C_item);
			temp_item_model.addSlew(D_item);
			item_jlist.setModel(temp_item_model);
			if( C_item.size() > 0 ) {
				item_jlist.setSelectionInterval(0, C_item.size()-1);
				item_jlist.ensureIndexIsVisible(0);
			}
			
			ignore_selection_events = false;
			
			for (TagChangeListener tagChangeListener : tagChangeListeners) {

				tagChangeListener.tagsChanged();
			}
		}
//		 System.out.println("BLAH2");
	}
}
