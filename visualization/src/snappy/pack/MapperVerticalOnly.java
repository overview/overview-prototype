package snappy.pack;

public class MapperVerticalOnly<T extends ISprite> implements IMapper<T> {

	Class<T> myClass = null;
	
	public MapperVerticalOnly( T foo ) {
		
		myClass = (Class<T>) foo.getClass();
	}
	
	@Override
	public T Mapping(Iterable<IImageInfo> images) {
        
		T spriteInfo = null;
        
		try {
			spriteInfo = myClass.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        int yOffset = 0;

        for (IImageInfo image : images)
        {
            MappedImageInfo imageLocation = new MappedImageInfo(0, yOffset, image);
            spriteInfo.AddMappedImage(imageLocation);
            yOffset += image.getHeight();
        }

        return spriteInfo;
	}

}
