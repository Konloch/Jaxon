package video;

/**
 * Represents a memory area that is used to store VideoChars.
 *
 * @see video.VideoChar
 */
public class VideoMemory extends STRUCT
{
	/**
	 * A predefined {@link video.VideoMemory} that points to the screen memory.
	 */
	public static final VideoMemory std = (VideoMemory) MAGIC.cast2Struct(0xB8000);
	
	/**
	 * The VideoChars of the memory area.
	 */
	@SJC(count = 2000)
	public VideoChar[] chars;
}
