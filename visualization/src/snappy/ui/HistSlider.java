package snappy.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.PApplet;

public class HistSlider extends PApplet implements ComponentListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8103299465750391907L;

	public int[] bins = null;
	
	public int h_border = 5;
	public int v_border = 25;
	public int tick_size = 2;
	public int cutoff = 0;
	public int vertical_ticks = 4;
	public int horizontal_ticks = 10;
	public int cur_mouse_x = -1;
	public int cur_mouse_y = -1;
	
	public int fraction_digits = 3;
	
	public float absolute_minimum = 0.f;
	public boolean useAbsolute  = false;

	public boolean isLog		= false;
	public boolean isSorted		= true;
	public boolean useBars 		= true;
	public boolean labelAxes 	= true;
	public boolean isCutoffLeft = false;
	public boolean useCutoff    = true;
	public boolean useLine 		= false;
	public boolean useDimensionNames = true;
	public int max_str_len 		= -1;

	Color bkgndColor 	= Color.WHITE;
	Color axesColor 	= Color.BLACK;
	Color lineColor 	= new Color(0xDF,0x3D,0x33);
	Color barColor		= new Color(0x25,0x8B,0xC1 );
	Color unselbarColor	= Color.LIGHT_GRAY;
	Color selbarColor	= new Color(0xC7,0xC5,0x2B);
	Color borderColor	= Color.BLACK;

	ArrayList<ChangeListener> changeListeners = null;

	float start_tick_value = Float.NaN;
	float stop_tick_value  = Float.NaN;
	
	public Dimension getPreferredSize( ) {
		
		return new Dimension(500,300);
	}
	

	public HistSlider( int[] bins, float start_tick, float stop_tick, int start_bin ) {
		
		super();
		
		start_tick_value = start_tick;
		stop_tick_value = stop_tick;
		
		cutoff = start_bin;
		
		changeListeners = new ArrayList<ChangeListener>();
		this.bins = bins;
	}
	
	public HistSlider( int[] bins, int start_bin ) {
		
		this(bins,Float.NaN,Float.NaN, start_bin);
	}
	
	public void setBins( int[] bins ) {

		this.bins = bins;
		redraw();
	}
	
	public void setup() {

		// added by HY

		smooth();

		textFont( createFont("SansSerif",10) );
		noLoop();		
		this.setPreferredSize(new Dimension(500,500));
	}
	
	public void addChangeListener( ChangeListener cl ) {
		
		this.changeListeners.add( cl );
	}
	
	public void draw() {

		cutoff = Math.min(cutoff, bins.length-1);
		
		// clear screen
		
		this.background( bkgndColor.getRed(), bkgndColor.getGreen(), bkgndColor.getBlue() );
				
		// compute scaled lines and data points
		
		float max_uni = Float.MIN_VALUE;
		float min_uni = Float.MAX_VALUE;
		
		for( int b : bins ) {

			if( isLog ) {
				if( b > 0) {
					max_uni = Math.max( max_uni, (float)Math.log10((float)b) );
					min_uni = Math.min( min_uni, (float)Math.log10((float)b) );
				}
			}
			else {
				max_uni = Math.max( max_uni, b );
				min_uni = Math.min( min_uni, b );
			}
//			max_uni = Math.max( max_uni, b );
//			min_uni = Math.min( min_uni, b );
		}
		
		if( useAbsolute ) {
			
			if( isLog ) {
				
				min_uni = (float) ((Math.abs(absolute_minimum)<1e-8)?-1.f:Math.log10(absolute_minimum));
			}
			else {
				min_uni = absolute_minimum;
			}
		}
		
		// calculate the labels and their sizes
		
		this.max_str_len = -1;
		horizontal_ticks = Math.min( 10, bins.length );
		String[] labelStrs = new String[vertical_ticks];
		String[] h_labelStrs = new String[horizontal_ticks];
		int[] h_labelInts = new int[horizontal_ticks];
		NumberFormat nf = new DecimalFormat("0.##E0");//NumberFormat.getInstance();
		NumberFormat nf_i = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(3);
		
		double scale = (max_uni-min_uni) / ((double) vertical_ticks-1.);
		double scale_h = Math.ceil( (bins.length) / ((double) horizontal_ticks) );
		int p = 0;
		for( int i = 0; i < bins.length; i += scale_h ) {
			
			if( Float.isNaN(start_tick_value) ) {
				
				h_labelStrs[p] = nf_i.format( (int)Math.floor( i+1 ) );
			}
			else {
				
				h_labelStrs[p] = nf.format( start_tick_value + i*(stop_tick_value-start_tick_value)/(bins.length-1) );
			}
			
			h_labelInts[p] = (int)Math.floor( i );
			p++;
		}
		for( int i = 0; i < vertical_ticks; i++ ) {
			
			// compute and store the string
//			if( isLog ) {
//
//				labelStrs[i] = nf.format( Math.pow(10, scale*i + min_uni) );
//			}
//			else {
//				
				labelStrs[i] = nf.format( scale*i + min_uni);
//			}
			
			// measure its size
			max_str_len = Math.max(max_str_len, (int)this.textWidth(labelStrs[i]) );			
		}

		if( ! labelAxes ) {
			
			max_str_len = 0;
		}
		
		int canvasHeight = this.getHeight() - 2*v_border;
		int canvasWidth = (this.getWidth() -  2*h_border) - max_str_len;
		double yscale =  ( (double) canvasHeight ) / (max_uni - min_uni);
		double barwidth = ((double)canvasWidth) / ((double)bins.length);
		double xscale =  ( (double) canvasWidth-barwidth ) / ((double) bins.length-1.0);
		
		ArrayList<Rectangle2D> dataRects = new ArrayList<Rectangle2D>();
		
		if( useBars ) {
			
			for (int index = 0; index < bins.length; index++) {
				
				if( !isLog ) {
					
					if( bins[index] > 0 ) {
						dataRects.add( new Rectangle2D.Double( 	(xscale*(index) + h_border + max_str_len), 
																(canvasHeight - yscale* (bins[index]-min_uni)) + v_border, 
																barwidth, 
																yscale* (bins[index]-min_uni) ) );
					}
					else {
						dataRects.add( new Rectangle2D.Double( 	(xscale*(index) + h_border + max_str_len), 
								canvasHeight + v_border, 
								barwidth, 
								0 ) );
					}
				}
				else {
					
					dataRects.add( new Rectangle2D.Double( 	(xscale*(index) + h_border + max_str_len), 
							(canvasHeight - yscale* (Math.log10(bins[index])-min_uni)) + v_border, 
							barwidth, 
							yscale* (Math.log10(bins[index])-min_uni) ) );					
				}
			}
		}
		
		Line2D xAxis = new Line2D.Double( 	h_border + max_str_len, 
											this.getHeight()-v_border, 
											this.getWidth() - h_border, 
											this.getHeight()-v_border );
		Line2D yAxis = new Line2D.Double( 	h_border + max_str_len, 
											v_border, 
											h_border + max_str_len, 
											this.getHeight()-v_border);
		ArrayList<Line2D> xTicks = new ArrayList<Line2D>();
		for( int q : h_labelInts ) {
			
			xTicks.add( new Line2D.Double(	(xscale*(q) + h_border + max_str_len)+barwidth/2., (this.getHeight()-v_border) ,
											(xscale*(q) + h_border + max_str_len)+barwidth/2.,(this.getHeight()-v_border)+h_border) );
		}
		ArrayList<Line2D> yTicks = new ArrayList<Line2D>();
		double tick_span = (double)canvasHeight / (double)(vertical_ticks-1.);
		for( int i = 0; i < vertical_ticks; i++ ) {
		
			yTicks.add( new Line2D.Double(	max_str_len, (int)Math.round( (v_border + canvasHeight ) - (i * tick_span) ),
											max_str_len+h_border,(int)Math.round( (v_border + canvasHeight ) - (i * tick_span) )) );
		}
		
		// draw the axes

		stroke( axesColor.getRed(),axesColor.getGreen(),axesColor.getBlue() );
		line( (int)xAxis.getX1(), (int)xAxis.getY1(), (int)xAxis.getX2(), (int)xAxis.getY2() );
		line( (int)yAxis.getX1(), (int)yAxis.getY1(), (int)yAxis.getX2(), (int)yAxis.getY2() );
		for( Line2D tick : xTicks ) {
			
			line( (int)tick.getX1(), (int)tick.getY1(), (int)tick.getX2(), (int)tick.getY2() );
		}
		for(Line2D tick: yTicks ) {
			
			line( (int)tick.getX1(), (int)tick.getY1(), (int)tick.getX2(), (int)tick.getY2() );
		}
		
		// label the axes
		
		if( labelAxes ) {
			
			fill( axesColor.getRed(),axesColor.getGreen(),axesColor.getBlue() );
			for( int i = 0; i < vertical_ticks; i++ ) {

				text( labelStrs[i], max_str_len - textWidth(labelStrs[i]) , 
						(int)Math.round( (v_border + canvasHeight ) - (i * tick_span) + (textAscent()+textDescent())/3. ) ); 
			}
			int t = 0;
			for( int q : h_labelInts ) {
				
				if( h_labelStrs[t]!=null) {
					
					text(	h_labelStrs[t], 
							(int)Math.round(((xscale*(q) + h_border + max_str_len)+barwidth/2.) - textWidth(h_labelStrs[t])/2.) , 
							(this.getHeight()-v_border)+(textAscent()+textDescent()) );				
				}
				t++;
			}
		}
		
		// draw the data

		if( useBars ) {
		
			if( barwidth > 4 ) {
				stroke(axesColor.getRed(), axesColor.getGreen(), axesColor.getBlue() );			
			}
			else {
				noStroke();
			}
			
			if( isCutoffLeft ) {
				
				fill( barColor.getRed(), barColor.getGreen(), barColor.getBlue() );
				int k = 0;
				for( Rectangle2D rect2D : dataRects ) {
							
					if( k == cutoff ) {
						
						fill( selbarColor.getRed(), selbarColor.getGreen(), selbarColor.getBlue() );
					}
					if( k > cutoff ) {
						
						fill( unselbarColor.getRed(), unselbarColor.getGreen(), unselbarColor.getBlue() );
					}
					rect( (int) rect2D.getMinX(), (int) rect2D.getMinY(), (int) rect2D.getWidth(), (int) rect2D.getHeight() );
					k++;
				}
			}
			else {
				
				fill(unselbarColor.getRed(), unselbarColor.getGreen(), unselbarColor.getBlue() );
				int k = 0;
				for( Rectangle2D rect2D : dataRects ) {
					
					if( k == cutoff ) {
						
						fill(selbarColor.getRed(), selbarColor.getGreen(), selbarColor.getBlue() );
					}
					if( k > cutoff ) {
						
						fill(barColor.getRed(), barColor.getGreen(), barColor.getBlue() );
					}
					rect( (int) rect2D.getMinX(), (int) rect2D.getMinY(), (int) rect2D.getWidth(), (int) rect2D.getHeight() );
					k++;
				}
			}
		}
		
		double polyspot = (xscale*cutoff) + h_border + max_str_len + barwidth/2.0;

		// draw the selection lines
		if (bins.length > 0)
		{
			
			stroke(0xDF,0x3D,0x33 );			
			strokeWeight(2.f);
			
			// vertical line			
			double y_rise = (canvasHeight - yscale* (bins[cutoff]-min_uni)) + v_border;
			if( isLog )
				y_rise = (canvasHeight - yscale* (Math.log10(bins[cutoff])-min_uni)) + v_border;
			line( (int)polyspot,
											this.getHeight()-v_border,
											(int)polyspot,
											(int)y_rise );
			// horizontal line
			line(	(int)h_border + max_str_len,
					(int)y_rise,
					(int)polyspot,
					(int)y_rise);
			
//			// find the values of the labels
			double v_label_value = (isLog?Math.log10(bins[cutoff]):bins[cutoff]);
			int h_label_value = cutoff+1;

			// compute the label statistics
			String v_label_string = null;
//			if( isLog ) {
//				
//				v_label_string = nf.format( Math.pow(10, v_label_value) );
//			}
//			else {
//
				v_label_string = nf.format(v_label_value);
//			}
			String h_label_string = ""+h_label_value;
			if( Float.isNaN(start_tick_value) )
				h_label_string = ""+(h_label_value+1);
			else
				h_label_string = nf.format( start_tick_value + (h_label_value+1)*(stop_tick_value-start_tick_value)/(bins.length-1) );
			
			// label the lines
			
			fill( 0xDF,0x3D,0x33 );
			rect( 0,(int)y_rise-textAscent(),(int)(h_border + max_str_len),textAscent()+textDescent() );
			rect( (int)polyspot-2,(int)this.getHeight()-(v_border-1),textWidth( h_label_string )+2,textAscent()+textDescent());
			fill( 255,255,255);
			strokeWeight( 1.f );			
			text(	v_label_string, 0, (int)y_rise );
			text(	h_label_string, (int)polyspot, this.getHeight()-(v_border-1)+textAscent() );
		}
		
		
		// draw the crosshairs
		
		if( this.cur_mouse_x >= 0 ) {

			stroke(0x91,0x90,0x90, 128 ); 
			strokeWeight(2.f);
			
			// vertical line
			line( 	cur_mouse_x,
											this.getHeight()-v_border,
											cur_mouse_x,
											v_border) ;
			
			// horizontal line
			line(	h_border + max_str_len,
											cur_mouse_y,
											this.getWidth() - h_border,
											cur_mouse_y) ;
			
			// find the values of the labels
			double v_label_value = (max_uni-min_uni)*(1.0 - ((double)(cur_mouse_y - (v_border))) / ((double)(this.getHeight()-2*v_border))) + min_uni;
			int h_label_value = (int) Math.floor( bins.length*((double)(cur_mouse_x - (h_border + max_str_len))) / ((double)(this.getWidth() - (2*h_border + max_str_len))) );			
			String h_label_string = "";
			if( Float.isNaN(start_tick_value) )
				h_label_string = ""+(h_label_value+1);
			else
				h_label_string = nf.format( start_tick_value + (h_label_value+1)*(stop_tick_value-start_tick_value)/(bins.length-1) );
			
			cur_mouse_x = (int)(xscale*((int) Math.floor( bins.length*((double)(cur_mouse_x - (h_border + max_str_len))) / ((double)(this.getWidth() - (2*h_border + max_str_len))) ) ) + h_border + max_str_len + barwidth/2.0);
			// compute the label statistics
			String v_label_string = null;
//			if( isLog ) {
//				
//				v_label_string = nf.format( Math.pow(10, v_label_value) );
//			}
//			else {
//
				v_label_string = nf.format(v_label_value);
//			}
			
			// label the lines
			//
			fill( 0xFE,0x97,0x29 );
			rect( 0,cur_mouse_y-textAscent(),h_border + max_str_len,textAscent()+textDescent());
			rect( Math.min( cur_mouse_x, this.getWidth()-textWidth(h_label_string)-this.h_border),this.getHeight()-(v_border-1),textWidth( h_label_string),textAscent()+textDescent());
			fill( 0,0,0 ); 
			strokeWeight(1.f);
			text(	v_label_string, 0, cur_mouse_y );
			text(	h_label_string, Math.min( cur_mouse_x, this.getWidth()-textWidth(h_label_string)-this.h_border), this.getHeight()-(v_border-1)+textAscent() );
		}
		
	}
	
	public void mouseClicked() {

		
		// compute selection dimension based on mouse click
		// check the bounds
		if(	(mouseX > h_border + max_str_len && mouseX < this.getWidth() - h_border ) && 
			(mouseY > v_border && mouseY < this.getHeight() - v_border ) ) {
			
			//record the coords
			this.cur_mouse_x = mouseX;
			this.cur_mouse_y = mouseY;

			// choose new dimension
			this.cutoff = (int) Math.floor( bins.length*((double)(cur_mouse_x - (h_border + max_str_len))) / ((double)(this.getWidth() - (2*h_border + max_str_len))) );
			
			// tell everyone about it
			for( ChangeListener cl : changeListeners ) {
				
				cl.stateChanged( new ChangeEvent(this) );
			}
			
			// update the view
			redraw();
		}		
	}
	
	public void mouseMoved() {
		
		// check the bounds
		if(	(mouseX > h_border + max_str_len && mouseX < this.getWidth() - h_border ) && 
			(mouseY > v_border && mouseY < this.getHeight() - v_border ) ) {
			
			//record the coords
			
			this.cur_mouse_x = mouseX;
			this.cur_mouse_y = mouseY;
			redraw();
		}	
		else {
			
			this.cur_mouse_x=-1;
			this.cur_mouse_y=-1;
			redraw();
		}
	}
	
	public static void main( String[] args ) {
		
		// create a jframe
		
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	
            	int bincount = 50;
        		int[] bins = new int[bincount];
        		for( int k= 0;k<bincount;k++) {
        			bins[k]=(int)(Math.random()*100);
        		}
        		JFrame foo_frame = new JFrame("test");
                foo_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        		HistSlider hslider = new HistSlider(bins,0.f,1.f,1);
        		hslider.isLog = false;
        		hslider.setPreferredSize(new Dimension(500,500));
        		foo_frame.getContentPane().add(hslider);		
        		hslider.init();
        		foo_frame.getContentPane().addComponentListener(hslider);
        		foo_frame.pack();
        		foo_frame.setVisible(true);
            }
        } );
	}

	@Override
	public void componentHidden(ComponentEvent e) {

	}

	@Override
	public void componentMoved(ComponentEvent e) {

	}

	@Override
	public void componentResized(ComponentEvent e) {
		
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {

				redraw();
	        }
	      });
	}

	@Override
	public void componentShown(ComponentEvent e) {

	}

}
