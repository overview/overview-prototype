package snappy.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.PApplet;

import snappy.graph.GlimmerLayout;
import snappy.graph.NodeLabeller;
import snappy.graph.TagChangeListener;
import snappy.graph.TagTable;
import snappy.graph.TagTable.Tag;
import snappy.graph.TopoTree;
import snappy.graph.TopoTreeNode;

public class GlimmerDrawer extends JPanel implements TagChangeListener, ChangeListener, ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9002807048822851883L;
	GlimmerLayout m_glimmer_layout = null;
	TopoTree m_topo_tree = null;
	Thread m_layout_thread = null;
	GlimmerCanvas draw_panel = null;
	JPanel control_panel = null;
	TagTable m_tag_table = null;
	JCheckBox draw_edges_box = null;
	JCheckBox draw_labels_box = null;
	JSlider power_slider = null;
	JSlider point_size_slider = null;
	public NodeLabeller node_labeller = null;

	JButton start_stop_button = null;

	Color bkgndColor = Color.WHITE;
	Color defaultColor = PrettyColors.DarkGrey;
	Color selectedColor = Color.BLACK;
	Color hilightColor = PrettyColors.Red;
	int boundary = 10;
	int point_radius = 7;
	boolean is_paused = false;
	int frame_rate = 2;
	JLabel squeeze_label = null;
	JLabel pointsize_label = null;
	JLabel title_label = null;

	ArrayList<TagChangeListener> tagChangeListeners = null; 
	public void addTagChangeListener( TagChangeListener listener ) {
		
		tagChangeListeners.add( listener );
	}
	
	public void doLayout() {

		int width = getWidth();
		int height = getHeight();
		Insets insets = getInsets();
		int myWidth = (width - insets.left) - insets.right;
		int myHeight = (height - insets.top) - insets.bottom;
		
		int title_height = title_label.getPreferredSize().height;
		int control_panel_height = (int)control_panel.getPreferredSize().getHeight();
		int mds_height = myHeight - control_panel_height - title_height - 10;
		
		title_label.setBounds(insets.left, insets.top, myWidth, title_height);
		draw_panel.setBounds(insets.left, insets.top + title_height + 5, myWidth, myHeight - control_panel_height - title_height - 10 );
		control_panel.setBounds(insets.left, insets.top+mds_height+title_height+10, myWidth, control_panel_height);
	}

	
	public GlimmerDrawer(	GlimmerLayout glimmer_layout, 
							TopoTree topo_tree, 
							TagTable tag_table) {

		super();
		
		tagChangeListeners = new ArrayList<TagChangeListener>();
		
		this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 1, PrettyColors.Grey), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		this.setBackground(Color.WHITE);
		
		m_glimmer_layout = glimmer_layout;
		m_topo_tree = topo_tree;
		m_tag_table = tag_table;
		
		m_tag_table.addTagChangeListener(this);
		
		// init components

		draw_panel = new GlimmerCanvas();
		control_panel = new JPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 7927535892217533170L;
			public void doLayout() {
			
				int width = getWidth();
				int height = getHeight();
				Insets insets = getInsets();
				int myWidth = (width - insets.left) - insets.right;
				int myHeight = (height - insets.top) - insets.bottom;
				
				start_stop_button.setBounds(insets.left, insets.top, myWidth/5 - 5, myHeight);
				
/*				
				int larger_label = (int) Math.max(  squeeze_label.getPreferredSize().getWidth(),
													pointsize_label.getPreferredSize().getWidth());	
				
				int larger_height = (int) Math.max(squeeze_label.getPreferredSize().getHeight(), 
						Math.max(pointsize_label.getPreferredSize().getHeight(), 
								Math.max(power_slider.getPreferredSize().getHeight(), 
										point_size_slider.getPreferredSize().getHeight())));				
				squeeze_label.setBounds(insets.left + myWidth/5, insets.top, larger_label, larger_height);
				pointsize_label.setBounds(insets.left + myWidth/5, insets.top + larger_height + 5, 
						larger_label, larger_height);
				power_slider.setBounds(insets.left + myWidth/5 + larger_label, insets.top, 4*myWidth/5 - larger_label, larger_height);
				point_size_slider.setBounds(insets.left + myWidth/5 + larger_label, insets.top + larger_height + 5, 
						4*myWidth/5 - larger_label, larger_height);
*/
			}			
			
			public Dimension getPreferredSize() {
				return new Dimension(5,(int)start_stop_button.getPreferredSize().getHeight());
/*				
				return new Dimension((int)Math.max(  squeeze_label.getPreferredSize().getWidth(),
						pointsize_label.getPreferredSize().getWidth()), (int)Math.max(squeeze_label.getPreferredSize().getHeight(), 
								Math.max(pointsize_label.getPreferredSize().getHeight(), 
										Math.max(power_slider.getPreferredSize().getHeight(), 
												point_size_slider.getPreferredSize().getHeight())))*2+5);
*/	
			}
		};
		control_panel.setBackground(Color.WHITE);
