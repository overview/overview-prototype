package snappy.pack;

public interface IMapperReturningStats<T extends ISprite> extends IMapper<T> {

    /// <summary>
    /// Version of IMapper.Mapping. See IMapper.
    /// </summary>
    /// <param name="images">Same as for IMapper.Mapping</param>
    /// <param name="mapperStats">
    /// The method will fill the properties of this statistics object.
    /// Set to null if you don't want statistics.
    /// </param>
    /// <returns>
    /// Same as for IMapper.Mapping
    /// </returns>
    T Mapping(Iterable<IImageInfo> images, IMapperStats mapperStats);
}
