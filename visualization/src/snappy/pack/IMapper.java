package snappy.pack;

public interface IMapper<T extends ISprite> {
    /// <summary>
    /// Works out how to map a series of images into a sprite.
    /// </summary>
    /// <param name="images">
    /// The list of images to place into the sprite.
    /// </param>
    /// <returns>
    /// A SpriteInfo object. This describes the locations of the images within the sprite,
    /// and the dimensions of the sprite.
    /// </returns>
    T Mapping(Iterable<IImageInfo> images);

}
