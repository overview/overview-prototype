package snappy.pack;

public class MapperHorizontalOnly<T extends ISprite> implements IMapper<T>{

	Class<T> myClass = null;
	
	public MapperHorizontalOnly( T foo ) {
		
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
		
        int xOffset = 0;

        for (IImageInfo image : images)
        {
            MappedImageInfo imageLocation = new MappedImageInfo(xOffset, 0, image);
            spriteInfo.AddMappedImage(imageLocation);
            xOffset += image.getWidth();
        }

        return spriteInfo;
	}

}
