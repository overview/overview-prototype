package snappy.pack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import snappy.graph.GraphLayout;
import snappy.ui.GraphDrawer;

public class MapperVerticalSorted<T extends ISprite> implements IMapper<T> {

	Class<T> myClass = null;
	
	public MapperVerticalSorted( T foo ) {
		
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

        ArrayList<GraphDrawer.LabelWidthMaker> myList = new ArrayList<GraphDrawer.LabelWidthMaker>();
        for (IImageInfo image : images)
        {

        	if( image instanceof GraphDrawer.LabelWidthMaker) {

        		myList.add((GraphDrawer.LabelWidthMaker)image); 
        	}
        }
        GraphDrawer.LabelWidthMaker[] sortedArray = new GraphDrawer.LabelWidthMaker[myList.size()];
        myList.toArray(sortedArray);
        Arrays.sort(sortedArray, new Comparator<GraphDrawer.LabelWidthMaker>() {

			@Override
			public int compare(GraphDrawer.LabelWidthMaker arg0, GraphDrawer.LabelWidthMaker arg1) {

				return arg1.gl.getNumPoints()-arg0.gl.getNumPoints();
			}} );
        
        for( int i = 0; i < myList.size(); i++) {
        	
            MappedImageInfo imageLocation = new MappedImageInfo(0, yOffset, sortedArray[i]);
            spriteInfo.AddMappedImage(imageLocation);
            yOffset += sortedArray[i].getHeight();
        }

        return spriteInfo;
	}

}