/*		
		squeeze_label = new JLabel("Squeeze");
		pointsize_label = new JLabel("Point Size");
		squeeze_label.setBackground(Color.WHITE);
		pointsize_label.setBackground(Color.WHITE);
		squeeze_label.setHorizontalAlignment(SwingConstants.RIGHT);
		pointsize_label.setHorizontalAlignment(SwingConstants.RIGHT);
*/		
		start_stop_button = new JButton("Cluster!");
		start_stop_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				synchronized (m_layout_thread) {

					if (!m_layout_thread.isAlive()) {
						start_stop_button.setText("STOP");
						is_paused = false;
						m_layout_thread.start();
					} else {
						is_paused = !is_paused;
						if (is_paused) {

							start_stop_button.setText("Cluster!");
						}
						else {
							
							start_stop_button.setText("STOP");
							m_layout_thread.notify();
						}
					}
				}
			}
		});

/*		power_slider = new JSlider(1, 32, GlimmerLayout.POWER_FACTOR);
		power_slider.addChangeListener(this);
		point_size_slider = new JSlider(1,10,point_radius);
		point_size_slider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
			
				point_radius = point_size_slider.getValue();
				draw_panel.redraw();
			}
		});
*/		
		title_label = new JLabel("Items Plot");
		title_label.setForeground(PrettyColors.DarkGrey);
		
//		draw_edges_box = new JCheckBox( "Draw Selected Edges" );
//		draw_labels_box = new JCheckBox( "Draw Selected Labels" );
//		draw_edges_box.setSelected(GlimmerLayout.DRAW_EDGES);
//		draw_labels_box.setSelected(GlimmerLayout.DRAW_LABELS);
//		draw_edges_box.addItemListener(this);
//		draw_labels_box.addItemListener(this);

//		control_panel.add( point_size_slider );
//		control_panel.add( power_slider );
		control_panel.add( start_stop_button );
