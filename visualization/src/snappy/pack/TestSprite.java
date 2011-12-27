package snappy.pack;

import java.util.ArrayList;
import java.util.List;

public class TestSprite implements ISprite {

	ArrayList<IMappedImageInfo> myList = null;
	int w;
	int h;

	public TestSprite(  ) {
		this(0,0);
	}
	
	public TestSprite( int w, int h ) {
		
		myList = new ArrayList<IMappedImageInfo>();
		this.w = w;
		this.h = h;
	}
	
	@Override
	public void AddMappedImage(IMappedImageInfo mappedImage) {

		myList.add( mappedImage );
		
		this.h = Math.max(this.h,mappedImage.getY() + mappedImage.getImageInfo().getHeight() );
		this.w = Math.max(this.w,mappedImage.getX() + mappedImage.getImageInfo().getWidth() );
	}

	@Override
	public int getArea() {
		return w*h;
	}

	@Override
	public int getHeight() {

		return h;
	}

	@Override
	public List<IMappedImageInfo> getMappedImages() {

		return myList;
	}

	@Override
	public int getWidth() {

		return w;
	}
}
