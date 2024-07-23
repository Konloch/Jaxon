package video;

/**
 * Represents a memory area that can be used to store
 * {@link video.VideoChar}s can be used.
 *
 * @see video.VideoChar
 */
public class VideoMemory extends STRUCT
{
	/**
	 * A predefined {@link video.VideoMemory}, which refers to the
	 * screen memory.
	 */
	public static final VideoMemory std = (VideoMemory) MAGIC.cast2Struct(0xB8000);
	
	/**
	 * The {@link video.VideoChar}s of the memory area.
	 */
	@SJC(count = 2000)
	public VideoChar[] chars;
}
