package snappy.ui;

import java.awt.Color;

/*
 * Static class for handling color picking with tags
 */
public class PrettyColors {

	public static Color Blue = new Color(37,139,193);
	public static Color Orange = new Color(254,151,41);
	public static Color Green = new Color(50,171,90);
	public static Color Red = new Color(223,61,51);
	public static Color Purple = new Color(166,127,190);
	public static Color Brown = new Color(157,106,94);
	public static Color Pink = new Color(232,151,199);
	public static Color Grey = new Color(145,144,144);
	public static Color Gold = new Color(199,197,43);
	public static Color Teal = new Color(5,199,215);
	public static Color DarkGrey = new Color(64,64,64);
	public static Color[] PrettyColor = {Blue,Orange,Green,Purple,Brown,Pink,Gold,Teal};
	
	public static Color colorFromInt( int color_index ) {
		
		return PrettyColor[color_index % PrettyColor.length];
	}
	
	public static Color getNextColor( Color prevColor ) {
		
		if( prevColor == Blue ) {
			
			return Orange;
		}
		if( prevColor == Orange ) {
			
			return Green;
		}
		if( prevColor == Green ) {
			
			return Purple;
		}
		if( prevColor == Purple ) {
			
			return Brown;
		}
		if( prevColor == Brown ) {
			
			return Pink;
		}
		if( prevColor == Pink ) {
			
			return Gold;
		}
		if( prevColor == Gold ) {
			
			return Teal;
		}
		
		return Blue;
	}
}
