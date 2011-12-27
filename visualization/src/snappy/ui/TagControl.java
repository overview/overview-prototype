package snappy.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;

import snappy.graph.TagTable;
import snappy.graph.TagChangeListener;
import snappy.graph.TopoTree;
import snappy.graph.TagTable.Tag;
import snappy.graph.TopoTreeNode;

public class TagControl extends JPanel implements ActionListener, TagChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3457881159438566542L;

	Point lastpoint = null;
	JTextField newTagField = null;
	JButton newTagButton = null;
	TagTable m_ttable = null;
	JButton save_button = null;
	File m_chosenFile = null;
	JLabel title_label = null;
	TagList m_tagList = null;
	NodeTree m_node_tree = null;
	
	class TagIcon implements Icon {

		Color m_color = null;
		
		public TagIcon( Color myColor ) {
			
			m_color = myColor;
		}
		
		@Override
		public int getIconHeight() {

			return 16;
		}

		@Override
		public int getIconWidth() {

			return 16;
		}

		@Override
		public void paintIcon(Component arg0, Graphics arg1, int arg2, int arg3) {

			arg1.setColor(m_color);
			arg1.fillRect(arg2+3,arg3+3, 10, 10);
		}
		
	}
	
	public void doLayout() {
		
		int width = getWidth();
		int height = getHeight();
		Insets insets = getInsets();
		int myWidth = (width - insets.left) - insets.right;
		int myHeight = (height - insets.top) - insets.bottom;
		
		// place title entry
		
		title_label.setBounds(insets.left, insets.top, myWidth, title_label.getPreferredSize().height);
		
		// place tag entry
		
		newTagField.setBounds(insets.left,
				insets.top + title_label.getPreferredSize().height + 5, myWidth
						- (newTagButton.getPreferredSize().width + 5),
				newTagField.getPreferredSize().height);

		newTagButton.setBounds(
				insets.left + myWidth
						- (newTagButton.getPreferredSize().width + 5),
				insets.top + title_label.getPreferredSize().height + 5,
				newTagButton.getPreferredSize().width,
				newTagButton.getPreferredSize().height);
		
		int taglist_height = myHeight
				- (title_label.getPreferredSize().height
						+ newTagButton.getPreferredSize().height
						+ save_button.getPreferredSize().height + 15);

		m_tagList.setBounds(insets.left,
				insets.top + title_label.getPreferredSize().height
						+ newTagButton.getPreferredSize().height + 10, myWidth,
				taglist_height);
		
		save_button.setBounds(insets.left,
				insets.top + title_label.getPreferredSize().height
						+ newTagButton.getPreferredSize().height
						+ taglist_height + 15, myWidth,
				save_button.getPreferredSize().height);
	}
	
	public Dimension getPreferredSize() {
		
		Dimension d = new Dimension();
		
		// get the preferred size
		
		return d;
	}
	
	public TagControl(TagTable ttable) {
		super();
		
		m_ttable = ttable;
		m_ttable.addTagChangeListener(this);
		
		this.setBorder( BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,1,PrettyColors.Grey), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		this.setBackground(Color.white);
		
		newTagField = new JTextField();
		newTagField.setColumns(80);
		newTagField.addActionListener( new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {

				newTagButton.doClick();
			}
		});
		newTagButton = new JButton("NEW");
		newTagButton.addActionListener(this);
		
		save_button = new JButton("SAVE");
		save_button.addActionListener(this);
		title_label = new JLabel("Tags View");
		title_label.setForeground(PrettyColors.DarkGrey);
		
		m_tagList = new TagList(m_ttable);
		
		this.add(title_label);
		this.add(newTagField);
		this.add(newTagButton);
		this.add(save_button);
		this.add(m_tagList);
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {

		if( arg0.getSource() == newTagButton ) {
			
			// add the tag to the (if it doesn't already exist
			
			if( newTagField.getText().length() > 0 ) {

				m_ttable.newTag(newTagField.getText());
			}
		}
		if( arg0.getSource() == save_button ) {

			JFileChooser chooser = null;
			if( m_chosenFile == null ) {
				chooser = new JFileChooser();
			}
			else {
				chooser = new JFileChooser(m_chosenFile);
			}
			int retval = chooser.showSaveDialog(this);
			if( retval == JFileChooser.APPROVE_OPTION) {
				
				m_chosenFile = chooser.getSelectedFile();
				try {
					m_ttable.saveTagFile(m_chosenFile.getCanonicalPath());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		
	}

	@Override
	public void tagsChanged() {

//		System.out.println("BEGIN TAG CONTROL TAGSCHANGED");
		
		this.remove(m_tagList);
		m_tagList = new TagList(m_ttable);
		this.add(m_tagList);
		this.validate();
		
//		System.out.println("END TAG CONTROL TAGSCHANGED");
	}
	
	public class TagList extends JPanel {
	
		/**
		 * 
		 */
		private static final long serialVersionUID = 8165545312676025002L;
		
		TagListItem nav_item = null;
		//TagListItem hi_item = null;
		JScrollPane t_list = null;
		
		ArrayList<TagListItem> other_tags = null;
		
		int v_spacing = 0;
		JPanel tag_p = null;
		public void doLayout() {

			int width = this.getWidth();
			int height = this.getHeight();
			Insets insets = this.getInsets();
			int myWidth = (width - insets.left) - insets.right;
			int myHeight = (height - insets.top) - insets.bottom;
			
			// place the top two items
			
			nav_item.setBounds(insets.left, insets.top, myWidth, nav_item.getPreferredSize().height);
//			hi_item.setBounds(insets.left, nav_item.getPreferredSize().height + v_spacing, 
//					myWidth, nav_item.getPreferredSize().height);
			
			// place the scroll pane
			t_list.setBounds(insets.left, insets.top + (nav_item.getPreferredSize().height + v_spacing), 
					myWidth, myHeight - (2*(nav_item.getPreferredSize().height + v_spacing)));
			
			if( m_ttable.topTag() != null ) {
				
				for(TagListItem tli : m_tagList.other_tags) {
					
					if(tli.m_t == m_ttable.topTag()) {
						
						t_list.getViewport().setViewPosition(lastpoint);
					}
				}
			}
		}
		
		public TagList( TagTable ttable ) {
			
			this.setBackground(Color.white);
			this.setBorder(BorderFactory.createLineBorder(PrettyColors.DarkGrey));

			nav_item = new TagListItem( ttable.topTag() == ttable.getSelTag(), ttable.getSelTag() );
//			hi_item = new TagListItem( ttable.topTag() == ttable.getItemTag(), ttable.getItemTag() );
			other_tags = new ArrayList<TagControl.TagListItem>();
			for( int i =0; i < ttable.tag_order_added.size(); i++ ) {

				Tag t = ttable.tag_order_added.get(i);
				if( ! t.is_item && ! t.is_select ) {
					
					other_tags.add( new TagListItem(ttable.topTag() == t, t) );
				}
			}
			
			tag_p = new JPanel() {
				
				public void doLayout() {
					
					int width = this.getWidth();
					int height = this.getHeight();
					Insets insets = this.getInsets();
					int myWidth = (width - insets.left) - insets.right;
					int myHeight = (height - insets.top) - insets.bottom;

					for( int i = 0; i < other_tags.size(); i++ ) {
						
						other_tags
								.get(i)
								.setBounds(
										0,
										i * (nav_item.getPreferredSize().height + v_spacing),
										nav_item.getPreferredSize().width-10,
										nav_item.getPreferredSize().height);
					}
				}
				
				public Dimension getPreferredSize() {
					
					return new Dimension(
							nav_item.getPreferredSize().width-10,
							Math.max(
									0,
									(other_tags.size() - 1)
											* v_spacing
											+ other_tags.size()
											* nav_item.getPreferredSize().height));
				}
			};
			
			tag_p.setBackground(Color.white);
			
			for( TagListItem tli : other_tags ) {
				
				tag_p.add( tli );
			}
			
			t_list = new JScrollPane( tag_p );
			
			this.add( nav_item );
//			this.add( hi_item );
			this.add( t_list );
		}
	}
	
	public class TagListItem extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1171912782223643656L;
		
		JButton kill_button;
		JButton add_to_button;
		JButton take_from_button;
		JLabel nombre_label;
		Tag m_t = null;
		boolean m_isTop = false;
		int tag_hard_width = 290;
		
		public TagListItem( boolean isTop, Tag t ) {
					
			m_isTop = isTop;
			m_t = t;
			kill_button = new JButton("X");
			add_to_button = new JButton("+");
			take_from_button = new JButton("-");
			
			add_to_button.addActionListener( new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {

					lastpoint = m_tagList.t_list.getViewport().getViewPosition();
					m_ttable.addFromActiveSet(m_t, m_node_tree.getActiveSet());
				}
			});
			take_from_button.addActionListener( new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {

					lastpoint = m_tagList.t_list.getViewport().getViewPosition();
					m_ttable.remFromActiveSet(m_t, m_node_tree.getActiveSet());
				}
			});
			kill_button.addActionListener( new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {

					lastpoint = m_tagList.t_list.getViewport().getViewPosition();
					m_ttable.killTag(m_t);
				}
			});
			
			Icon tag_icon = new TagIcon(m_t.tag_color);
			nombre_label = new JLabel(m_t.name, tag_icon, JLabel.LEFT );
			nombre_label.addMouseListener(new MouseAdapter() {
				
				@Override
				public void mouseClicked(MouseEvent arg0) {

					lastpoint = m_tagList.t_list.getViewport().getViewPosition();
					m_ttable.promoteTag(m_t);
				}
			});
			if( isTop ) {
				
				this.setBackground(m_t.tag_color);
				nombre_label.setBackground( m_t.tag_color );
				nombre_label.setForeground( Color.white );
			}
			else {
				this.setBackground(Color.white);
				nombre_label.setBackground(Color.white);
				nombre_label.setForeground(Color.black);
			}
			
			if( ! ( t.is_item || t.is_select ) ) {
				
				this.add(kill_button);
				this.add(add_to_button);
				this.add(take_from_button);
			}
			
			this.add(nombre_label);
		}
		
		public Dimension getPreferredSize() {
			
			return new Dimension(tag_hard_width, add_to_button.getPreferredSize().height);
		}
		
		public void doLayout() {
			
			int width = this.getWidth();
			int height = this.getHeight();
			Insets insets = this.getInsets();
			int myWidth = (width - insets.left) - insets.right;
			int myHeight = (height - insets.top) - insets.bottom;
			int button_spacer = 2;
			
			int max_button_width = Math.max( kill_button.getPreferredSize().width, add_to_button.getPreferredSize().width);
			max_button_width = Math.max( max_button_width, take_from_button.getPreferredSize().width);
			max_button_width /= 2;
			int button_height = kill_button.getPreferredSize().height;
			
			// layout label
			
			int label_width = tag_hard_width - (2 * button_spacer + 3 * max_button_width ) - 10;
			nombre_label.setBounds( 0,0, label_width, button_height ); 
			
			// layout buttons
			
			if( ! ( m_t.is_item || m_t.is_select ) ) {
				
				add_to_button.setBounds( label_width,0, max_button_width, button_height ); 
				take_from_button.setBounds( label_width + button_spacer + max_button_width,0, max_button_width, button_height ); 
				kill_button.setBounds( label_width + 2*(button_spacer + max_button_width),0, max_button_width, button_height ); 			
			}
		}
	}
	
	public static void main( String[] args ) {
		
		// construct a Jframe
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	
        		// build a simple table

        		// create test tree
        		
        		TopoTree test_tree = new TopoTree();
        		test_tree.num_levels = 3;
        		TopoTreeNode a = new TopoTreeNode();
        		a.component = new ArrayList<Integer>();
        		TopoTreeNode b = new TopoTreeNode();
        		b.component = new ArrayList<Integer>();
        		TopoTreeNode c = new TopoTreeNode();
        		c.component = new ArrayList<Integer>();
        		TopoTreeNode d = new TopoTreeNode();
        		d.component = new ArrayList<Integer>();
        		TopoTreeNode e = new TopoTreeNode();
        		e.component = new ArrayList<Integer>();
        		TopoTreeNode f = new TopoTreeNode();
        		f.component = new ArrayList<Integer>();
        		TopoTreeNode g = new TopoTreeNode();
        		g.component = new ArrayList<Integer>();
        		TopoTreeNode h = new TopoTreeNode();
        		h.component = new ArrayList<Integer>();

        		a.component.add(1);
        		a.component.add(2);
        		a.component.add(3);
        		a.component.add(4);
        		a.component.add(5);
        		a.children.add(b);
        		a.children.add(c);
        		a.parent = null;
        		
        		b.parent = a;
        		b.children.add(d);
        		b.children.add(e);
        		b.component.add(1);
        		b.component.add(2);
        		
        		c.parent = a;
        		c.children.add(f);
        		c.children.add(g);
        		c.children.add(h);
        		c.component.add(3);
        		c.component.add(4);
        		c.component.add(5);
        		
        		d.parent = b;
        		d.component.add(1);
        		
        		e.parent = b;
        		e.component.add(2);
        		
        		f.parent = c;
        		f.component.add(3);
        		
        		g.parent = c;
        		g.component.add(4);
        		
        		h.parent = c;
        		h.component.add(5);

        		test_tree.roots.add(a);
        		
        		System.out.println("Created test tree");
        		
        		TagTable test_table = new TagTable(test_tree);
        		test_table.newTag("TEST1");
        		
        		TagControl tagC = new TagControl(test_table);
            	
            	JFrame frame = new JFrame("TEST TAG CONTROL");
            	frame.getContentPane().setLayout(new BorderLayout(5,5));
            	frame.getContentPane().add(tagC,"Center");
            	frame.setSize(300, 400);
            	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            	frame.setVisible(true);
            }
        } );
	}
}
