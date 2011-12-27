package snappy.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import processing.core.PApplet;
import processing.core.PFont;
import snappy.pack.*;
import snappy.graph.*;
import snappy.graph.GraphLayout.LayoutType;

public class GraphDrawer extends PApplet implements  ComponentListener {

	public class LabelWidthMaker implements IImageInfo {

	    public GraphLayout gl 		= null;
		PApplet pa 			= null;
		float width_cache 	= -1.f;
		NodeLabeller nl		= null;
		
		public LabelWidthMaker( GraphLayout graphLayout, 
								PApplet pa,
								NodeLabeller nl ) {
			
			this.gl = graphLayout;
			this.pa = pa;
			this.nl = nl;
		}
		
		@Override
		public int getWidth() {
			
			if( width_cache == -1.f ) {
				
				pa.textFont(( gl.getLayouttype() == LayoutType.SUMMARY )?summary_font:term_font);
				
				if( gl.getLayouttype() == LayoutType.SUMMARY ) {
					
					pa.textFont(summary_font);
					
					String summaryLabel = " " + gl.getNumPoints() + ": ";
					for( SizedLabel sl : node_labeller.getSummaryLabel(gl.getNode()) ) {
						
						summaryLabel += sl.label + " ";
					}
					width_cache = pa.textWidth(" " + summaryLabel + "  ");
				}
				else {
					for( int pt = 0; pt < gl.getNumPoints(); pt++ ) {
						
						width_cache = Math.max(width_cache,pa.textWidth(" " + nl.getLabel(gl.getPoint(pt).idx) + "  "));
					}
				}
			}
			return gl.getWidth() + (int)width_cache;
		}

		@Override
		public int getHeight() {
			
			pa.textFont(( gl.getLayouttype() == LayoutType.SUMMARY )?summary_font:term_font);
			return gl.getHeight() +  (int)Math.ceil(1.5*(pa.textAscent() + pa.textDescent()));
		}
		
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -6615032497901900883L;

	NodeLabeller node_labeller = null;
	IMapper<LayoutHolder> square_packer = null;
	ISprite square_holder = null;
	ArrayList<IImageInfo> rectangles = null;
	
	Color bkgndColor 	= Color.WHITE;
	Color boundsColor 	= Color.BLACK;
	
	boolean draw_edges = false;
	boolean draw_labels = true;
	
	PFont term_font = null;
	PFont summary_font = null;
	
	public GraphDrawer(NodeLabeller nl) {
		super();
		
		node_labeller = nl;
		square_holder = new LayoutHolder();
		LayoutHolder foo = new LayoutHolder();
		//square_packer = new MapperOptimalEfficiency<LayoutHolder>(new SnappyCanvas(), foo);
		square_packer = new MapperVerticalSorted<LayoutHolder>(foo);
	}
	
	public void mouseClicked() {
		if( square_holder != null ) {
			for( IMappedImageInfo mappedImage : square_holder.getMappedImages() ) {
			
				if( mouseX > mappedImage.getX() && mouseX-mappedImage.getX() < mappedImage.getImageInfo().getWidth() ) {
					if( mouseY > mappedImage.getY() && mouseY-mappedImage.getY() < mappedImage.getImageInfo().getHeight() ) {
						
						GraphLayout gl = null;
						if( mappedImage.getImageInfo() instanceof GraphLayout )
							gl = ((GraphLayout)mappedImage.getImageInfo());
						else
							gl = ((GraphDrawer.LabelWidthMaker)mappedImage.getImageInfo()).gl;
						
						gl.setLayoutType((gl.getLayouttype()==LayoutType.SUMMARY)?LayoutType.VERTICAL_LAYOUT:LayoutType.SUMMARY);
						packup();
					}
				}
			}			
		}
	}
	
	/*
	 * Call this
	 */
	public void packup( ) {

		//System.out.println("BEGIN PACKUP");
		square_holder = square_packer.Mapping(rectangles);
		if( square_holder == null ) {
			System.out.println("NULL SQUARE HOLDER");
		}
		else {
			size(Math.max(square_holder.getWidth(),100), Math.max(100, square_holder.getHeight()));
			SwingUtilities.invokeLater(new Runnable() {
		        public void run() {
		        	invalidate();
		          // Set the preferred size so that the layout managers can handle it
		          getParent().validate();
					redraw();
		        }
		      });
		}
		//System.out.println("END PACKUP");
	}

