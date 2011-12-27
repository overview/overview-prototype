package snappy.pack;

import java.util.List;

public interface ISprite {
    /// <summary>
    /// Width of the sprite image
    /// </summary>
    int getWidth();

    /// <summary>
    /// Height of the sprite image
    /// </summary>
    int getHeight();

    /// <summary>
    /// Area of the sprite image
    /// </summary>
    int getArea();

    /// <summary>
    /// Holds the locations of all the individual images within the sprite image.
    /// </summary>
    List<IMappedImageInfo> getMappedImages();

    /// <summary>
    /// Adds an image to the SpriteInfo, and updates the width and height of the SpriteInfo.
    /// </summary>
    /// <param name="mappedImage"></param>
    void AddMappedImage(IMappedImageInfo mappedImage);

}
