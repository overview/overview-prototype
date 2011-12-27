package snappy.pack;

public class TestImageInfo implements IImageInfo {


	int w = 0;
	int h = 0;
	int border = 5;
	public int id=0;
	
	public TestImageInfo( int w, int h ) {
		
		this.w = w;
		this.h = h;
	}
	
	@Override
	public int getHeight() {
		// TODO Auto-generated method stub
		return h + 2*border;
	}

	@Override
	public int getWidth() {
		// TODO Auto-generated method stub
		return w + 2*border;
	} 
}
