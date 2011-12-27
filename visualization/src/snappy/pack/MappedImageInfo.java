package snappy.pack;

public class MappedImageInfo implements IMappedImageInfo {

	int X;
	int Y;
	IImageInfo imageInfo;
	
    public MappedImageInfo(int x, int y, IImageInfo imageInfo)
    {
        X = x;
        Y = y;
        this.imageInfo = imageInfo;
    }
    
    @Override
	public IImageInfo getImageInfo() {

		return imageInfo;
	}

	@Override
	public int getX() {

		return X;
	}

	@Override
	public int getY() {

		return Y;
	}

}