	public void setRectangles( ArrayList<GraphLayout> subgraphs) {
		
		this.rectangles = new ArrayList<IImageInfo>();
		
		for( GraphLayout graph_layout : subgraphs ) {
			if( node_labeller != null )
				rectangles.add(new LabelWidthMaker(graph_layout,this,node_labeller));
			else
				rectangles.add(graph_layout);
		}
	}
	
	public void setup() {
		
		smooth();

		term_font = createFont("SansSerif",8); 
		summary_font = createFont("SansSerif",12); 
		
		textFont( term_font );
		noLoop();	
		
		packup();
	}
	
	public void draw() {
	
		// clear screen		
		
		background( bkgndColor.getRed(), bkgndColor.getGreen(), bkgndColor.getBlue() );
		
		if( square_holder == null ) {
			return;
		}
		
		for( IMappedImageInfo mappedImage : square_holder.getMappedImages() ) {
			
			// draw the bounding rect
			
			fill( bkgndColor.getRed(), bkgndColor.getGreen(), bkgndColor.getBlue() );
			if( mappedImage.getImageInfo() instanceof LabelWidthMaker ) {
				LabelWidthMaker lwm = (LabelWidthMaker) mappedImage.getImageInfo();
				if(lwm.gl.getNode().hilighted)
				{
					strokeWeight(4.f);
					stroke( 223,	61,	51 );
				}
				else {
					strokeWeight(1.f);
					stroke( boundsColor.getRed(), boundsColor.getGreen(), boundsColor.getBlue() );
				}
			}
			else 
				stroke( boundsColor.getRed(), boundsColor.getGreen(), boundsColor.getBlue() );
			rect(	mappedImage.getX(),
					mappedImage.getY(),
					mappedImage.getImageInfo().getWidth(),
					mappedImage.getImageInfo().getHeight() );

			strokeWeight(1.f);			
			fill( boundsColor.getRed(), boundsColor.getGreen(), boundsColor.getBlue() );
			
			GraphLayout gl = null;
			if( mappedImage.getImageInfo() instanceof GraphLayout )
				gl = ((GraphLayout)mappedImage.getImageInfo());
			else
				gl = ((GraphDrawer.LabelWidthMaker)mappedImage.getImageInfo()).gl;

			if( gl.getLayouttype() == GraphLayout.LayoutType.SUMMARY ) {
				
				// draw the summary
				textFont(summary_font);
				
				float space_width = textWidth(" ");
				
				String summaryLabel = " " + gl.getNumPoints() + ": ";
				for( SizedLabel sl : node_labeller.getSummaryLabel(gl.getNode()) ) {
					
					summaryLabel += sl.label + " ";
				}
				text(	summaryLabel,
						mappedImage.getX() + space_width,
						mappedImage.getY() + ((int)Math.ceil(textAscent()) + (int)Math.ceil(textDescent())));
			}
			else {
				
				// draw the summary
				textFont(summary_font);
				
				float space_width = textWidth(" ");
				
				String summaryLabel = " " + gl.getNumPoints() + ": ";
				for( SizedLabel sl : node_labeller.getSummaryLabel(gl.getNode()) ) {
					
					summaryLabel += sl.label + " ";
				}
				text(	summaryLabel,
						mappedImage.getX() + space_width,
						mappedImage.getY() + ((int)Math.ceil(textAscent()) + (int)Math.ceil(textDescent())));
				
				int verticalOffset = ((int)Math.ceil(textAscent()) + (int)Math.ceil(textDescent()));
				
				textFont(term_font);
				
				verticalOffset += (int)Math.ceil(textAscent()) + (int)Math.ceil(textDescent());
				
				// draw the points
				
				for( int pt = 0; pt < gl.getNumPoints(); pt++ ) {
					
					point(mappedImage.getX() + gl.getPoint(pt).x,
							mappedImage.getY() + verticalOffset + gl.getPoint(pt).y + ((int)Math.ceil(textAscent()) + (int)Math.ceil(textDescent())));
	
					// draw the labels
					if( draw_labels && node_labeller != null ) {
						text(node_labeller.getLabel(gl.getPoint(pt).idx),
								mappedImage.getX() + gl.getPoint(pt).x + space_width,
								mappedImage.getY() +  verticalOffset + gl.getPoint(pt).y + ((int)Math.ceil(textAscent()) + (int)Math.ceil(textDescent())));
					}
				}
			}
		}
	}
	
	public Dimension getPreferredSize( ) {
		
		return new Dimension(Math.max(square_holder.getWidth(),100), Math.max(100, square_holder.getHeight()));
	}

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
	          // Set the preferred size so that the layout managers can handle it
				redraw();
	        }
	      });
		
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
