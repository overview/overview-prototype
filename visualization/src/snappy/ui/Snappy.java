package snappy.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import snappy.data.DistanceFunction;
import snappy.data.DistanceReader;
import snappy.data.FeatureList;
import snappy.data.NZData;
import snappy.data.SparseReader;
import snappy.graph.EdgeIntersectionLabeller;
import snappy.graph.GlimmerLayout;
import snappy.graph.GraphManager;
import snappy.graph.NodeLabeller;
import snappy.graph.SimpleNodeLabeller;
import snappy.graph.TagTable;
import snappy.graph.TopoTree;

//import org.lobobrowser.html.gui.*;
//import org.lobobrowser.html.test.*;

import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser; 

public class Snappy extends JFrame implements ChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6484320090318536140L;
	public static String VERSION_STRING = "0.1.7";	// updated after every commit
	public static int DISTANCE_BINS 	= 25;		// how many bins between 0 ... 1
	
	int default_component_bin = 1;
	
	String nonzero_data_filename = "";		// command line filenames
	String df_data_filename 	 = "";
	String point_label_filename  = "";
	String nz_feature_label_filename = "";
	String url_list_filename = "";
	String tag_filename = "";
	
	float[] current_histo = null;			// current state of the distance histogram
	HistSlider component_slider = null;		// slider that controls which components we see
	HistSlider distance_slider  = null;		// slider that controls which distance we permit in the graph
	TopoTreeControl tt_control = null;		// draws the topological component tree
	SparseReader sparse_reader  = null;		// reads the distance data
	TagControl tag_control 		= null;		// tag widget
	DistanceFunction distance_function = null;
	NZData nz_data 					   = null;

	JPanel tree_panel = null;
	JLabel tt_panel_title = null;
	GraphManager graph_manager = null;		
	TopoTree topo_tree		   = null;
	TagTable tag_table 		   = null;
	float initial_cutoff_value = 1.f;
//	GraphDrawer graph_drawer = null;
	DocList doc_list_control = null;
	HtmlDispatch html_dispatch = null;
//	HtmlPanel html_panel = null;
//	SimpleHtmlRendererContext renderContext = null;
	JPanel html_panel_holder = null;
	GlimmerLayout glimmer_layout = null;
	GlimmerDrawer glimmer_drawer = null;
	
	long startup_seconds = 5000L;
	
//	JWebBrowser webBrowser = null;
	