//		control_panel.add( squeeze_label );
//		control_panel.add( pointsize_label );
		
		this.add(title_label);
		this.add(draw_panel);
		this.add(control_panel);
		
		draw_panel.init();

		// make the layout thread

		m_layout_thread = new Thread(new Runnable() {

			@Override
			public void run() {

				int frame = 0;
				while (true) {
					frame++;
					synchronized (Thread.currentThread()) {
						if (is_paused) {
							try {
								Thread.currentThread().wait();
							} catch (InterruptedException e) {

								e.printStackTrace();
							}
						}
					}
					m_glimmer_layout.updateLayout();
					if( frame % frame_rate == 0 ) {

						draw_panel.redraw();
					}
				}
			}
		});

	}

	@Override
	public void tagsChanged() {
		
//		System.out.println("BEGIN GLIMMER TAGSCHANGED");
		draw_panel.redraw();
//		System.out.println("END GLIMMER TAGSCHANGED");
	}

	public void paintComponent( Graphics g ) {
	
		draw_panel.redraw();
	}
	
	public class GlimmerCanvas extends PApplet {

		int x_offset = 0;
		int y_offset = 0;
		
		int x_initial = 0;
		int y_initial = 0;

		boolean is_dragging_box = false;
		
		int x_initial_box = 0;
		int y_initial_box = 0;
		int x_end_box = 0;
		int y_end_box = 0;

		int x_initial_2 = 0;
		int y_initial_2 = 0;
		
		float x_scaler = 1.f;
		
		boolean shift_key_down = false;
				
		public void fill( Color c ) {
			
			fill( c.getRed(), c.getGreen(), c.getBlue() );
		}
		
		public void stroke( Color c ) {
			
			stroke( c.getRed(), c.getGreen(), c.getBlue() );
		}
		
		public void mouseReleased() {
			
			if( mouseButton == LEFT ) {
				
				is_dragging_box = false;
				redraw();
			}
		}
		public void mouseDragged() {
			
			if( shift_key_down ) {
				
				if( mouseButton == LEFT ) {
					
					x_offset += mouseX - x_initial;
					y_offset += mouseY - y_initial;
					x_initial = mouseX;
					y_initial = mouseY;			
				}
				else {
					
					int x = mouseX - x_initial_2;
					int y = mouseY - y_initial_2;
					x_initial_2 = mouseX;
					y_initial_2 = mouseY;			
	
					x_scaler += (Math.abs(x)>Math.abs(y)?x:y)*0.01;
	//				System.out.println("x_scaler = " + x_scaler);
				}
				
				redraw();
			}
			else {
				
				is_dragging_box = true;
				x_end_box = mouseX;
				y_end_box = mouseY;
				
				// update the tagging
				
				float min_x = Float.MAX_VALUE;
				float max_x = Float.MIN_VALUE;
				float min_y = Float.MAX_VALUE;
				float max_y = Float.MIN_VALUE;

				for (int i = 0; i < m_glimmer_layout.m_embed.length / 2; i++) {

					min_x = Math.min(min_x, m_glimmer_layout.m_embed[i * 2]);
					max_x = Math.max(max_x, m_glimmer_layout.m_embed[i * 2]);
					min_y = Math.min(min_y, m_glimmer_layout.m_embed[i * 2 + 1]);
					max_y = Math.max(max_y, m_glimmer_layout.m_embed[i * 2 + 1]);
				}
				
				float x_trans = (getWidth() - 2 * boundary);
				float y_trans = (getHeight() - 2 * boundary);
				
				int x_small = Math.min(x_initial_box, x_end_box);
				int x_large = Math.max(x_initial_box, x_end_box);
				int y_small = Math.min(y_initial_box, y_end_box);
				int y_large = Math.max(y_initial_box, y_end_box);

				float x_small_space = (((x_small - boundary - x_offset) / x_trans) / x_scaler) + min_x;
				float x_large_space = (((x_large - boundary - x_offset) / x_trans) / x_scaler) + min_x;
				float y_small_space = (((y_small - boundary - y_offset) / y_trans) / x_scaler) + min_y;
				float y_large_space = (((y_large - boundary - y_offset) / y_trans) / x_scaler) + min_y;
				
				// determine the thing we're under

				ArrayList<Integer> bounded_items = new ArrayList<Integer>();
				for (int i = 0; i < m_glimmer_layout.m_embed.length / 2; i++) {

					// translate mouse clicks to item space
					if( m_glimmer_layout.m_embed[i * 2] >= x_small_space && m_glimmer_layout.m_embed[i * 2] <= x_large_space 
							&& m_glimmer_layout.m_embed[i * 2 + 1] >= y_small_space && m_glimmer_layout.m_embed[i * 2 + 1] <= y_large_space ) {
						
						bounded_items.add(i);
					}
				}
				
				// update the listed items
				m_tag_table.getListedTag().setItems(bounded_items);				
				m_tag_table.promoteTag(m_tag_table.getListedTag());
				
				redraw();
			}
			
		}
		
		public void keyPressed() {
			
			if (key == CODED) {
				if (keyCode == SHIFT) {
					
					shift_key_down = true;
				}
			}
		}
		public void keyReleased() {
			
			if (key == CODED) {
				if (keyCode == SHIFT) {
					
					shift_key_down = false;
				}
			}			
		}
		
		public void mousePressed() {
			
			if( mouseButton == LEFT ) {
				
				if( shift_key_down ) {
					x_initial = mouseX;
					y_initial = mouseY;
				}
				else {
					
					x_initial_box = mouseX;
					y_initial_box = mouseY;
				}
			}
			else {
				
				x_initial_2 = mouseX;
				y_initial_2 = mouseY;
			}
		}
		
		public void mouseClicked() {
			
			if( !shift_key_down ) {
				
				float min_x = Float.MAX_VALUE;
				float max_x = Float.MIN_VALUE;
				float min_y = Float.MAX_VALUE;
				float max_y = Float.MIN_VALUE;

				for (int i = 0; i < m_glimmer_layout.m_embed.length / 2; i++) {

					min_x = Math.min(min_x, m_glimmer_layout.m_embed[i * 2]);
					max_x = Math.max(max_x, m_glimmer_layout.m_embed[i * 2]);
					min_y = Math.min(min_y, m_glimmer_layout.m_embed[i * 2 + 1]);
					max_y = Math.max(max_y, m_glimmer_layout.m_embed[i * 2 + 1]);
				}
				
				float x_trans = (getWidth() - 2 * boundary);
				float y_trans = (getHeight() - 2 * boundary);
				
				float spaceX = (((mouseX - boundary - x_offset) / x_trans) / x_scaler) + min_x;
				float spaceY = (((mouseY - boundary - y_offset) / y_trans) / x_scaler) + min_y;
				float spaceR = ((float) point_radius) / y_trans / x_scaler ;
				
				// determine the thing we're under
				
				int min_idx = -1;
				float min_d = Float.MAX_VALUE;
				for (int i = 0; i < m_glimmer_layout.m_embed.length / 2; i++) {

					// translate mouse clicks to item space
					float dx = m_glimmer_layout.m_embed[i * 2] - spaceX;
					float dy = m_glimmer_layout.m_embed[i * 2 + 1] - spaceY;
					float d = (float) Math.sqrt( dx*dx + dy*dy );
					if( d <= spaceR && d < min_d ) {
						
						min_d = d;
						min_idx = i;
					}
				}
				
				// update the item tag
				
				m_tag_table.getListedTag().removeItem( new ArrayList<Integer>(m_tag_table.getListedTag().items) );

				if(min_idx > -1) {	
					ArrayList<Integer> itemToAdd = new ArrayList<Integer>();
					itemToAdd.add( min_idx );
					m_tag_table.getListedTag().addItem( itemToAdd );
				}
				
				m_tag_table.promoteTagSilent(m_tag_table.getListedTag());

				for( TagChangeListener tagChangeListener : tagChangeListeners ) {
					tagChangeListener.tagsChanged();
				}		
				
				redraw();
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -8333653779999966548L;

		public void setup() {
			
			noSmooth();
			noLoop();

		}
		
		private void drawItem(int item, float min_x, float min_y, float x_trans, float y_trans, int rad) {
			ellipse(
					(int) Math.round(boundary
							+ x_offset +(m_glimmer_layout.m_embed[item * 2] - min_x)
							* x_trans * x_scaler),
					(int) Math.round(boundary
							+ y_offset +(m_glimmer_layout.m_embed[item * 2 + 1] - min_y)
							* y_trans * x_scaler), rad, rad);		
		}
		
		private void drawTag(Tag tag, float min_x, float min_y, float x_trans, float y_trans, int rad) {
			
			Color c = tag.tag_color;
			fill(c);
			
			for( int item : tag.items) {
				drawItem(item, min_x, min_y, x_trans, y_trans, rad);
			}			
		}
		
		public void draw() {

			noStroke();
			
			// clear screen

			background(255);

			// get the bounds of the points

			float min_x = Float.MAX_VALUE;
			float max_x = Float.MIN_VALUE;
			float min_y = Float.MAX_VALUE;
			float max_y = Float.MIN_VALUE;

			for (int i = 0; i < m_glimmer_layout.m_embed.length / 2; i++) {

				min_x = Math.min(min_x, m_glimmer_layout.m_embed[i * 2]);
				max_x = Math.max(max_x, m_glimmer_layout.m_embed[i * 2]);
				min_y = Math.min(min_y, m_glimmer_layout.m_embed[i * 2 + 1]);
				max_y = Math.max(max_y, m_glimmer_layout.m_embed[i * 2 + 1]);
			}

			float x_trans = (getWidth() - 2 * boundary);// / (max_x - min_x);
			float y_trans = (getHeight() - 2 * boundary);// / (max_y - min_y));

			// first draw all not tagged points
			fill( defaultColor );
			for (int i = 0; i < m_glimmer_layout.m_embed.length / 2; i++) {
				
				Color c = m_tag_table.itemColor(i);
				if( c != null )
					continue;
				
				drawItem(i, min_x, min_y, x_trans, y_trans, point_radius);
			}
			
			// draw the tagged points (from back to front.) Use a larger size for the top tag			
			for( int i = m_tag_table.tag_queue.size()-1; i >= 0; i-- ) {
				
				Tag tag = m_tag_table.tag_queue.get(i);
				if (!tag.is_selected) {
					int rad_multiplier = (tag == m_tag_table.topTag()) ? 3 : 1;
					drawTag(tag, min_x, min_y, x_trans, y_trans, point_radius*rad_multiplier);
				}
			}
			
			// draw the selected tag on top of all, always large
			drawTag(m_tag_table.getSelectedTag(), min_x, min_y, x_trans, y_trans, point_radius*3);
			
			fill(defaultColor);

			strokeWeight(1.f);
			
			// draw the selection rect
			if (is_dragging_box) {

				noFill();
				stroke(PrettyColors.DarkGrey);
				int x_small = Math.min(x_initial_box, x_end_box);
				int x_large = Math.max(x_initial_box, x_end_box);
				int y_small = Math.min(y_initial_box, y_end_box);
				int y_large = Math.max(y_initial_box, y_end_box);
				rect(x_small, y_small, x_large - x_small, y_large - y_small);
			}
		}
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
/*
		if( arg0.getSource() == power_slider)  {
			
			GlimmerLayout.POWER_FACTOR = power_slider.getValue();
		}
*/
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
/*
		if( arg0.getSource() == draw_edges_box ) {
			
			GlimmerLayout.DRAW_EDGES = draw_edges_box.isSelected();
		}
		if( arg0.getSource() == draw_labels_box ) {
			
			GlimmerLayout.DRAW_LABELS = draw_labels_box.isSelected();
		}
*/
	}

}
