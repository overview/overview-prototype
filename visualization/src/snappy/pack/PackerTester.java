package snappy.pack;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import snappy.ui.HistSlider;

public class PackerTester extends JPanel {

	private static final long serialVersionUID = -7210254652944008469L;
	ISprite mySprite = null;
	
	public PackerTester( ISprite sprite ) {
		super();
	
		mySprite = sprite;
		this.setPreferredSize(new Dimension(sprite.getWidth(), sprite.getHeight()));
	}
	
	public void paintComponent( Graphics g ) {
		
		super.paintComponent(g);
		
		Graphics2D g2D = (Graphics2D) g;
		
		// clear screen
		
		g2D.setColor( Color.WHITE );
		g2D.drawRect(0, 0, this.getWidth(), this.getHeight());
		g2D.fillRect(0, 0, this.getWidth(), this.getHeight());

		int k = 0;
		for( IMappedImageInfo mappedImage : mySprite.getMappedImages() ) {

			g2D.setColor( Color.BLACK );			
			g2D.fillRect( mappedImage.getX(), mappedImage.getY(), mappedImage.getImageInfo().getWidth(), mappedImage.getImageInfo().getHeight());
			g2D.setColor( Color.WHITE );
			g2D.setStroke( new BasicStroke(5) );
			g2D.drawRect( mappedImage.getX(), mappedImage.getY(), mappedImage.getImageInfo().getWidth(), mappedImage.getImageInfo().getHeight());
			g2D.setStroke( new BasicStroke(1) );
			g2D.drawString(""+((TestImageInfo)mappedImage.getImageInfo()).id, mappedImage.getX() + mappedImage.getImageInfo().getWidth()/3, mappedImage.getY()+mappedImage.getImageInfo().getHeight()/2);
			k++;
		}
	}
	
	public Dimension getPreferredSize( ) {
		
		return new Dimension(mySprite.getWidth(), mySprite.getHeight());
	}
	
	public static void main( String[] args ) {

		// create a jframe
		
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	
        		SnappyCanvas sp_canvas = new SnappyCanvas();
        		TestSprite foo = new TestSprite();
        		IMapper<TestSprite> mapper = new MapperOptimalEfficiency<TestSprite>(sp_canvas,foo);
//        		IMapper<TestSprite> mapper = new MapperVerticalOnly<TestSprite>(foo);
        		ArrayList<IImageInfo> rectangles = new ArrayList<IImageInfo>();
        		
//        		rectangles.add(new TestImageInfo( (int)20, (int)30 ) );
//        		rectangles.add(new TestImageInfo( (int)30, (int)40 ) );
//        		rectangles.add(new TestImageInfo( (int)14, (int)8 ) );
//        		rectangles.add(new TestImageInfo( (int)14, (int)8 ) );
//        		rectangles.add(new TestImageInfo( (int)14, (int)8 ) );
//        		rectangles.add(new TestImageInfo( (int)14, (int)8 ) );

        		for( int i = 0; i < 101; i++ ) {
        			TestImageInfo tii = new TestImageInfo( (int)Math.ceil(Math.random()*100), 
            				(int)Math.ceil(Math.random()*100) );
        			tii.id = i;
            		rectangles.add(tii);
        		}
        		
        		JFrame foo_frame = new JFrame("test packertesting");
                foo_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                PackerTester packerTester = new PackerTester( mapper.Mapping(rectangles) );
        		foo_frame.getContentPane().add(packerTester);		
        		foo_frame.pack();
        		foo_frame.setVisible(true);
            }
        } );
	}
}