//	ArrayList<GraphLayout> graph_layouts = null;
	
	boolean is_sparse = false;				// controls if we're reading sparse or not
	boolean is_url_available = false;		// do we have URLs for each do (and can launch them on double-click)
	
	FeatureList point_feature_list = null;
	FeatureList edge_feature_list = null;
	NodeLabeller node_labeller = null;
		
	public class SnappyPanel extends JPanel {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 4961085365793328208L;

		public SnappyPanel() {
			
			super();
			setBackground(Color.WHITE);
		}
		
		public void doLayout() {
			
            int width = getWidth();
            int height = getHeight();
            Insets insets = getInsets();
            int myWidth  = ((width - insets.left) - insets.right);
            int myHeight = (height - insets.top) - insets.bottom;
            
			if( ! is_url_available ) {
				int tag_width = 300;
				int non_tt_width = tag_width + myHeight/2;
				
				tree_panel.setBounds(	insets.left, 
						insets.top, 
						myWidth - non_tt_width, 
						myHeight/2);
				tag_control.setBounds( insets.left + (myWidth - non_tt_width), 
						insets.top , 
						tag_width, 
						myHeight/2 );
				glimmer_drawer.setBounds(insets.left + (myWidth - myHeight/2), insets.top,myHeight/2,myHeight/2 );
				
	            doc_list_control.setBounds(	insets.left, 
						insets.top + myHeight/2, 
						myWidth, 
						myHeight/2);

			}
			else {
				
				int tag_width = 300;
				int non_tt_width = tag_width + myHeight/2;
				
				tree_panel.setBounds(	insets.left, 
						insets.top, 
						myWidth - non_tt_width, 
						myHeight/2);
				tag_control.setBounds( insets.left + (myWidth - non_tt_width), 
						insets.top , 
						tag_width, 
						myHeight/2 );
				glimmer_drawer.setBounds(insets.left + (myWidth - myHeight/2), insets.top,myHeight/2,myHeight/2 );
				
				int doc_list_width = (int) (myWidth * 0.4);
	            doc_list_control.setBounds(	insets.left, 
						insets.top + myHeight/2, 
						doc_list_width, 
						myHeight /2);
	            html_panel_holder.setBounds( insets.left +  doc_list_width + 5, 
						insets.top + myHeight/2, 
						myWidth - doc_list_width, 
						myHeight/2);				
//	            tt_control.setBounds(	insets.left, 
//						insets.top + myHeight/2, 
//						3*myWidth/4, 
//						myHeight/2);
//	            node_tree_control.setBounds(	insets.left, 
//						insets.top, 
//						myWidth, 
//						myHeight/4);
//	            html_panel.setBounds(	insets.left, 
//						insets.top + myHeight/4, 
//						myWidth, 
//						myHeight/4);				
//				tag_control.setBounds( insets.left + 3*myWidth/4, 
//						insets.top + myHeight/2, 
//						myWidth/4, 
//						myHeight/2 );
//				glimmer_drawer.setBounds(insets.left + myWidth, insets.top,myWidth,myHeight );
			}
		}
	}
	
	public void parseArgs(String[] args) {
				
		boolean inNZ 	= false;	// 'N' next argument is for nonzero data
		boolean inDM 	= false;	// 'D' next argument is for distance matrix
		boolean inIND 	= false;	// 'I' next argument is for indifferentiated value
		boolean inPF 	= false;	// 'P' next argument is for point label file
		boolean inNZF 	= false;	// 'Z' next argument is for nonzero label file
		boolean inHTML  = false;    // 'H' next argument is for html url directory
		boolean inTAG   = false;    // 'T' next argument is for tag file
		boolean inSEC   = false;	// 'S' next argument is a long int for millis
		boolean inURL	= false;	// 'S' next argument is file for URLs
		
		int arg_num = 0; 
		
		for( String arg : args ) {
			
			// determine if the argument has a "dash switch"
			
			if( arg.length() > 1 && arg.charAt(0) == '-' ) {

				// the next argument should *not* be a dash if we're expecting input
				
				if( inNZ || inDM || inIND || inHTML || inTAG || inSEC ) {
					
					System.out.println("PARSE ERROR: Trouble parsing argument \"" + arg + "\"");
					System.exit(0);
				}

				// version switch
				
				if( arg.charAt(1) == 'V' ) {
					
					System.out.println("Snappy Version " + Snappy.VERSION_STRING );
					System.exit(0);
				}

				// nonzero data switch
				
				else if( arg.charAt(1) == 'N' ) {
					
					is_sparse = true;
					inNZ = true; 	// we are using nonzero data
					
					if( arg.length() == 2 ) {
						
						inNZ = true;
					}
					else {
						
						nonzero_data_filename = arg.substring(2);
					}
				}
				else if( arg.charAt(1) == 'D' ) {
					
					inDM = true; 	// we are using nonzero data
					is_sparse = false;

					if( arg.length() == 2 ) {
						
						inDM = true;
					}
					else {
						
						df_data_filename = arg.substring(2);
					}
				}
				else if( arg.charAt(1) == 'I' ) {
					
					inIND = true; 	// we are using nonzero data
					
					if( arg.length() == 2 ) {
						
						inIND = true;
					}
					else {
						
						initial_cutoff_value = Float.parseFloat(arg.substring(2));
					}
				}
				else if( arg.charAt(1) == 'P' ) {
					
					inPF = true; 	// we are using nonzero data
					
					if( arg.length() == 2 ) {
						
						inPF = true;
					}
					else {
						
						point_label_filename = arg.substring(2);
					}
				}
				else if( arg.charAt(1) == 'Z' ) {
					
					inNZF = true; 	// we are using nonzero data
					
					if( arg.length() == 2 ) {
						
						inNZF = true;
					}
					else {
						
						nz_feature_label_filename = arg.substring(2);
					}
				}
				else if( arg.charAt(1) == 'T' ) {
					
					inTAG = true;
					if( arg.length() == 2 ) {
						
						inTAG = true;
					}
					else {
						
						tag_filename = arg.substring(2);
					}
				}
				else if( arg.charAt(1) == 'S' ) {
					
					inSEC = true;
					if( arg.length() == 2 ) {
						
						inSEC = true;
					}
					else {
						
						startup_seconds = Long.parseLong(arg.substring(2));
					}
				}

				else if( arg.charAt(1) == 'U' ) {
					
					inURL = true; 	// we are using html lookups
					is_url_available = true;
					
					if( arg.length() == 2 ) {
						
						inURL = true;
					}
					else {

						System.err.println("Error parsing URL command line.");
						System.exit(0);
					}
				}
				
				// error switch
				
				else {
					
					System.out.println("PARSE ERROR: Trouble parsing argument \"" + arg + "\"");
					System.exit(0);
				}
			}
			
			else if( inNZ ) {
				
				is_sparse = true;
				nonzero_data_filename = arg;
				inNZ = false;
			}			
			
			else if( inDM ) {
				
				is_sparse = false;
				df_data_filename = arg;
				inDM = false;
			}			
			
			else if( inIND ) {

				initial_cutoff_value = Float.parseFloat(arg);
				
				inIND = false;
			}			
			
			else if( inPF ) {
				
				point_label_filename = arg;
				inPF = false;
			}			
			
			else if( inNZF ) {
				
				nz_feature_label_filename = arg;
				inNZF = false;
			}			
			
			else if( inTAG ) {
				
				tag_filename = arg;
				inTAG = false;
			}			
			
			else if( inSEC ) {
				
				startup_seconds = Long.parseLong(arg);
				inSEC = false;
			}
						
			else if( inURL ) {
				
				url_list_filename = arg;
				inURL = false;
			}	
		}
		
		// check if we never got an expected argument
		
		if( inNZ || inDM || inIND || inPF || inNZF || inHTML || inTAG || inSEC || inURL) {
			
			System.err.println("Missing argument(s) after option.");
			System.exit(0);
		}
	}
	
	public Snappy( String[] args ) {
		
		super("MoDiscoTag");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		System.out.print("Parsing Command Line Args...");
		this.parseArgs(args);
		System.out.println("done.");
		
		// load data based on the arguments

		if( is_sparse ) {
			
			System.out.print("Loading Sparse Data...");
			try {
				
				nz_data = SparseReader.readNZData( new FileReader(nonzero_data_filename) );
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
			
			graph_manager = new GraphManager( nz_data ); 
			System.out.println("done.");
		}
		else {
			
			System.out.print("Loading Distance Matrix Data from " + df_data_filename +"...");
			try {
				
				int ptCount = DistanceReader.readDistancePointSize(new FileReader( df_data_filename ));
				distance_function = DistanceReader.readSortedDistanceData(new FileReader( df_data_filename ),ptCount);
//				distance_function = DistanceReader.readSortedSubDistanceData(new FileReader( df_data_filename ),ptCount);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			graph_manager = new GraphManager( distance_function );
			System.out.println("done.");
		}
		
		// load label data
		
		if( point_label_filename != "" ) {
			
			System.out.print("Loading Point Features...");
			try {
				point_feature_list = FeatureList.readFeatureList(new FileReader(point_label_filename));
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
			System.out.println("done.");
		}
		if( nz_feature_label_filename != "" ) {
			System.out.print("Loading Nonzero Features...");
			try {
				edge_feature_list = FeatureList.readFeatureList(new FileReader(nz_feature_label_filename));
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
			System.out.println("done.");
		}
				
		// let the graph manager run for a moment
		
		System.out.print("Loading Initial Edges...");
		long start_time = System.currentTimeMillis();
		while( System.currentTimeMillis() - start_time < startup_seconds && !graph_manager.updateGraph() );
		
//		graph_manager.outputSimpleEdgesToFile("edge_data_file.txt");
//		System.exit(0);
		
		graph_manager.sortEdges();
		System.out.println("done.");
		glimmer_layout = new GlimmerLayout( graph_manager );
		
		// compute the topo tree
		
		System.out.print("Computing TopoTree...");
		float[] mylevels = new float[DISTANCE_BINS];
		for( int i = DISTANCE_BINS; i > 0 ; i-- ) {
			
			mylevels[DISTANCE_BINS-i] = ((float)i) / ((float)DISTANCE_BINS);
		}
		topo_tree = new TopoTree(graph_manager, mylevels);
		System.out.println("done.");
		graph_manager.setCutoff(1.f);
		tag_table = new TagTable(topo_tree);
		
		// load up the tag file here

		if( tag_filename.length() > 0 ) {
			
			tag_table.loadTagFile(tag_filename);
		}
		
		// perform component count and component layout
		
//		System.out.print("Performing component count...");
//		int component_count = graph_manager.countComponents();
//		System.out.println("Number of components = " + component_count);
//		System.out.println("done.");
//		
//		System.out.print("Performing component layout...");
//		graph_layouts = new ArrayList<GraphLayout>();
//		for( TopoTree.TopoTreeNode node : topo_tree.level_lookup.get(0)) {
//			
//			if(node.num_points > (int)Math.pow(2, 3) )
//				graph_layouts.add(new GraphLayout(node,LayoutType.SUMMARY));
//		}
//		for( int component = 0; component < component_count; component++ ) {
//			
//			System.out.println("Component Size = " + graph_manager.getSubComponents().get(component).size() );
//			if( graph_manager.getSubComponents().get(component).size() > default_component_bin )
//				graph_layouts.add(new GraphLayout(graph_manager,component,LayoutType.SUMMARY));
//		}
//		System.out.println("done.");
		
		// build the initial histograms
		
		System.out.print("Building Histograms...");
//		int[] trashhisto_comp = graph_manager.getComponentHisto();
//		for( int i = 0; i < trashhisto_comp.length; i++ ) {
//			System.out.println(""+i+":" + trashhisto_comp[i] );
//		}
//		component_slider = new HistSlider(graph_manager.getComponentHisto(),default_component_bin);
//		distance_slider  = new HistSlider(graph_manager.getHisto(Snappy.DISTANCE_BINS),0.f,1.f,DISTANCE_BINS-1);
//		distance_slider.isLog = true;
//		distance_slider.useAbsolute = true;
//		component_slider.useAbsolute = true;
//		component_slider.isLog = true;
		
		tt_control = new TopoTreeControl(topo_tree,graph_manager.getHisto(Snappy.DISTANCE_BINS));
		tt_control.setTagTable(tag_table);
		tag_control = new TagControl(tag_table );

//		int[] trashhisto = graph_manager.getHisto(Snappy.DISTANCE_BINS);
		current_histo = new float[Snappy.DISTANCE_BINS];
		for(int i = 0; i < Snappy.DISTANCE_BINS; i++ ) {
//			System.out.println(""+i+":"+trashhisto[i]);
			current_histo[i] = (i+1)*(1.f / (float)Snappy.DISTANCE_BINS);
		}
		System.out.println("done.");
		
		// connect the graph and the sliders
		
		graph_manager.addChangeListener(this);
		tt_control.addChangeListener(this);
		
		// build the graph drawing component
		
		System.out.print("Initializing graph drawer...");
		if( point_feature_list != null ) {
			node_labeller = new SimpleNodeLabeller(point_feature_list);
		}
		if( edge_feature_list!= null ) {
			node_labeller = new EdgeIntersectionLabeller(graph_manager, edge_feature_list, nz_data);
		}
		doc_list_control = new DocList( node_labeller, tag_table );
		glimmer_drawer = new GlimmerDrawer(glimmer_layout, topo_tree, tag_table);

		
		tt_control.addTagChangeListener(doc_list_control);
		tt_control.addTagChangeListener(tag_control);
		tt_control.addTagChangeListener(glimmer_drawer);
		
		doc_list_control.addChangeListener(tt_control);

		doc_list_control.addTagChangeListener(glimmer_drawer);
		doc_list_control.addTagChangeListener(tt_control);
		doc_list_control.addTagChangeListener(tag_control);

		glimmer_drawer.addTagChangeListener(doc_list_control);
		glimmer_drawer.addTagChangeListener(tt_control);
		glimmer_drawer.addTagChangeListener(tag_control);
		
		tt_control.node_labeller = node_labeller;
		glimmer_drawer.node_labeller = node_labeller;
		
		tag_control.m_node_tree = doc_list_control;
		
		// If we have a list of HTML files, create a panel to view them in
		if( is_url_available ) {
		
			JWebBrowser browser = new JWebBrowser();
			browser.setBarsVisible(false);
			browser.setStatusBarVisible(false);
			browser.navigate("http://www.google.com");
			html_panel_holder = browser;
	
			html_dispatch = new HtmlDispatch(	doc_list_control.item_jlist, 
												HtmlDispatch.loadURLList(url_list_filename),
												browser);			
			doc_list_control.addKeyListener(html_dispatch);
		}
		

//		graph_drawer = new GraphDrawer( node_labeller );
		System.out.println("done.");
		
		// lay out the components
		
//		JPanel slider_panels = new JPanel();
//		//slider_panels.setLayout(new GridLayout(2,1) );
//		slider_panels.setLayout(new GridLayout(1,1) );
////		slider_panels.add(distance_slider);
//		//slider_panels.add(component_slider);
//		slider_panels.add(tt_control);
		
		SnappyPanel snappyPanel = new SnappyPanel();
		snappyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		tree_panel = new JPanel() {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void doLayout() {
				
				int width = getWidth();
				int height = getHeight();
				Insets insets = getInsets();
				int myWidth = (width - insets.left) - insets.right;
				int myHeight = (height - insets.top) - insets.bottom;
				
				int title_height = tt_panel_title.getPreferredSize().height;

				tt_panel_title.setBounds(insets.left, insets.top, myWidth, title_height);
				tt_control.setBounds(insets.left, insets.top + title_height + 5, myWidth, myHeight - title_height - 5 );
			}
			
			public void paintComponent( Graphics g ) {
				
				tt_control.redraw();
			}
		};
		tt_panel_title = new JLabel("Disconnected Component Tree");
		tt_panel_title.setForeground(PrettyColors.DarkGrey);
		tree_panel.setBackground(Color.white);
		tree_panel.add(tt_panel_title);
		tree_panel.add(tt_control);
		tree_panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, PrettyColors.Grey),BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		
		snappyPanel.add( tree_panel );
		snappyPanel.add( glimmer_drawer );
//		snappyPanel.setPreferredSize(new Dimension(1024,700));
		
		//JScrollPane scroll_pane = new JScrollPane(graph_drawer);
//		ScrollPane scroll_pane2 = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
//		scroll_pane2.setPreferredSize(new Dimension(500,100));
//		scroll_pane2.add(graph_drawer);
//		scroll_pane2.getVAdjustable().addAdjustmentListener(new AdjustmentListener() {
//
//			@Override
//			public void adjustmentValueChanged(AdjustmentEvent arg0) {
//				graph_drawer.redraw();
//				
//			}} );
//		this.getContentPane().add( scroll_pane2, "Center" );

		snappyPanel.add(doc_list_control);
		snappyPanel.add(tag_control);
		if( is_url_available ) {
//			snappyPanel.add(webBrowser);
			snappyPanel.add(html_panel_holder);
		}
		this.getContentPane().add(snappyPanel);
		
		// init the processing apps
		
//		distance_slider.init();
//		component_slider.init();
//		graph_drawer.init();
		tt_control.init();

		// set the proper sizes for the graph drawer
		
		//graph_drawer.setRectangles(graph_layouts);

		this.pack();
		this.setVisible(true);
	}
	
	public static void main( final String[] args ) {
		
		NativeInterface.open();  
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
        	Snappy snappy = null;
            public void run() {
            	
            	snappy = new Snappy(args);
//            	snappy.setSize(new Dimension(1024,700));
            }
        } );
        NativeInterface.runEventPump(); 
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		
	}
}
